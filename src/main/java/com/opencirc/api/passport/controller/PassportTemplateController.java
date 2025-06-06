package com.opencirc.api.passport.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.opencirc.api.passport.dto.PassportTemplateDto;
import com.opencirc.api.passport.model.PassportTemplate;
import com.opencirc.api.passport.service.PassportTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoint for operations related to passport.
 */
@RestController
@Tag(name = "Passport template", description = "Operations related to Passport templates")
public class PassportTemplateController {

    /**
     * Injecting PassportTemplateService class.
     */
    @Autowired
    private PassportTemplateService passportTemplateService;

    /**
     * Endpoint to create a template from the existing passport.
     * @param passportId
     * @param dryRun
     * @param templateName
     * @return the template
     */
    @Operation(summary = "Create a template from the existing passport")
    @PostMapping(value = "/api/passport-template/{passportId}/", produces = {
            "application/json" })
    public ResponseEntity<PassportTemplateDto> createPassportTemplateFromPassport(
            @Parameter(description = "Id of the Passport", required = true)
            @RequestParam String passportId,
            @Parameter(description = "Set to true if extracted template needs to persisted")
            @RequestParam(required = false) boolean dryRun,
            @Parameter(description = "Provide name to extracted"
                    + "template for future retrieval")
            @RequestBody(required = false) String templateName) throws JsonProcessingException {
        return ResponseEntity.ok(passportTemplateService.createTemplateFromPassport(
                passportId, dryRun, templateName));
    }


    /**
     * Endpoint to retrieve passport template.
     * @param id
     * @return the template
     */
    @Operation(summary = "Retrieves the persisted Template")
    @GetMapping("/api/passport-template/{id}/")
    public ResponseEntity<PassportTemplateDto> getPersistedTemplate(
            @Parameter(description = "Name of the Template stored in DB", required = true)
            @PathVariable Long id)
            throws JsonMappingException, JsonProcessingException {
        return ResponseEntity
                .ok(passportTemplateService.getPassportTemplate(id));
    }


    /**
     * Endpoint to list template from database.
     * @return the template
     */
    @Operation(summary = "Lists all the persisted Template")
    @GetMapping("/api/passport-templates/all")
    public ResponseEntity<List<PassportTemplateDto>> getPassportTemplates()
            throws JsonMappingException, JsonProcessingException {
        return ResponseEntity.ok(passportTemplateService.getAllPassportTemplates());
    }
}
