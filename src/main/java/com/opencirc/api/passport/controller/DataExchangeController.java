package com.opencirc.api.passport.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Endpoint controller for Data Exchange for importing and exporting passport entities.
 */
@Hidden
@RestController
@Tag(name = "Data Exchange", description = "Operations related to Import/Export of "
        + "Passport Entities and its templates")
public class DataExchangeController {

    /**
     * Endpoint to export passport with logs.
     *
     * @param passportName
     * @return the status
     */
    @Operation(summary = "Exports Passport with log")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully exported PE",
                    content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid file name",
            content = @Content),
            @ApiResponse(responseCode = "500", description = "Server error",
            content = @Content) })
    @GetMapping(value = "/api/PE/export")
    public String exportPassport(
            @Parameter(description = "Name of the PE") @PathVariable String
            passportName) {
        return null;

    }

    /**
     * Endpoint to import passport with logs.
     *
     * @param fileName
     * @return the status
     */
    @Operation(summary = "Imports Passport with log")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully exported PE",
                    content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid file format",
            content = @Content),
            @ApiResponse(responseCode = "500", description = "Server error",
            content = @Content) })
    @PostMapping(value = "/api/PE/import")
    public String importPassport(
            @Parameter(description = "Name of the PE") @PathVariable String fileName) {
        return null;

    }

    /**
     * Endpoint to export passport template.
     *
     * @param passportName
     * @return the status
     */
    @Operation(summary = "Exports Passport Template with/without Generic info")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully exported"
                    + "Passport Template", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid file name",
            content = @Content),
            @ApiResponse(responseCode = "500", description = "Server error",
            content = @Content) })
    @GetMapping(value = "/api/template/export")
    public String exportPassportTemplate(
            @Parameter(description = "Name of the PE") @PathVariable
            String passportName) {
        return null;

    }

    /**
     * Endpoint to imports passport template.
     *
     * @param fileName
     * @return the status
     */
    @Operation(summary = "Imports Passport Template")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully"
                    + "exported Passport Template", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid file name",
            content = @Content),
            @ApiResponse(responseCode = "500", description = "Server error",
            content = @Content) })
    @GetMapping(value = "/api/template/import")
    public String importPassportTemplate(
            @Parameter(description = "FilePath of the template file")
            @PathVariable String fileName) {
        return null;

    }

}
