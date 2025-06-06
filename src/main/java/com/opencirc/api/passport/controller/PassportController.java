package com.opencirc.api.passport.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.opencirc.api.passport.dto.PassportDto;
import com.opencirc.api.passport.enums.DataDictionary;
import com.opencirc.api.passport.exception.InvalidInputException;
import com.opencirc.api.passport.exception.JsonValidationException;
import com.opencirc.api.passport.service.PassportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
     * @param dictionary
     * @return the status
     * @throws JsonValidationException
     * @throws InvalidInputException
     */
    @Operation(summary = "Creates Passport and validates it")
    @PostMapping(value = "/api/passport-entity/dictionary/{dictionary}/", produces = {
            "application/text" }, consumes = { "application/json" })
    public ResponseEntity<PassportDto> createPassportUsingDictionary(
            @io.swagger.v3.oas.annotations.parameters.RequestBody
            (description = "JSON template retrieved from external APIs, "
                    + "populated with actual data to create the Passport")
            @RequestBody JsonNode data,
            @Parameter(description = "Dictionary", required = true, in = ParameterIn.PATH)
            @PathVariable String dictionary) // @TODO this should be a DataDictionary
                    throws InvalidInputException, JsonValidationException {
        return ResponseEntity.ok(passportService.createPassportUsingDictionary(DataDictionary.valueOf(dictionary), data));
    }

    /**
     * Endpoint to fetch the passport.
     * @param id
     * @return the passport in json
     */
    @Operation(summary = "Retrieves the Passport")
    @GetMapping("/api/passport/{id}/")
    public ResponseEntity<PassportDto> getPassport(
            @Parameter(description = "Id of the Passport", required = true, in = ParameterIn.PATH)
            @PathVariable String id,
            @Parameter(description = "Whether to return the children", required = false, in = ParameterIn.QUERY)
            @RequestParam Boolean includeChildren)
            throws JsonProcessingException {
        return ResponseEntity.ok(passportService.getPassport(id, includeChildren));
    }
}
