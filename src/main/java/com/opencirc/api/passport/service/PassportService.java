package com.opencirc.api.passport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencirc.api.passport.adapter.DictionaryAdapterFactory;
import com.opencirc.api.passport.config.AppProperties;
import com.opencirc.api.passport.constants.AppConstants;
import com.opencirc.api.passport.dao.DatasheetPropertyRepository;
import com.opencirc.api.passport.dao.DatasheetRepository;
import com.opencirc.api.passport.dao.PassportDatasheetMappingRepository;
import com.opencirc.api.passport.dao.PassportRepository;
import com.opencirc.api.passport.dto.CreatePassportRequestDto;
import com.opencirc.api.passport.dto.CreatedByDto;
import com.opencirc.api.passport.dto.DatasheetDto;
import com.opencirc.api.passport.dto.DatasheetPropertyDto;
import com.opencirc.api.passport.dto.PassportDto;
import com.opencirc.api.passport.dto.UpdateDataRequestDto;
import com.opencirc.api.passport.dto.query.PassportDatasheetResultMapDto;
import com.opencirc.api.passport.enums.DataDictionary;
import com.opencirc.api.passport.enums.DataDictionaryPlatform;
import com.opencirc.api.passport.exception.InvalidInputException;
import com.opencirc.api.passport.exception.JsonValidationException;
import com.opencirc.api.passport.model.Datasheet;
import com.opencirc.api.passport.model.Datasheet.DataCategory;
import com.opencirc.api.passport.model.DatasheetProperty;
import com.opencirc.api.passport.model.Passport;
import com.opencirc.api.passport.model.PassportDatasheetMapping;
import io.github.thibaultmeyer.cuid.CUID;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
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

@Service
public class PassportService {

  /** Injecting DatasheetRepository class. */
  private final DatasheetRepository datasheetRepository;

  /** Injecting DatasheetPropertyRepository class. */
  private final DatasheetPropertyRepository datasheetPropertyRepository;

  /** Injecting PassportRepository class. */
  private final PassportRepository passportRepository;

  /** Injecting PassportDatasheetMappingRepository class. */
  private final PassportDatasheetMappingRepository passportDatasheetMappingRepository;

  /** Injecting ObjectMapper bean. */
  private final ObjectMapper objectMapper;

  /** Injecting DictionaryAdapterFactory class. */
  private final DictionaryAdapterFactory dictionaryAdapterFactory;

  /** Injecting AppProperties class. */
  private final AppProperties appProperties;

  /**
   * Constructor.
   *
   * @param datasheetRepository
   * @param passportRepository
   * @param passportDatasheetMappingRepository
   * @param dictionaryAdapterFactory
   * @param objectMapper
   * @param appProperties
   */
  public PassportService(
      DatasheetRepository datasheetRepository,
      DatasheetPropertyRepository datasheetPropertyRepository,
      PassportRepository passportRepository,
      PassportDatasheetMappingRepository passportDatasheetMappingRepository,
      DictionaryAdapterFactory dictionaryAdapterFactory,
      ObjectMapper objectMapper,
      AppProperties appProperties) {
    this.datasheetRepository = datasheetRepository;
    this.datasheetPropertyRepository = datasheetPropertyRepository;
    this.passportRepository = passportRepository;
    this.passportDatasheetMappingRepository = passportDatasheetMappingRepository;
    this.dictionaryAdapterFactory = dictionaryAdapterFactory;
    this.objectMapper = objectMapper;
    this.appProperties = appProperties;
  }

  /**
   * Creates template Entry.
   *
   * @param dictionaryPlatform
   * @param data
   * @return Passport DTO from passport
   */
  @Transactional
  public PassportDto createPassportUsingDictionary(
      DataDictionaryPlatform dictionaryPlatform,
      DataDictionary dictionary,
      CreatePassportRequestDto data)
      throws InvalidInputException {
    JsonNode datasheetData = data.getDatasheetData();

    try {
      validatePassportData(dictionaryPlatform, datasheetData);
    } catch (JsonValidationException e) {
      throw new InvalidInputException(e.getMessage());
    }

    int customLength = AppConstants.CUID_LENGTH;
    CUID cuid = CUID.randomCUID2(customLength);

    Passport passport = new Passport();
    passport.setId(cuid.toString());
    passport.setName(data.getPassportName());
    passport.setStatus(Passport.Status.active);
    passport.setCreatedById(data.getCreatedById());
    passport.setCreatedTime(OffsetDateTime.now());
    passport.setCreatedBy(getOrDefaultCreatedBy(data.getCreatedById(), data.getCreatedBy()));

    String parentId = data.getParentId();
    if (parentId != null && !parentId.isBlank()) {
      if (passportRepository.findPassport(parentId, Passport.Status.active).isEmpty()) {
        throw new HttpServerErrorException(
            HttpStatus.UNPROCESSABLE_ENTITY, "Invalid parentId: active parent not found");
      }
      passport.setParentId(parentId);
    }

    passport = passportRepository.save(passport);

    String code = datasheetData.hasNonNull("code") ? datasheetData.get("code").asText() : null;
    String name = datasheetData.hasNonNull("name") ? datasheetData.get("name").asText() : null;
    String description =
        datasheetData.hasNonNull("definition") ? datasheetData.get("definition").asText() : null;
    String platformId =
        datasheetData.hasNonNull("dictionaryUri")
            ? datasheetData.get("dictionaryUri").asText()
            : null;

    Datasheet datasheet = new Datasheet();
    datasheet.setPlatform(dictionaryPlatform);
    datasheet.setDictionary(dictionary);
    datasheet.setCode(code);
    datasheet.setName(name);
    datasheet.setDescription(description);
    datasheet.setPlatformId(platformId);
    datasheet.setDataCategory(DataCategory.fromValue(data.getDataCategory()));
    datasheet.setCreatedById(data.getCreatedById());
    datasheet.setCreatedTime(OffsetDateTime.now());
    datasheet.setCreatedBy(getOrDefaultCreatedBy(data.getCreatedById(), data.getCreatedBy()));

    datasheet = datasheetRepository.save(datasheet);

    List<DatasheetProperty> propertyList = new ArrayList<>();
    JsonNode propertiesNode = datasheetData.path("classProperties");
    if (propertiesNode.isArray()) {
      for (JsonNode propNode : propertiesNode) {
        String propCode = getText(propNode, "propertyCode");
        if (propCode == null || propCode.isBlank()) {
          continue;
        }
        DatasheetProperty property = new DatasheetProperty();
        property.setCode(propCode);
        property.setGroupTag(getText(propNode, "propertySet"));
        property.setPropertyType(getText(propNode, "dataType"));
        property.setDefinition(propNode);
        property.setPlatformId(platformId);
        property.setDatasheet(datasheet);
        propertyList.add(property);
      }
    }
    propertyList = datasheetPropertyRepository.saveAll(propertyList);

    ObjectNode dataJson = JsonNodeFactory.instance.objectNode();
    Map<String, DatasheetProperty> propertyByCode =
        propertyList.stream()
            .filter(property -> property.getCode() != null)
            .collect(Collectors.toMap(DatasheetProperty::getCode, property -> property));

    for (JsonNode propertyNode : propertiesNode) {
      String propertyCode = getText(propertyNode, "propertyCode");
      JsonNode actualValueNode = propertyNode.path("actualValue");
      DatasheetProperty property = propertyByCode.get(propertyCode);
      if (property != null) {
        dataJson.set(
            property.getCode().toString(),
            actualValueNode.isMissingNode() ? NullNode.instance : actualValueNode);
      }
    }

    datasheet.setData(dataJson);
    datasheet = datasheetRepository.save(datasheet);

    PassportDatasheetMapping mapping = new PassportDatasheetMapping();
    mapping.setPassport(passport);
    mapping.setDatasheet(datasheet);
    mapping = passportDatasheetMappingRepository.save(mapping);

    if (passport.getDatasheetMappings() == null) {
      passport.setDatasheetMappings(new HashSet<>());
    }
    passport.getDatasheetMappings().add(mapping);
    return PassportDto.from(passport);
  }

  private String getText(JsonNode node, String field) {
    return node.hasNonNull(field) ? node.get(field).asText() : null;
  }

  private CreatedByDto getOrDefaultCreatedBy(String createdById, CreatedByDto createdBy) {
    if (createdBy != null) {
      return createdBy;
    }
    return new CreatedByDto(
        appProperties.getSystemAdminName(), appProperties.getSystemAdminEmail());
  }

  /**
   * Retrieves passport.
   *
   * @param passportId
   * @return Passport DTO from passport
   */
  public PassportDto getPassport(String passportId) throws JsonProcessingException {

    Optional<Passport> optionalPassport =
        passportRepository.findPassport(passportId, Passport.Status.active);
    if (optionalPassport.isEmpty()) {
      throw new HttpServerErrorException(
          HttpStatus.NOT_FOUND, "Could not find passport with ID " + passportId);
    }

    return PassportDto.from(optionalPassport.get());
  }

  /**
   * Retrieves passport and its children for the given id.
   *
   * @param id
   * @return a list of {@link PassportDto} objects
   * @throws JsonProcessingException
   */
  public List<PassportDto> getPassportChildren(String id) throws JsonProcessingException {
    List<PassportDatasheetResultMapDto> resultRows =
        passportRepository.findPassportWithDescendants(id).orElse(Collections.emptyList());

    if (resultRows.isEmpty()) {
      throw new HttpServerErrorException(
          HttpStatus.NOT_FOUND, "No active passports found with children");
    }

    Map<String, PassportDto> passportMap = new LinkedHashMap<>();

    for (PassportDatasheetResultMapDto row : resultRows) {
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

  /**
   * Retrieves the passports with the given parent ID.
   *
   * @param passportId
   * @return a list of {@link PassportDto} objects
   * @throws JsonProcessingException
   */
  public List<PassportDto> getImmediateChildren(String passportId) throws JsonProcessingException {
    List<PassportDatasheetResultMapDto> resultRows =
        passportRepository.findImmediateChildren(passportId).orElse(Collections.emptyList());

    Map<String, PassportDto> passportMap = new LinkedHashMap<>();

    for (PassportDatasheetResultMapDto row : resultRows) {
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
          boolean alreadyExists =
              datasheetDto.getDatasheetProperties().stream()
                  .anyMatch(
                      property -> Objects.equals(property.getId(), row.getDatasheetPropertyId()));
          if (!alreadyExists) {
            DatasheetPropertyDto propertyDto = buildDatasheetProperty(row);
            datasheetDto.getDatasheetProperties().add(propertyDto);
          }
        }
      }
    }
    return new ArrayList<>(passportMap.values());
  }

  /**
   * Validate the passport, throw if there is an error.
   *
   * @param dictionaryPlatform
   * @param passportData
   */
  private void validatePassportData(
      DataDictionaryPlatform dictionaryPlatform, JsonNode passportData)
      throws JsonValidationException {
    if (passportData == null
        || passportData.isNull()
        || passportData.isObject() && passportData.size() == 0) {
      throw new InvalidInputException("Input JSON node is null");
    }
    List<String> errorMessages = new ArrayList<>();
    ArrayNode properties = null;
    try {
      if (passportData.has("classType")
          && "Class".equals(passportData.path("classType").asText())) {
        if (passportData.has("classProperties")) {
          properties = (ArrayNode) passportData.get("classProperties");
        }

      } else if (passportData.has("properties")) {
        if (passportData.get("properties").isArray()) {
          properties = (ArrayNode) passportData.get("properties");
        }

      } else {
        throw new InvalidInputException("Invalid Template");
      }
    } catch (ClassCastException e) {
      throw new InvalidInputException("Expected an array node for properties");
    }
    for (JsonNode property : properties) {
      String error =
          dictionaryAdapterFactory.getAdapter(dictionaryPlatform).validatePassportData(property);
      if (error != null) {
        errorMessages.add(error);
      }
    }

    if (!errorMessages.isEmpty()) {
      throw new JsonValidationException(
          "Validation failed with the following errors: " + errorMessages);
    }
  }

  /**
   * Retrieves all the root passports.
   *
   * @return Passport DTO list from passport
   */
  public List<PassportDto> getRootPassports() {
    List<Passport> passports = passportRepository.getRootPassports();
    if (passports == null) {
      throw new HttpServerErrorException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Could not retrieve root passports");
    }
    return passports.stream().map(PassportDto::from).collect(Collectors.toList());
  }

  private PassportDto buildPassportDto(PassportDatasheetResultMapDto row) {
    PassportDto dto = new PassportDto();
    dto.setId(row.getPassportId());
    dto.setParentId(row.getParentId());
    dto.setName(row.getPassportName());
    dto.setStatus(Passport.Status.fromValue(row.getStatus()));
    dto.setCreatedById(row.getPassportCreatedById());
    dto.setCreatedBy(parseCreatedBy(row.getPassportCreatedBy()));
    if (row.getPassportCreatedTime() != null) {
      dto.setCreatedTime(row.getPassportCreatedTime().atOffset(ZoneOffset.UTC));
    }
    dto.setDatasheets(new ArrayList<>());
    return dto;
  }

  private DatasheetDto buildDatasheetDto(PassportDatasheetResultMapDto row) {
    try {
      DatasheetDto dto = new DatasheetDto();
      dto.setId(row.getDatasheetId());

      dto.setPlatform(
          row.getPlatform() != null ? DataDictionaryPlatform.fromValue(row.getPlatform()) : null);

      dto.setDictionary(
          row.getDictionary() != null ? DataDictionary.fromValue(row.getDictionary()) : null);

      dto.setCode(row.getDatasheetCode());
      dto.setName(row.getDatasheetName());
      dto.setDescription(row.getDatasheetDescription());
      dto.setPlatformId(row.getDatasheetPlatformId());
      dto.setDataCategory(
          row.getDataCategory() != null ? DataCategory.fromValue(row.getDataCategory()) : null);

      JsonNode dataNode = null;
      String data = row.getData();
      if (data != null && !data.isBlank()) {
        dataNode = objectMapper.readTree(data);
      }
      dto.setData(dataNode);

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

  private DatasheetPropertyDto buildDatasheetProperty(PassportDatasheetResultMapDto row) {

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

  /**
   * Updates datasheet property values for the given passport and group. Only properties present in
   * {@code updateDataRequestDto.values} are updated.
   *
   * @param passportId passport ID
   * @param updateDataRequestDto
   * @return updated Passport dto
   * @throws JsonValidationException
   */
  @Transactional
  public PassportDto updateData(String passportId, UpdateDataRequestDto updateDataRequestDto)
      throws JsonValidationException {
    Passport passport =
        passportRepository
            .findPassport(passportId, Passport.Status.active)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Passport not found"));

    Map<String, Object> updatedProperties = new LinkedHashMap<>();

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
              ? datasheet.getData().deepCopy()
              : JsonNodeFactory.instance.objectNode();

      boolean changed = false;
      var properties = datasheet.getDatasheetProperties();

      if (properties == null || properties.isEmpty()) {
        continue;
      }

      List<DatasheetProperty> propertyGroupList =
          properties.stream()
              .filter(property -> updateDataRequestDto.getGroup().equals(property.getGroupTag()))
              .toList();
      List<String> errorMessages = new ArrayList<>();
      for (DatasheetProperty property : propertyGroupList) {
        String propertyCode = property.getCode();

        if (updateDataRequestDto.getValues().containsKey(propertyCode)) {
          Object newValue = updateDataRequestDto.getValues().get(propertyCode);
          ObjectNode propertyDefinition = (ObjectNode) property.getDefinition();
          JsonNode newValueNode =
              newValue == null ? NullNode.instance : objectMapper.valueToTree(newValue);

          propertyDefinition.set("actualValue", newValueNode);

          String error = null;
          error =
              dictionaryAdapterFactory
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
          }
        }
      }
      if (!errorMessages.isEmpty()) {
        throw new InvalidInputException(
            "Validation failed with the following errors: " + errorMessages);
      }
      if (changed) {
        datasheet.setData(dataNode);
        datasheet = datasheetRepository.save(datasheet);
      }
    }

    if (updatedProperties.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "No valid properties found for group : " + updateDataRequestDto.getGroup());
    }

    return PassportDto.from(passport);
  }
}
