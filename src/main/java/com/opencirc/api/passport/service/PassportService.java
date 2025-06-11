package com.opencirc.api.passport.service;

import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.opencirc.api.passport.adapter.DictionaryAdapter;
import com.opencirc.api.passport.adapter.DictionaryAdapterFactory;
import com.opencirc.api.passport.constants.AppConstants;
import com.opencirc.api.passport.dao.DatasheetRepository;
import com.opencirc.api.passport.dao.PassportDatasheetMappingRepository;
import com.opencirc.api.passport.dao.PassportRepository;
import com.opencirc.api.passport.dao.PassportTemplateRepository;
import com.opencirc.api.passport.dto.CreatePassportRequestDto;
import com.opencirc.api.passport.dto.PassportDto;
import com.opencirc.api.passport.enums.DataDictionary;
import com.opencirc.api.passport.exception.InvalidInputException;
import com.opencirc.api.passport.exception.JsonValidationException;
import com.opencirc.api.passport.model.Datasheet;
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
     * Injecting PassportTemplateRepository class.
     */
    @Autowired
    private PassportTemplateRepository passportTemplateRepository;

    /**
     * Injecting DictionaryAdapterFactory class.
     */
    @Autowired
    private DictionaryAdapterFactory dictionaryAdapterFactory;

    /**
     * Creates template Entry.
     *
     * @param dictionary
     * @param datasheetData
     * @return Passport DTO from passport
     */
    public PassportDto createPassportUsingDictionary(DataDictionary dictionary, CreatePassportRequestDto data)
            throws InvalidInputException {
        JsonNode dataSheetData = data.getDataSheetData();
        try {
            validatePassportData(dictionary, dataSheetData);
        } catch (JsonValidationException e) {
            throw new HttpServerErrorException(HttpStatusCode.valueOf(422), e.getMessage());
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
        datasheet.setData(dataSheetData);
        datasheet.setDataCategory(null);
        datasheet.setCreatedBy(data.getCreatedBy());
        datasheet.setCreatedTime(data.getCreatedTime());
        datasheetRepository.save(datasheet);

        PassportDatasheetMapping passportDatasheet = new PassportDatasheetMapping();
        passportDatasheet.setPassport(rawPassport);
        passportDatasheet.setDatasheet(datasheet);
        passportDatasheetMappingRepository.save(passportDatasheet);

        return PassportDto.from(passport);
    }

    /**
     * Retrieves active passport.
     *
     * @param id
     * @return Passport DTO from passport
     */
    public PassportDto getPassport(String id, boolean includeChildren)
            throws JsonProcessingException {
        Optional<Passport> optionalPassport = passportRepository.findPassport(id);
        if (optionalPassport.isEmpty()
                || optionalPassport.get().getStatus() != Passport.Status.ACTIVE) {
            throw new HttpServerErrorException(HttpStatusCode.valueOf(404),
                    "No active passport found");
        }

         return PassportDto.from(optionalPassport.get());
    }
    
    /**
     * Retrieves active passport.
     *
     * @param id
     * @return Passport DTO from passport
     */
    public List<PassportDto> getPassportWithChildren(String id)
            throws JsonProcessingException {

            Optional<List<Passport>> optionalPassportList = passportRepository.findActivePassportWithDescendant(id);

            if (optionalPassportList.isEmpty() || optionalPassportList.get().isEmpty()) {
                throw new HttpServerErrorException(HttpStatus.NOT_FOUND, "No active passports found with descendants");
            }

            return optionalPassportList.get().stream()
                    .map(PassportDto::from)
                    .collect(Collectors.toList());

    }

    /**
     * Validate the passport, throw if there is an error
     *
     * @param dictionary
     * @param passportData
     */
    private void validatePassportData(DataDictionary dictionary, JsonNode passportData)
            throws JsonValidationException {
        dictionaryAdapterFactory.getAdapter(dictionary).validatePassportData(passportData);
    }
    




}
