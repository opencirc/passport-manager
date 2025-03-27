package com.oc.api.passport.controller;

import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.oc.api.passport.dto.PassportEntityTemplateDto;
import com.oc.api.passport.model.PassportEntity;
import com.oc.api.passport.service.PassportEntityService;
import com.oc.api.passport.util.CommonUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Endpoint for operations related to passport entity.
 */
@RestController
@Tag(name = "Passport Entity", description = "Operations related to Passport Entity")
public class PassportEntityController {

    /**
     * Injecting PassportEntityService class.
     */
    @Autowired
    private PassportEntityService passportEntityService;

    /**
     * Endpoint to create template entry.
     * @param templateEntry
     * @param dictionaryName
     * @return the status
     */
    @Operation(summary = "Creates Passport and validates it")
    @PostMapping(value = "/api/templateEntry", produces = {
            "application/text" }, consumes = { "application/json" })
    public String createTemplateEntry(
            @io.swagger.v3.oas.annotations.parameters.RequestBody
            (description = "JSON template retrieved from external APIs, "
                    + "populated with actual data to create the PassportEntity")
            @RequestBody JsonNode templateEntry,
            @Parameter(description = "Dictionary Name", required = true)
            @RequestParam String dictionaryName) {
        return passportEntityService.createTemplateEntry(templateEntry,
                CommonUtil.convertToLowercase(dictionaryName));

    }

    /**
     * Endpoint to fetch the passport.
     * @param passportEntityId
     * @return the passport in json
     */
    @Operation(summary = "Retrieves the PassportEntity")
    @GetMapping("/api/passports/active/")
    public ResponseEntity<?> getActivePassportEntity(
            @Parameter(description = "Id of the Passport Entity", required = true)
            @RequestParam String passportEntityId)
            throws JsonMappingException, JsonProcessingException {
        System.out.println("passport id requested : " + passportEntityId);
        JsonNode passport = passportEntityService
                .getActivePassportEntity(passportEntityId);

        return ResponseEntity.ok(Collections.singletonMap("passport", passport));
    }

    /**
     * Endpoint to fetch the passport and its children.
     * @param passportEntityId
     * @return the passports
     */
    @Operation(summary = "Retrieves the PassportEntity and all its descendants")
    @GetMapping("/passports/active/with-children/")
    public List<PassportEntity> getActivePassportEntitywithChildPE(
            @Parameter(description = "Id of the Passport Entity", required = true)
            @RequestParam String passportEntityId)
            throws JsonMappingException, JsonProcessingException {
        return passportEntityService
                .getActivePassportEntitywithChildPE(passportEntityId);
    }

    /**
     * Endpoint to update the passport.
     * @param passportEntityId
     * @param templateEntry
     * @return the status
     */
    @Operation(summary = "Update the existing Passport entity")
    @PostMapping(value = "/api/passportEntity/update/", produces = {
            "application/text" }, consumes = { "application/json" })
    public String updatePassportEntity(@RequestBody JsonNode templateEntry,
            @Parameter(description = "Id of the Passport Entity", required = true)
    @RequestParam String passportEntityId)
            throws NoSuchAlgorithmException {
        return passportEntityService.updatePassportEntity(templateEntry,
                passportEntityId);

    }

    /**
     * Endpoint to create a template from the existing passport entity.
     * @param passportEntityId
     * @param saveTemplate
     * @param templateName
     * @return the template
     */
    @Operation(summary = "Create a template from the existing passport entity")
    @PostMapping(value = "/api/passportEntity/createTemplate/", produces = {
            "application/json" })
    public JsonNode createTemplateFromExistingPE(
            @Parameter(description = "Id of the Passport Entity", required = true)
            @RequestParam String passportEntityId,
            @Parameter(description = "Set to true if"
                    + "extracted template needs to persisted")
            @RequestParam(required = false) boolean saveTemplate,
            @Parameter(description = "Provide name to extracted"
                    + "template for future retrieval")
            @RequestParam(required = false) String templateName) {
        try {
            return passportEntityService.createTemplateFromExistingPE(
                    passportEntityId, saveTemplate, templateName);
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * Endpoint to retrieve template from database.
     * @param templateName
     * @return the template
     */
    @Operation(summary = "Retrieves the persisted Template")
    @GetMapping("/passports/retrieveTemplate/")
    public ResponseEntity<PassportEntityTemplateDto> getPersistedTemplate(
            @Parameter(description = "Name of the Template stored in DB", required = true)
            @RequestParam String templateName)
            throws JsonMappingException, JsonProcessingException {
        return ResponseEntity
                .ok(passportEntityService.getPersistedTemplate(templateName));
    }


    /**
     * Endpoint to list template from database.
     * @return the template
     */
    @Operation(summary = "Lists all the persisted Template")
    @GetMapping("/passports/listPersistedTemplate/")
    public ResponseEntity<List<PassportEntityTemplateDto>> listPersistedTemplates()
            throws JsonMappingException, JsonProcessingException {
        return ResponseEntity.ok(passportEntityService.listPersistedTemplate());
    }

}
