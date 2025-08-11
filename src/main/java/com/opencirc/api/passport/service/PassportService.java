package com.opencirc.api.passport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencirc.api.passport.adapter.DictionaryAdapterFactory;
import com.opencirc.api.passport.dao.DatasheetRepository;
import com.opencirc.api.passport.dao.PassportDatasheetMappingRepository;
import com.opencirc.api.passport.dao.PassportRepository;
import com.opencirc.api.passport.dto.CreatePassportRequestDto;
import com.opencirc.api.passport.dto.DatasheetDto;
import com.opencirc.api.passport.dto.PassportDatasheetResultMapDto;
import com.opencirc.api.passport.dto.PassportDto;
import com.opencirc.api.passport.enums.DataDictionary;
import com.opencirc.api.passport.exception.InvalidInputException;
import com.opencirc.api.passport.exception.JsonValidationException;
import com.opencirc.api.passport.model.Datasheet;
import com.opencirc.api.passport.model.Datasheet.DataCategory;
import com.opencirc.api.passport.model.Passport;
import com.opencirc.api.passport.model.PassportDatasheetMapping;
import io.github.thibaultmeyer.cuid.CUID;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;

@Service
public class PassportService {

  /** Injecting DatasheetRepository class. */
  @Autowired private DatasheetRepository datasheetRepository;

  /** Injecting PassportRepository class. */
  @Autowired private PassportRepository passportRepository;

  /** Injecting PassportDatasheetMappingRepository class. */
  @Autowired private PassportDatasheetMappingRepository passportDatasheetMappingRepository;

  /** Injecting DictionaryAdapterFactory class. */
  @Autowired private DictionaryAdapterFactory dictionaryAdapterFactory;

  /**
   * Creates a passport using the given data.
   *
   * @param dictionary the dictionary to use for validation
   * @param data the data to use for validation
   * @return Passport DTO from the created passport
   */
  public PassportDto createPassportUsingDictionary(
      DataDictionary dictionary, CreatePassportRequestDto data) throws InvalidInputException {
    JsonNode datasheetData = data.getDatasheetData();
    try {
      validatePassportData(dictionary, datasheetData);
    } catch (JsonValidationException e) {
      throw new HttpServerErrorException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    Datasheet datasheet = new Datasheet();
    datasheet.setData(datasheetData);
    datasheet.setDataCategory(DataCategory.fromValue(data.getDataCategory()));
    datasheet.setDataDictionary(dictionary);
    datasheet.setCreatedBy(data.getCreatedBy());
    datasheet.setCreatedTime(data.getCreatedTime());
    datasheet = datasheetRepository.save(datasheet);

    CUID cuid = CUID.randomCUID2(36);
    Passport rawPassport = new Passport();
    rawPassport.setId(cuid.toString());
    rawPassport.setName(data.getPassportName());
    rawPassport.setStatus(Passport.Status.ACTIVE);
    rawPassport.setCreatedBy(data.getCreatedBy());
    rawPassport.setCreatedTime(LocalDateTime.now());
    Passport passport = passportRepository.save(rawPassport);

    PassportDatasheetMapping passportDatasheet = new PassportDatasheetMapping();
    passportDatasheet.setPassport(rawPassport);
    passportDatasheet.setDatasheet(datasheet);
    PassportDatasheetMapping passportDatasheetMapping =
        passportDatasheetMappingRepository.save(passportDatasheet);
    // Set up the data sheet mapping details in the return value of the Passport DTO
    passport.setDatasheetMappings(new ArrayList<>());
    passport.getDatasheetMappings().add(passportDatasheetMapping);

    return PassportDto.from(passport);
  }

  /**
   * Retrieves passport.
   *
   * @param passportId the ID of the passport
   * @return Passport DTO from passport
   */
  public PassportDto getPassport(String passportId) {

    Optional<Passport> optionalPassport =
        passportRepository.findPassport(passportId, Passport.Status.ACTIVE);
    if (optionalPassport.isEmpty()
        || optionalPassport.get().getStatus() != Passport.Status.ACTIVE) {
      throw new HttpServerErrorException(
          HttpStatus.NOT_FOUND, "Could not find passport with ID " + passportId);
    }

    return PassportDto.from(optionalPassport.get());
  }

  /**
   * Retrieves passport and its children for the given id.
   *
   * @param passportId the ID of the root passport
   * @return a list of {@link PassportDto} objects
   */
  public List<PassportDto> getChildren(String passportId) throws JsonProcessingException {
    Optional<List<PassportDatasheetResultMapDto>> optionalPassportList =
        passportRepository.findPassportWithDescendants(passportId);

    List<PassportDatasheetResultMapDto> resultRows =
        optionalPassportList.orElse(Collections.emptyList());

    if (resultRows.isEmpty()) {
      throw new HttpServerErrorException(
          HttpStatus.NOT_FOUND, "No active passports found with children");
    }

    Map<String, PassportDto> dtoById = new LinkedHashMap<>();
    ObjectMapper objectMapper = new ObjectMapper();

    for (PassportDatasheetResultMapDto row : resultRows) {
      String rowPassportId = row.passportId();

      PassportDto passportDto =
          dtoById.computeIfAbsent(
              rowPassportId,
              key -> {
                PassportDto dto = new PassportDto();
                dto.setId(rowPassportId);
                dto.setName(row.passportName());
                dto.setStatus(Passport.Status.fromValue(row.status()));
                dto.setCreatedBy(row.createdBy());
                dto.setCreatedTime(row.createdTime().toLocalDateTime());
                dto.setDatasheets(new ArrayList<>());
                return dto;
              });

      if (row.datasheetId() != null) {
        boolean alreadyExists =
            passportDto.getDatasheets().stream()
                .anyMatch(ds -> Objects.equals(ds.getId(), row.datasheetId()));

        if (!alreadyExists) {
          DatasheetDto datasheetDto = new DatasheetDto();
          datasheetDto.setId(row.datasheetId());
          datasheetDto.setData(objectMapper.readTree(row.data()));
          if (row.dataCategory() != null) {
            datasheetDto.setDataCategory(DataCategory.fromValue(row.dataCategory()));
          } else {
            datasheetDto.setDataCategory(null);
          }

          if (row.dataDictionary() != null) {
            datasheetDto.setDataDictionary(DataDictionary.valueOf(row.dataDictionary()));
          } else {
            datasheetDto.setDataDictionary(null);
          }
          datasheetDto.setCreatedBy(row.createdBy());
          datasheetDto.setCreatedTime(row.createdTime().toLocalDateTime());

          passportDto.getDatasheets().add(datasheetDto);
        }
      }
    }

    for (PassportDatasheetResultMapDto row : resultRows) {
      String parentId = row.parentId();
      if (parentId != null && dtoById.containsKey(parentId)) {
        PassportDto child = dtoById.get(row.passportId());
        PassportDto parent = dtoById.get(parentId);
        child.setParent(parent);
      }
    }

    return new ArrayList<>(dtoById.values());
  }

  /**
   * Retrieves the passports with the given parent ID.
   *
   * @param passportId the ID of the parent passport
   * @return a list of {@link PassportDto} objects
   */
  public List<PassportDto> getImmediateChildren(String passportId) throws JsonProcessingException {
    Optional<List<PassportDatasheetResultMapDto>> optionalPassportList =
        passportRepository.findImmediateChildren(passportId);

    List<PassportDatasheetResultMapDto> resultRows =
        optionalPassportList.orElse(Collections.emptyList());

    ObjectMapper objectMapper = new ObjectMapper();
    List<PassportDto> passportDtoList = new ArrayList<>();
    Map<String, DatasheetDto> datasheetDtoMap = new LinkedHashMap<>();

    for (PassportDatasheetResultMapDto row : resultRows) {
      PassportDto passportDto = new PassportDto();
      passportDto.setId(row.passportId());
      passportDto.setName(row.passportName());
      passportDto.setStatus(Passport.Status.fromValue(row.status()));
      passportDto.setCreatedBy(row.createdBy());
      passportDto.setCreatedTime(row.createdTime().toLocalDateTime());
      passportDto.setDatasheets(new ArrayList<>());

      if (row.datasheetId() != null) {
        if (!datasheetDtoMap.containsKey(row.datasheetId())) {
          DatasheetDto datasheetDto = new DatasheetDto();
          datasheetDto.setId(row.datasheetId());
          datasheetDto.setData(objectMapper.readTree(row.data()));
          if (row.dataCategory() != null) {
            datasheetDto.setDataCategory(DataCategory.fromValue(row.dataCategory()));
          } else {
            datasheetDto.setDataCategory(null);
          }

          if (row.dataDictionary() != null) {
            datasheetDto.setDataDictionary(DataDictionary.valueOf(row.dataDictionary()));
          } else {
            datasheetDto.setDataDictionary(null);
          }
          datasheetDto.setCreatedBy(row.createdBy());
          datasheetDto.setCreatedTime(row.createdTime().toLocalDateTime());
          datasheetDtoMap.put(datasheetDto.getId(), datasheetDto);
        }

        passportDto.getDatasheets().add(datasheetDtoMap.get(row.datasheetId()));
      }

      passportDtoList.add(passportDto);
    }

    return passportDtoList;
  }

  /**
   * Validate the passport, throw if there is an error.
   *
   * @param dictionary The dictionary to validate against
   * @param passportData The passport data to validate
   */
  private void validatePassportData(DataDictionary dictionary, JsonNode passportData)
      throws JsonValidationException {
    dictionaryAdapterFactory.getAdapter(dictionary).validatePassportData(passportData);
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
}
