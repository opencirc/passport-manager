package com.opencirc.api.passport.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencirc.api.passport.adapter.DictionaryAdapterFactory;
import com.opencirc.api.passport.constants.AppConstants;
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

@Service
public class PassportService {

    /**
     * Injecting DatasheetRepository class.
     */
    @Autowired
    private DatasheetRepository datasheetRepository;

    /**
     * Injecting PassportRepository class.
     */
    @Autowired
    private PassportRepository passportRepository;

    /**
     * Injecting PassportDatasheetMappingRepository class.
     */
    @Autowired
    private PassportDatasheetMappingRepository passportDatasheetMappingRepository;

    /**
     * Injecting ObjectMapper bean.
     */
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Injecting DictionaryAdapterFactory class.
     */
    @Autowired
    private DictionaryAdapterFactory dictionaryAdapterFactory;

    /**
     * Creates template Entry.
     *
     * @param dictionary
     * @param data
     * @return Passport DTO from passport
     */
    public PassportDto createPassportUsingDictionary(DataDictionary dictionary,
            CreatePassportRequestDto data)
            throws InvalidInputException {
        JsonNode datasheetData = data.getDatasheetData();
        try {
            validatePassportData(dictionary, datasheetData);
        } catch (JsonValidationException e) {
            throw new HttpServerErrorException(HttpStatus.UNPROCESSABLE_ENTITY,
                    e.getMessage());
        }

        int customLength = AppConstants.NUM_THIRTY_SIX;
        CUID cuid = CUID.randomCUID2(customLength);

        Passport rawPassport = new Passport();
        rawPassport.setId(cuid.toString());

        rawPassport.setName(data.getPassportName());
        rawPassport.setStatus(Passport.Status.ACTIVE);
        rawPassport.setCreatedBy(data.getCreatedBy());
        rawPassport.setCreatedTime(LocalDateTime.now());
        Passport passport = passportRepository.save(rawPassport);

        Datasheet datasheet = new Datasheet();
        datasheet.setData(datasheetData);
        datasheet.setDataCategory(DataCategory.fromValue(data.getDataCategory()));
        datasheet.setDataDictionary(dictionary);
        datasheet.setCreatedBy(data.getCreatedBy());
        datasheet.setCreatedTime(data.getCreatedTime());
        datasheet = datasheetRepository.save(datasheet);

        PassportDatasheetMapping passportDatasheet = new PassportDatasheetMapping();
        passportDatasheet.setPassport(rawPassport);
        passportDatasheet.setDatasheet(datasheet);
        PassportDatasheetMapping passportDatasheetMapping =
                passportDatasheetMappingRepository.save(passportDatasheet);
        //Set up the data sheet mapping details in the return value of the Passport DTO
        passport.setDatasheetMappings(new ArrayList<>());
        passport.getDatasheetMappings().add(passportDatasheetMapping);

        return PassportDto.from(passport);
    }

    /**
     * Retrieves passport.
     *
     * @param id
     * @return Passport DTO from passport
     */
    public PassportDto getPassport(String id)
            throws JsonProcessingException {

        Optional<Passport> optionalPassport = passportRepository
                .findPassport(id, Passport.Status.ACTIVE);
        if (optionalPassport.isEmpty()
                || optionalPassport.get().getStatus() != Passport.Status.ACTIVE) {
            throw new HttpServerErrorException(HttpStatus.NOT_FOUND,
                    "No active passport found");
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
                passportRepository.findActivePassportChildren(id);

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
                PassportDto dto = new PassportDto();
                dto.setId(passportId);
                dto.setName(row.getPassportName());
                dto.setStatus(Passport.Status.fromValue(row.getStatus()));
                dto.setCreatedBy(row.getCreatedBy());
                dto.setCreatedTime(row.getCreatedTime().toLocalDateTime());
                dto.setDatasheets(new ArrayList<>());
                return dto;
            });

            if (row.getDatasheetId() != null) {
                boolean alreadyExists = passportDto.getDatasheets().stream()
                        .anyMatch(ds -> ds.getId() == row.getDatasheetId());

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
                    datasheetDto.setCreatedBy(row.getCreatedBy());
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
        if (passports == null || passports.isEmpty()) {
            throw new HttpServerErrorException(HttpStatus.NOT_FOUND, "No active passport found");
        }

        return passports.stream()
                .map(PassportDto::from)
                .collect(Collectors.toList());

    }


}
