package com.opencirc.api.passport.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.opencirc.api.passport.dto.CreatePassportRequestDto;
import com.opencirc.api.passport.dto.PassportDto;
import com.opencirc.api.passport.enums.DataDictionary;
import com.opencirc.api.passport.exception.InvalidInputException;
import com.opencirc.api.passport.exception.JsonValidationException;
import com.opencirc.api.passport.service.PassportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Endpoint for operations related to passport.
 */
@RestController
@Tag(name = "Passport", description = "Operations related to Passport")
public class PassportController {

    /**
     * Injecting PassportService class.
     */
    @Autowired
    private PassportService passportService;

    /**
     * Endpoint to create a passport.
     * @param data
     * @param dictionaryName
     * @return the status
     * @throws JsonValidationException
     * @throws InvalidInputException
     */
    @Operation(summary = "Creates Passport and validates it")
    @PostMapping(value = "/api/passport/dictionary/{dictionary}", produces = {
            "application/json" }, consumes = { "application/json" })
    public ResponseEntity<PassportDto> createPassportUsingDictionary(
            @io.swagger.v3.oas.annotations.parameters.RequestBody
            (description = "JSON template retrieved from external APIs, "
                    + "populated with actual data to create the Passport")
            @RequestBody CreatePassportRequestDto data,
            @Parameter(description = "Dictionary", required = true, in = ParameterIn.PATH)
            @PathVariable("dictionary") String dictionaryName)
                    throws InvalidInputException, JsonValidationException {
        return ResponseEntity.ok(passportService.createPassportUsingDictionary(
                DataDictionary.fromValue(dictionaryName), data));
    }

    /**
     * Endpoint to fetch the passport.
     * @param id
     * @return the passport in json
     */
    @Operation(summary = "Retrieves the Passport")
    @GetMapping("/api/passport/{id}")
    public ResponseEntity<PassportDto> getPassport(
            @Parameter(description = "Id of the Passport", required = true,
            in = ParameterIn.PATH)
            @PathVariable String id)
            throws JsonProcessingException {
        return ResponseEntity.ok(passportService.getPassport(id));
    }

    /**
     * Endpoint to fetch the children of the specified passport.
     * @param id
     * @return the passport in json
     * @throws JsonValidationException
     */
    @Operation(summary = "Get Passport and its children for the given ID")
    @GetMapping("/api/passport/{id}/children")
    public ResponseEntity<List<PassportDto>> getPassportChildren(
            @Parameter(description = "Id of the Passport",
            required = true, in = ParameterIn.PATH) @PathVariable String id)
            throws JsonProcessingException, JsonValidationException {
        return ResponseEntity.ok(passportService.getPassportChildren(id));
    }

    /**
     * Endpoint to fetch the children of the specified passport.
     * @param id
     * @return the passport in json
     * @throws JsonValidationException
     */
    @Operation(summary = "Get passport's immediate children for the given ID")
    @GetMapping("/api/passport/{id}/immediate-children")
    public ResponseEntity<List<PassportDto>>  getPassportImmediateChildren(
            @Parameter(description = "Id of the Passport",
            required = true, in = ParameterIn.PATH) @PathVariable String id)
            throws JsonProcessingException, JsonValidationException {
        return ResponseEntity.ok(passportService.getPassportImmedidateChildren(id));
    }

    /**
     * Endpoint to fetch all the root passports.
     * @return the passports without parent
     * @throws JsonValidationException
     */
    @Operation(summary = "Get all the root passports available")
    @GetMapping("/api/passport/root/all")
    public ResponseEntity<List<PassportDto>> getRootPassports() {
        return ResponseEntity.ok(passportService.getRootPassports());
    }

}
