package com.opencirc.api.passport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencirc.api.passport.adapter.PlatformAdapterFactory;
import com.opencirc.api.passport.config.AppProperties;
import com.opencirc.api.passport.constants.AppConstants;
import com.opencirc.api.passport.dao.DatasheetRepository;
import com.opencirc.api.passport.dao.PassportDatasheetMappingRepository;
import com.opencirc.api.passport.dao.PassportRepository;
import com.opencirc.api.passport.dto.CreatePassportUsingPlatformRequestDto;
import com.opencirc.api.passport.dto.CreatedByDto;
import com.opencirc.api.passport.dto.DataDictionaryTreeStructureDto;
import com.opencirc.api.passport.dto.DatasheetDto;
import com.opencirc.api.passport.dto.DatasheetPropertyDto;
import com.opencirc.api.passport.dto.PassportDto;
import com.opencirc.api.passport.dto.UpdateDataRequestDto;
import com.opencirc.api.passport.dto.UserDto;
import com.opencirc.api.passport.dto.query.PassportDatasheetResultMapQueryResult;
import com.opencirc.api.passport.enums.DataDictionary;
import com.opencirc.api.passport.enums.Platform;
import com.opencirc.api.passport.exception.InvalidDataDictionaryException;
import com.opencirc.api.passport.exception.InvalidInputException;
import com.opencirc.api.passport.exception.JsonValidationException;
import com.opencirc.api.passport.model.Datasheet;
import com.opencirc.api.passport.model.DatasheetProperty;
import com.opencirc.api.passport.model.Passport;
import com.opencirc.api.passport.model.PassportDatasheetMapping;
import io.github.thibaultmeyer.cuid.CUID;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.server.ResponseStatusException;

/** Service class for Passport operations. */
@Service
public class PassportService {

  private final DatasheetRepository datasheetRepository;

  private final PassportRepository passportRepository;

  private final PassportDatasheetMappingRepository passportDatasheetMappingRepository;

  private final ObjectMapper objectMapper;

  private final PlatformAdapterFactory platformAdapterFactory;

  private final AppProperties appProperties;

  /** Constructor. */
  public PassportService(
      DatasheetRepository datasheetRepository,
      PassportRepository passportRepository,
      PassportDatasheetMappingRepository passportDatasheetMappingRepository,
      PlatformAdapterFactory platformAdapterFactory,
      ObjectMapper objectMapper,
      AppProperties appProperties) {
    this.datasheetRepository = datasheetRepository;
    this.passportRepository = passportRepository;
    this.passportDatasheetMappingRepository = passportDatasheetMappingRepository;
    this.platformAdapterFactory = platformAdapterFactory;
    this.objectMapper = objectMapper;
    this.appProperties = appProperties;
  }

  /** Creates multiple passports. */
  @Transactional
  public List<PassportDto> batchCreatePassportsUsingPlatform(
      Platform platform, List<CreatePassportUsingPlatformRequestDto> dataArray, UserDto author)
      throws InvalidInputException, JsonValidationException, JsonProcessingException {
    var passportDtos = new ArrayList<PassportDto>();
    for (var passportData : dataArray) {
      passportDtos.add(createPassportUsingPlatform(platform, passportData, author, true));
    }
    return passportDtos;
  }

  /** Creates a passport. */
  @Transactional
  public PassportDto createPassportUsingPlatform(
      Platform platform,
      CreatePassportUsingPlatformRequestDto data,
      UserDto author,
      boolean asBatchOperation)
      throws InvalidInputException, JsonValidationException, JsonProcessingException {

    int customLength = AppConstants.CUID_LENGTH;
    CUID cuid = CUID.randomCUID2(customLength);

    Passport passport = new Passport();
    passport.setId(cuid.toString());
    passport.setName(data.getName());
    passport.setStatus(Passport.Status.ACTIVE);
    passport.setCreatedById(author != null ? author.getId() : null);
    passport.setCreatedTime(OffsetDateTime.now());
    passport.setCreatedBy(getOrDefaultCreatedBy(author != null ? CreatedByDto.from(author) : null));
    passport.setDatasheetMappings(new HashSet<>());

    String parentId = data.getParentId();
    if (parentId != null && !parentId.isBlank()) {
      if (passportRepository.findPassport(parentId, Passport.Status.ACTIVE).isEmpty()) {
        throw new HttpServerErrorException(
            HttpStatus.UNPROCESSABLE_ENTITY, "Invalid parentId: active parent not found");
      }
      passport.setParentId(parentId);
    }

    passport = passportRepository.save(passport);

    addDatasheetsToPassportUsingPlatform(
        passport,
        platform,
        data.getPlatformId(),
        Datasheet.DataCategory.fromValue(data.getDataCategory()),
        author,
        false);

    if (asBatchOperation && platform == Platform.BSDD) {
      // @TODO this is an INSANELY ugly hack, but is needed for now.
      //   it is marked as invokedExternally because we need the one datasheet.
      addDatasheetsToPassportUsingPlatform(
          passport,
          platform,
          "https://identifier.buildingsmart.org/uri/LCA/LCA/3.0/class/GeneralInformation",
          Datasheet.DataCategory.fromValue(data.getDataCategory()),
          author,
          true);
    }

    passport =
        passportRepository
            .findById(passport.getId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Passport not found"));

    var dataValues = data.getValues();
    if (dataValues != null) {
      var mappings = passport.getDatasheetMappings();
      if (mappings != null && !mappings.isEmpty()) {
        Map<String, Object> updateValues = new HashMap<>();
        for (var mapping : mappings) {
          var datasheet = mapping.getDatasheet();
          if (datasheet == null) {
            continue;
          }

          var properties = datasheet.getDatasheetProperties();
          if (properties == null || properties.isEmpty()) {
            continue;
          }

          for (var property : properties) {
            if (dataValues.containsKey(property.getCode())) {
              updateValues.put(property.getId(), dataValues.get(property.getCode()));
            }
          }
        }

        if (!updateValues.isEmpty()) {
          updateData(passport.getId(), updateValues);
        }
      }
    }

    return PassportDto.from(passport);
  }

  /**
   * Creates a datasheet and adds it to the passport using information from the provided platform.
   */
  @Transactional
  public PassportDto addDatasheetsToPassportUsingPlatform(
      String passportId,
      Platform platform,
      String platformId,
      Datasheet.DataCategory dataCategory,
      UserDto author,
      boolean invokedExternally)
      throws JsonValidationException, JsonProcessingException {
    Passport passport =
        passportRepository
            .findById(passportId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Passport not found"));
    if (passport.getStatus() != Passport.Status.ACTIVE) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passport is not active");
    }
    return addDatasheetsToPassportUsingPlatform(
        passport, platform, platformId, dataCategory, author, invokedExternally);
  }

  /**
   * Creates a datasheet and adds it to the passport using information from the provided platform.
   */
  @Transactional
  public PassportDto addDatasheetsToPassportUsingPlatform(
      Passport passport,
      Platform platform,
      String platformId,
      Datasheet.DataCategory dataCategory,
      UserDto author,
      boolean invokedExternally)
      throws JsonValidationException, JsonProcessingException {
    var adapter = platformAdapterFactory.getAdapter(platform);
    var rawDatasheets = adapter.generateDatasheetsFromPlatformId(platformId, false);
    for (var rawDatasheet : rawDatasheets) {
      rawDatasheet.setCreatedById(author != null ? author.getId() : null);
      rawDatasheet.setCreatedBy(
          getOrDefaultCreatedBy(author != null ? CreatedByDto.from(author) : null));
      rawDatasheet.setDataCategory(dataCategory);
      var datasheet = datasheetRepository.save(rawDatasheet);
      PassportDatasheetMapping mapping = new PassportDatasheetMapping();
      mapping.setPassport(passport);
      mapping.setDatasheet(datasheet);
      mapping = passportDatasheetMappingRepository.save(mapping);
      passport.getDatasheetMappings().add(mapping);
    }

    return PassportDto.from(passport);
  }

  private CreatedByDto getOrDefaultCreatedBy(CreatedByDto createdBy) {
    if (createdBy != null) {
      return createdBy;
    }
    return new CreatedByDto(
        appProperties.getSystemAdminName(), appProperties.getSystemAdminEmail());
  }

  /** Retrieves passport. */
  public PassportDto getPassport(String passportId) {
    Optional<Passport> optionalPassport =
        passportRepository.findPassport(passportId, Passport.Status.ACTIVE);
    if (optionalPassport.isEmpty()) {
      throw new HttpServerErrorException(
          HttpStatus.NOT_FOUND, "Could not find passport with ID " + passportId);
    }

    return PassportDto.from(optionalPassport.get());
  }

  /** Retrieves passport and its children for the given id. */
  public List<PassportDto> getPassportChildren(String id) {
    List<PassportDatasheetResultMapQueryResult> resultRows =
        passportRepository.findPassportWithDescendants(id).orElse(Collections.emptyList());

    if (resultRows.isEmpty()) {
      throw new HttpServerErrorException(
          HttpStatus.NOT_FOUND, "No active passports found with children");
    }

    Map<String, PassportDto> passportMap = new LinkedHashMap<>();

    for (PassportDatasheetResultMapQueryResult row : resultRows) {
      PassportDto passportDto =
          passportMap.computeIfAbsent(row.getPassportId(), key -> buildPassportDto(row));

      if (row.getDatasheetId() != null) {
        DatasheetDto datasheetDto =
            passportDto.getDatasheets().stream()
                .filter(datasheet -> Objects.equals(datasheet.getId(), row.getDatasheetId()))
                .findFirst()
                .orElseGet(
                    () -> {
                      DatasheetDto newDatasheet = buildDatasheetDto(row);
                      passportDto.getDatasheets().add(newDatasheet);
                      return newDatasheet;
                    });

        if (row.getDatasheetPropertyId() != null) {
          boolean alreadyExists =
              datasheetDto.getDatasheetProperties().stream()
                  .anyMatch(
                      property -> Objects.equals(property.getId(), row.getDatasheetPropertyId()));
          if (!alreadyExists) {
            DatasheetPropertyDto propertyDto = buildDatasheetProperty(row);
            if (propertyDto != null) {
              datasheetDto.getDatasheetProperties().add(propertyDto);
            }
          }
        }
      }
    }

    return new ArrayList<>(passportMap.values());
  }

  /** Retrieves the passports with the given parent ID. */
  public List<PassportDto> getImmediateChildren(String passportId) {
    if (!passportRepository.existsById(passportId)) {
      throw new HttpServerErrorException(
          HttpStatus.NOT_FOUND, "Could not find passport with ID " + passportId);
    }
    List<PassportDatasheetResultMapQueryResult> resultRows =
        passportRepository.findImmediateChildren(passportId).orElse(Collections.emptyList());

    return assemblePassportsFromResultRows(resultRows);
  }

  /** Retrieves all the root passports. */
  public List<PassportDto> getRootPassports() {
    List<Passport> passports = passportRepository.getRootPassports();
    if (passports == null) {
      throw new HttpServerErrorException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Could not retrieve root passports");
    }
    return passports.stream().map(PassportDto::from).collect(Collectors.toList());
  }

  private PassportDto buildPassportDto(PassportDatasheetResultMapQueryResult row) {
    PassportDto dto = new PassportDto();
    dto.setId(row.getPassportId());
    dto.setParentId(row.getParentId());
    dto.setName(row.getPassportName());
    dto.setStatus(row.getStatus());
    dto.setCreatedById(row.getPassportCreatedById());
    dto.setCreatedBy(parseCreatedBy(row.getPassportCreatedBy()));
    if (row.getPassportCreatedTime() != null) {
      dto.setCreatedTime(row.getPassportCreatedTime().atOffset(ZoneOffset.UTC));
    }
    dto.setDatasheets(new ArrayList<>());
    return dto;
  }

  private DatasheetDto buildDatasheetDto(PassportDatasheetResultMapQueryResult row) {
    try {
      DatasheetDto dto = new DatasheetDto();
      dto.setId(row.getDatasheetId());

      dto.setPlatform(row.getPlatform());

      dto.setDictionary(row.getDictionary());

      dto.setCode(row.getDatasheetCode());
      dto.setName(row.getDatasheetName());
      dto.setDescription(row.getDatasheetDescription());
      dto.setPlatformId(row.getDatasheetPlatformId());
      dto.setDataCategory(row.getDataCategory());

      Map<String, Object> dataMap = null;
      String data = row.getData();
      if (data != null && !data.isBlank()) {
        dataMap =
            objectMapper.readValue(data, new com.fasterxml.jackson.core.type.TypeReference<>() {});
      }
      dto.setData(dataMap);

      dto.setCreatedById(row.getDatasheetCreatedById());
      dto.setCreatedBy(parseCreatedBy(row.getDatasheetCreatedBy()));
      if (row.getDatasheetCreatedTime() != null) {
        dto.setCreatedTime(row.getDatasheetCreatedTime().atOffset(ZoneOffset.UTC));
      }
      dto.setDatasheetProperties(new ArrayList<>());

      return dto;
    } catch (JsonProcessingException e) {
      throw new RuntimeException(
          "Error parsing datasheet JSON for datasheetId="
              + row.getDatasheetId()
              + ", passportId="
              + row.getPassportId(),
          e);
    }
  }

  private DatasheetPropertyDto buildDatasheetProperty(PassportDatasheetResultMapQueryResult row) {

    if (row.getDatasheetPropertyId() == null) {
      return null;
    }

    DatasheetPropertyDto propertyDto = new DatasheetPropertyDto();

    propertyDto.setId(row.getDatasheetPropertyId());
    propertyDto.setDatasheetId(row.getDatasheetId());
    propertyDto.setCode(row.getDatasheetPropertyCode());
    propertyDto.setPlatformId(row.getDatasheetPropertyPlatformId());
    propertyDto.setGroupTag(row.getDatasheetPropertyGroupTag());
    propertyDto.setPropertyType(row.getDatasheetPropertyType());

    String definition = row.getDatasheetPropertyDefinition();
    if (definition != null && !definition.isBlank()) {
      try {
        JsonNode definitionNode = objectMapper.readTree(definition);
        propertyDto.setDefinition(definitionNode);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(
            "Error parsing datasheet property JSON for datasheetPropertyId = "
                + row.getDatasheetPropertyId()
                + ", datasheetId = "
                + row.getDatasheetId()
                + ", passportId = "
                + row.getPassportId(),
            e);
      }
    } else {
      propertyDto.setDefinition(null);
    }

    return propertyDto;
  }

  private CreatedByDto parseCreatedBy(String createdByJson) {
    if (createdByJson == null || createdByJson.isBlank()) {
      return null;
    }
    try {
      return objectMapper.readValue(createdByJson, CreatedByDto.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Error parsing createdBy JSON", e);
    }
  }

  /** Update passport datasheet data from a DTO. */
  @Transactional
  public PassportDto updateData(String passportId, UpdateDataRequestDto updateDataRequestDto) {
    return updateData(passportId, updateDataRequestDto.getValues());
  }

  /**
   * Updates datasheet property values for the given passport and group. Only properties present in
   * {@code updateDataRequestDto.values} are updated.
   */
  @Transactional
  public PassportDto updateData(String passportId, Map<String, Object> values) {
    Passport passport =
        passportRepository
            .findPassport(passportId, Passport.Status.ACTIVE)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Passport not found"));

    var mappings = passport.getDatasheetMappings();
    if (mappings == null || mappings.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Passport does not have any datasheet mappings: " + passportId);
    }

    for (PassportDatasheetMapping mapping : mappings) {
      Datasheet datasheet = mapping.getDatasheet();
      if (datasheet == null) {
        continue;
      }
      ObjectNode dataNode =
          datasheet.getData() != null
              ? objectMapper.valueToTree(datasheet.getData())
              : JsonNodeFactory.instance.objectNode();

      boolean isChanged = false;

      // List<String> errorMessages = new ArrayList<>();
      for (DatasheetProperty property : datasheet.getDatasheetProperties()) {
        String propertyId = property.getId();

        if (!values.containsKey(property.getId())) {
          continue;
        }

        JsonNode newValue = objectMapper.valueToTree(values.get(propertyId));
        JsonNode existingValue = dataNode.get(propertyId);
        if (Objects.equals(newValue, existingValue)) {
          continue;
        }

        isChanged = true;
        dataNode.set(propertyId, newValue);

        /*
        ObjectNode propertyDefinition = (ObjectNode) property.getDefinition();
        JsonNode newValueNode =
            newValue == null ? NullNode.instance : objectMapper.valueToTree(newValue);

        propertyDefinition.set("actualValue", newValueNode);

        String error =
            platformAdapterFactory
                .getAdapter(datasheet.getPlatform())
                .validatePassportData(property.getDefinition());
        if (error != null) {
          errorMessages.add(error);
          continue;
        }

        JsonNode currentValue = dataNode.get(propertyCode);

        if (!Objects.equals(currentValue, newValueNode)) {
          dataNode.set(propertyCode, newValueNode);
          changed = true;
          updatedProperties.put(propertyCode, newValue);
        } */
      }

      /* if (!errorMessages.isEmpty()) {
        throw new InvalidInputException(
            "Validation failed with the following errors: " + errorMessages);
      } */
      if (isChanged) {
        datasheet.setData(
            objectMapper.convertValue(
                dataNode, new com.fasterxml.jackson.core.type.TypeReference<>() {}));
        datasheetRepository.save(datasheet);
      }
    }

    // if (updatedProperties.isEmpty()) {
    //  throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No valid properties found");
    // }

    return PassportDto.from(passport);
  }

  /** Retrieves all passports associated with the specified code. */
  public List<PassportDto> listPassportsByCode(String code) {

    List<PassportDatasheetResultMapQueryResult> resultRows =
        passportRepository.findPassportsByCode(code).orElse(Collections.emptyList());

    return assemblePassportsFromResultRows(resultRows);
  }

  /** This method sets the result sets to passport dto. */
  private List<PassportDto> assemblePassportsFromResultRows(
      List<PassportDatasheetResultMapQueryResult> resultRows) {

    Map<String, PassportDto> passportMap = new LinkedHashMap<>();

    for (PassportDatasheetResultMapQueryResult row : resultRows) {

      PassportDto passportDto =
          passportMap.computeIfAbsent(row.getPassportId(), id -> buildPassportDto(row));

      if (row.getDatasheetId() != null) {

        DatasheetDto datasheetDto =
            passportDto.getDatasheets().stream()
                .filter(datasheet -> Objects.equals(datasheet.getId(), row.getDatasheetId()))
                .findFirst()
                .orElseGet(
                    () -> {
                      DatasheetDto newDatasheet = buildDatasheetDto(row);
                      passportDto.getDatasheets().add(newDatasheet);
                      return newDatasheet;
                    });

        if (row.getDatasheetPropertyId() != null) {

          boolean exists =
              datasheetDto.getDatasheetProperties().stream()
                  .anyMatch(
                      datasheetProperty ->
                          Objects.equals(datasheetProperty.getId(), row.getDatasheetPropertyId()));

          if (!exists) {
            DatasheetPropertyDto propertyDto = buildDatasheetProperty(row);
            datasheetDto.getDatasheetProperties().add(propertyDto);
          }
        }
      }
    }

    return new ArrayList<>(passportMap.values());
  }

  /** Parse and form the tree structure of the platform. */
  public List<DataDictionaryTreeStructureDto> getDictionaryTreeStructure(
      Platform platform, DataDictionary dictionary)
      throws IOException, InvalidDataDictionaryException {
    return this.platformAdapterFactory.getAdapter(platform).getDictionaryTreeStructure(dictionary);
  }
}
