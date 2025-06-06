package com.opencirc.api.passport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.opencirc.api.passport.adapter.DictionaryAdapterFactory;
import com.opencirc.api.passport.constants.AppConstants;
import com.opencirc.api.passport.dao.DatasheetRepository;
import com.opencirc.api.passport.dao.PassportDatasheetMappingRepository;
import com.opencirc.api.passport.dao.PassportRepository;
import com.opencirc.api.passport.dao.PassportTemplateRepository;
import com.opencirc.api.passport.dto.PassportDto;
import com.opencirc.api.passport.enums.DataDictionary;
import com.opencirc.api.passport.exception.InvalidInputException;
import com.opencirc.api.passport.exception.JsonValidationException;
import com.opencirc.api.passport.model.Datasheet;
import com.opencirc.api.passport.model.Passport;
import com.opencirc.api.passport.model.PassportDatasheetMapping;
import io.github.thibaultmeyer.cuid.CUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;

import java.time.LocalDateTime;
import java.util.Optional;

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
    public PassportDto createPassportUsingDictionary(DataDictionary dictionary, JsonNode datasheetData)
            throws InvalidInputException {
        try {
            validatePassportData(dictionary, datasheetData);
        } catch (JsonValidationException e) {
            throw new HttpServerErrorException(HttpStatusCode.valueOf(422), e.getMessage());
        }

        int customLength = AppConstants.NUM_THIRTY_SIX;
        CUID cuid = CUID.randomCUID2(customLength);

        Passport rawPassport = new Passport();
        rawPassport.setId(cuid.toString());
        // @TODO this is wrong, if the passport has a name, it should not be hidden
        // in the JSON data, it should be provided along with strongly structured/typed metadata
        rawPassport.setName(datasheetData.get("templateName").asText());
        rawPassport.setStatus(Passport.Status.ACTIVE);
        rawPassport.setCreatedBy("OCTest"); // Update this code when auth is
                                               // implemented
        rawPassport.setCreatedTime(LocalDateTime.now());
        Passport passport = passportRepository.save(rawPassport);

        Datasheet datasheet = new Datasheet();
        datasheet.setData(datasheetData);
        datasheet.setDictionary(dictionary);
        datasheet.setData(datasheetData);
        datasheet.setCreatedBy("OCTest");
        datasheet.setCreatedTime(LocalDateTime.now());
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
        if (optionalPassport.isEmpty() || optionalPassport.get().getStatus() != Passport.Status.ACTIVE) {
            throw new HttpServerErrorException(HttpStatusCode.valueOf(404), "No active passport found");
        }

        // @TODO implement includeChildren
        return PassportDto.from(optionalPassport.get());
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
