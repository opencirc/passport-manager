package com.opencirc.api.passport.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencirc.api.passport.adapter.DictionaryAdapterFactory;
import com.opencirc.api.passport.config.AppProperties;
import com.opencirc.api.passport.constants.AppConstants;
import com.opencirc.api.passport.dao.DatasheetRepository;
import com.opencirc.api.passport.dao.PassportDatasheetMappingRepository;
import com.opencirc.api.passport.dao.PassportRepository;
import com.opencirc.api.passport.dto.CreatePassportRequestDto;
import com.opencirc.api.passport.dto.CreatedByDto;
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

@Service
public class PassportService {

    /**
     * Injecting DatasheetRepository class.
     */
    private final DatasheetRepository datasheetRepository;

    /**
     * Injecting PassportRepository class.
     */
    private final PassportRepository passportRepository;

    /**
     * Injecting PassportDatasheetMappingRepository class.
     */
    private final PassportDatasheetMappingRepository passportDatasheetMappingRepository;

    /**
     * Injecting ObjectMapper bean.
     */
    private final ObjectMapper objectMapper;

    /**
     * Injecting DictionaryAdapterFactory class.
     */
    private final DictionaryAdapterFactory dictionaryAdapterFactory;


    /**
     * Injecting AppProperties class.
     */
    private final AppProperties appProperties;

    /**
     * Constructor.
     * @param datasheetRepository
     * @param passportRepository
     * @param passportDatasheetMappingRepository
     * @param dictionaryAdapterFactory
     * @param objectMapper
     * @param appProperties
     */
    public PassportService(DatasheetRepository datasheetRepository,
                           PassportRepository passportRepository,
                           PassportDatasheetMappingRepository
                           passportDatasheetMappingRepository,
                           DictionaryAdapterFactory dictionaryAdapterFactory,
                           ObjectMapper objectMapper,
                           AppProperties appProperties) {
        this.datasheetRepository = datasheetRepository;
        this.passportRepository = passportRepository;
        this.passportDatasheetMappingRepository = passportDatasheetMappingRepository;
        this.dictionaryAdapterFactory = dictionaryAdapterFactory;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
    }

    /**
     * Creates template Entry.
     *
     * @param dictionary
     * @param data
     * @return Passport DTO from passport
     */
    public PassportDto createPassportUsingDictionary(DataDictionary dictionary,
            CreatePassportRequestDto data) throws InvalidInputException {
        JsonNode datasheetData = data.getDatasheetData();
        try {
            validatePassportData(dictionary, datasheetData);
        } catch (JsonValidationException e) {
            throw new HttpServerErrorException(HttpStatus.UNPROCESSABLE_ENTITY,
                    e.getMessage());
        }

        int customLength = AppConstants.CUID_LENGTH;
        CUID cuid = CUID.randomCUID2(customLength);

        Passport rawPassport = new Passport();
        rawPassport.setId(cuid.toString());
        rawPassport.setName(data.getPassportName());
        rawPassport.setStatus(Passport.Status.ACTIVE);
        String createdById = data.getCreatedById();
        rawPassport.setCreatedById(createdById);

        if (createdById == null || createdById.isBlank()) {
            rawPassport.setCreatedBy(new CreatedByDto(appProperties.getSystemAdminName(),
                    appProperties.getSystemAdminEmail()));
        } else {
            rawPassport.setCreatedBy(data.getCreatedBy());
        }

        String parentId = data.getParentId();
        if (parentId != null && !parentId.isBlank()) {
            boolean parentExists = passportRepository.existsById(parentId);
            if (!parentExists) {
                throw new HttpServerErrorException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Invalid parentId: active parent not found");
            }
            rawPassport.setParentId(parentId);
        }
        Passport passport = passportRepository.save(rawPassport);

        Datasheet datasheet = new Datasheet();
        datasheet.setData(datasheetData);
        datasheet.setDataCategory(DataCategory.fromValue(data.getDataCategory()));
        datasheet.setDataDictionary(dictionary);
        datasheet.setCreatedById(data.getCreatedById());

        if (createdById == null || createdById.isBlank()) {
            datasheet.setCreatedBy(new CreatedByDto(appProperties.getSystemAdminName(),
                    appProperties.getSystemAdminEmail()));
        } else {
            datasheet.setCreatedBy(data.getCreatedBy());
        }
        datasheet = datasheetRepository.save(datasheet);

        PassportDatasheetMapping passportDatasheet = new PassportDatasheetMapping();
        passportDatasheet.setPassport(passport);
        passportDatasheet.setDatasheet(datasheet);
        PassportDatasheetMapping passportDatasheetMapping =
                passportDatasheetMappingRepository.save(passportDatasheet);
        passport.setDatasheetMappings(new ArrayList<>());
        passport.getDatasheetMappings().add(passportDatasheetMapping);

        return PassportDto.from(passport);
    }

    /**
     * Retrieves passport.
     *
     * @param passportId
     * @return Passport DTO from passport
     */
    public PassportDto getPassport(String passportId)
            throws JsonProcessingException {

        Optional<Passport> optionalPassport = passportRepository
                .findPassport(passportId, Passport.Status.ACTIVE);
        if (optionalPassport.isEmpty()) {
            throw new HttpServerErrorException(HttpStatus.NOT_FOUND,
                    "Could not find passport with ID " + passportId);
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
    public List<PassportDto> getPassportChildren(String id)
            throws JsonProcessingException {
        List<PassportDatasheetResultMapDto> resultRows = passportRepository
                .findPassportWithDescendants(id)
                .orElse(Collections.emptyList());

        if (resultRows.isEmpty()) {
            throw new HttpServerErrorException(HttpStatus.NOT_FOUND,
                    "No active passports found with children");
        }

        Map<String, PassportDto> dtoById = new LinkedHashMap<>();
        for (PassportDatasheetResultMapDto row : resultRows) {
            PassportDto passportDto = dtoById.computeIfAbsent(row.getPassportId(),
                    key -> buildPassportDto(row));

            if (row.getDatasheetId() != null && passportDto.getDatasheets().stream()
                    .noneMatch(ds -> Objects.equals(ds.getId(), row.getDatasheetId()))) {
                passportDto.getDatasheets().add(buildDatasheetDto(row));
            }
        }

        // Assign parent-child relationship
        for (PassportDatasheetResultMapDto row : resultRows) {
            if (row.getParentId() != null && dtoById.containsKey(row.getParentId())) {
                PassportDto child = dtoById.get(row.getPassportId());
                PassportDto parent = dtoById.get(row.getParentId());
                child.setParent(parent);
            }
        }

        return new ArrayList<>(dtoById.values());
    }

    /**
     * Retrieves the passports with the given parent ID.
     *
     * @param passportId
     * @return a list of {@link PassportDto} objects
     * @throws JsonProcessingException
     */
    public List<PassportDto> getImmediateChildren(String passportId)
            throws JsonProcessingException {
        List<PassportDatasheetResultMapDto> resultRows =
                passportRepository.findImmediateChildren(passportId)
                .orElse(Collections.emptyList());

        Map<String, DatasheetDto> datasheetDtoMap = new LinkedHashMap<>();
        List<PassportDto> passportDtoList = new ArrayList<>();

        for (PassportDatasheetResultMapDto row : resultRows) {
            PassportDto passportDto = buildPassportDto(row);

            if (row.getDatasheetId() != null) {
                datasheetDtoMap.putIfAbsent(row.getDatasheetId(),
                        buildDatasheetDto(row));
                passportDto.getDatasheets().add(datasheetDtoMap
                        .get(row.getDatasheetId()));
            }

            passportDtoList.add(passportDto);
        }

        return passportDtoList;
    }


    /**
     * Validate the passport, throw if there is an error.
     *
     * @param dictionary
     * @param passportData
     */
    private void validatePassportData(DataDictionary dictionary, JsonNode passportData)
            throws JsonValidationException {
        dictionaryAdapterFactory.getAdapter(dictionary)
        .validatePassportData(passportData);
    }

    /**
     * Retrieves all the root passports.
     *
     * @return Passport DTO list from passport
     */
    public List<PassportDto> getRootPassports() {
        List<Passport> passports = passportRepository
                .getRootPassports();
        if (passports == null) {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not retrieve root passports");

        }
        return passports.stream()
                .map(PassportDto::from)
                .collect(Collectors.toList());

    }


    private PassportDto buildPassportDto(PassportDatasheetResultMapDto row) {
        PassportDto dto = new PassportDto();
        dto.setId(row.getPassportId());
        dto.setName(row.getPassportName());
        dto.setStatus(Passport.Status.fromValue(row.getStatus()));
        dto.setCreatedById(row.getPassportCreatedById());
        dto.setCreatedBy(parseCreatedBy(row.getPassportCreatedBy()));
        if (row.getPassportCreatedTime() != null) {
            dto.setCreatedTime(row.getPassportCreatedTime());
        }
        dto.setDatasheets(new ArrayList<>());
        return dto;
    }

    private DatasheetDto buildDatasheetDto(PassportDatasheetResultMapDto row) {
        try {
            DatasheetDto dto = new DatasheetDto();
            dto.setId(row.getDatasheetId());
            JsonNode dataNode = null;
            String data = row.getData();
            if (data != null && !data.isBlank()) {
                dataNode = objectMapper.readTree(data);
            }
            dto.setData(dataNode);
            dto.setDataCategory(row.getDataCategory() != null
                    ? DataCategory.fromValue(row.getDataCategory())
                    : null);
            dto.setDataDictionary(row.getDataDictionary() != null
                    ? DataDictionary.fromValue(row.getDataDictionary())
                    : null);
            dto.setCreatedById(row.getDatasheetCreatedById());
            dto.setCreatedBy(parseCreatedBy(row.getDatasheetCreatedBy()));
            if (row.getDatasheetCreatedTime() != null) {
                dto.setCreatedTime(row.getDatasheetCreatedTime());
            }
            return dto;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(
                    "Error parsing datasheet JSON for datasheetId=" + row.getDatasheetId()
                    + ", passportId=" + row.getPassportId(), e);
        }
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
}
