package com.opencirc.api.passport.service;

import java.io.IOException;
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
import com.opencirc.api.passport.constants.AppConstants;
import com.opencirc.api.passport.context.UserContext;
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
     * Injecting UserContext class.
     */
    private final UserContext userContext;

    /**
     * Constructor.
     * @param datasheetRepository
     * @param passportRepository
     * @param passportDatasheetMappingRepository
     * @param dictionaryAdapterFactory
     * @param objectMapper
     * @param userContext
     */
    public PassportService(DatasheetRepository datasheetRepository,
                           PassportRepository passportRepository,
                           PassportDatasheetMappingRepository
                           passportDatasheetMappingRepository,
                           DictionaryAdapterFactory dictionaryAdapterFactory,
                           ObjectMapper objectMapper, UserContext userContext) {
        this.datasheetRepository = datasheetRepository;
        this.passportRepository = passportRepository;
        this.passportDatasheetMappingRepository = passportDatasheetMappingRepository;
        this.dictionaryAdapterFactory = dictionaryAdapterFactory;
        this.objectMapper = objectMapper;
        this.userContext = userContext;
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
        String reqCreatedById = data.getCreatedById();
        if (reqCreatedById == null || reqCreatedById.isBlank()) {
            rawPassport.setCreatedById(userContext.getCurrentUserId());
        } else {
            rawPassport.setCreatedById(data.getCreatedById());
        }

        if (data.getCreatedBy() == null || data.getCreatedBy().toString().isBlank()) {
            rawPassport.setCreatedBy(userContext.getCurrentUserInformation());
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
        if (reqCreatedById == null || reqCreatedById.isBlank()) {
            datasheet.setCreatedById(userContext.getCurrentUserId());
        } else {
            datasheet.setCreatedById(data.getCreatedById());
        }

        if (data.getCreatedBy() == null) {
            datasheet.setCreatedBy(userContext.getCurrentUserInformation());
        } else {
            datasheet.setCreatedBy(data.getCreatedBy());
        }
        datasheet = datasheetRepository.save(datasheet);

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
        Optional<List<PassportDatasheetResultMapDto>> optionalPassportList =
                passportRepository.findPassportWithDescendants(id);

        List<PassportDatasheetResultMapDto> resultRows = optionalPassportList
                .orElse(Collections.emptyList());

        if (resultRows.isEmpty()) {
            throw new HttpServerErrorException(HttpStatus.NOT_FOUND,
                    "No active passports found with children");
        }

        Map<String, PassportDto> dtoById = new LinkedHashMap<>();
        for (PassportDatasheetResultMapDto row : resultRows) {
            String passportId = row.getPassportId();

            PassportDto passportDto = dtoById.computeIfAbsent(passportId, key -> {

                CreatedByDto createdByDto = null;
                String passportCreatedByJson = row.getCreatedBy();
                if (passportCreatedByJson != null && !passportCreatedByJson.isBlank()) {
                    try {
                        createdByDto = objectMapper.readValue(passportCreatedByJson,
                                CreatedByDto.class);
                    } catch (IOException e) {
                        throw new RuntimeException("Error parsing createdBy JSON", e);
                    }
                }
                PassportDto dto = new PassportDto();
                dto.setId(passportId);
                dto.setName(row.getPassportName());
                dto.setStatus(Passport.Status.fromValue(row.getStatus()));
                dto.setCreatedById(row.getCreatedById());
                dto.setCreatedBy(createdByDto);
                dto.setCreatedTime(row.getCreatedTime().toLocalDateTime());
                dto.setDatasheets(new ArrayList<>());
                return dto;
            });

            if (row.getDatasheetId() != null) {
                boolean alreadyExists = passportDto.getDatasheets().stream()
                        .anyMatch(ds -> Objects.equals(ds.getId(), row.getDatasheetId()));

                if (!alreadyExists) {
                    DatasheetDto datasheetDto = new DatasheetDto();
                    datasheetDto.setId(row.getDatasheetId());
                    datasheetDto.setData(objectMapper.readTree(row.getData()));
                    if (row.getDataCategory() != null) {
                        datasheetDto.setDataCategory(DataCategory.fromValue(row
                                .getDataCategory()));
                    } else {
                        datasheetDto.setDataCategory(null);
                    }

                    if (row.getDataDictionary() != null) {
                        datasheetDto.setDataDictionary(DataDictionary
                                .valueOf(row.getDataDictionary()));
                    } else {
                        datasheetDto.setDataDictionary(null);
                    }
                    datasheetDto.setCreatedById(row.getCreatedById());

                    CreatedByDto createdByDto = null;
                    String datasheetCreatedByJson = row.getCreatedBy();
                    if (datasheetCreatedByJson != null
                            && !datasheetCreatedByJson.isBlank()) {
                        createdByDto = objectMapper.readValue(datasheetCreatedByJson,
                                CreatedByDto.class);
                    }

                    datasheetDto.setCreatedBy(createdByDto);
                    datasheetDto.setCreatedTime(row.getCreatedTime().toLocalDateTime());

                    passportDto.getDatasheets().add(datasheetDto);
                }
            }
        }

        for (PassportDatasheetResultMapDto row : resultRows) {
            String passportId = row.getPassportId();
            String parentId = row.getParentId();
            if (parentId != null && dtoById.containsKey(parentId)) {
                PassportDto child = dtoById.get(passportId);
                PassportDto parent = dtoById.get(parentId);
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
        Optional<List<PassportDatasheetResultMapDto>> optionalPassportList =
                passportRepository.findImmediateChildren(passportId);

        List<PassportDatasheetResultMapDto> resultRows = optionalPassportList
                .orElse(Collections.emptyList());

        List<PassportDto> passportDtoList = new ArrayList<>();
        Map<String, DatasheetDto> datasheetDtoMap = new LinkedHashMap<>();

        for (PassportDatasheetResultMapDto row : resultRows) {
            PassportDto passportDto = new PassportDto();
            passportDto.setId(row.getPassportId());
            passportDto.setName(row.getPassportName());
            passportDto.setStatus(Passport.Status.fromValue(row.getStatus()));
            passportDto.setCreatedById(row.getCreatedById());

            CreatedByDto createdByDto = objectMapper.readValue(
                    row.getCreatedBy(), CreatedByDto.class
                );
            passportDto.setCreatedBy(createdByDto);
            passportDto.setCreatedTime(row.getCreatedTime().toLocalDateTime());
            passportDto.setDatasheets(new ArrayList<>());

            if (row.getDatasheetId() != null) {
                if (!datasheetDtoMap.containsKey(row.getDatasheetId())) {
                    DatasheetDto datasheetDto = new DatasheetDto();
                    datasheetDto.setId(row.getDatasheetId());
                    datasheetDto.setData(objectMapper.readTree(row.getData()));
                    if (row.getDataCategory() != null) {
                        datasheetDto.setDataCategory(DataCategory.fromValue(row
                                .getDataCategory()));
                    } else {
                        datasheetDto.setDataCategory(null);
                    }

                    if (row.getDataDictionary() != null) {
                        datasheetDto.setDataDictionary(DataDictionary
                                .valueOf(row.getDataDictionary()));
                    } else {
                        datasheetDto.setDataDictionary(null);
                    }
                    datasheetDto.setCreatedById(row.getCreatedById());
                    CreatedByDto datasheetCreatedBy = objectMapper.readValue(
                            row.getCreatedBy(), CreatedByDto.class
                        );
                    datasheetDto.setCreatedBy(datasheetCreatedBy);
                    datasheetDto.setCreatedTime(row.getCreatedTime().toLocalDateTime());
                    datasheetDtoMap.put(datasheetDto.getId(), datasheetDto);
                }

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


}
