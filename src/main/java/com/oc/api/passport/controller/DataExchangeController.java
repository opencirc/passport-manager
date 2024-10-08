package com.oc.api.passport.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Data Exchange", description = "Operations related to Import/Export of Passport Entities and its templates")
public class DataExchangeController {

	@Operation(summary = "Exports Passport Entity with log")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully exported PE", content = @Content),
			@ApiResponse(responseCode = "400", description = "Invalid file name", content = @Content),
			@ApiResponse(responseCode = "500", description = "Server error", content = @Content) })
	@GetMapping(value = "/api/PE/export")
	public String exportPassportEntity(
			@Parameter(description = "Name of the PE") @PathVariable String passportEntityName) {
		return null;

	}

	@Operation(summary = "Imports Passport Entity with log")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully exported PE", content = @Content),
			@ApiResponse(responseCode = "400", description = "Invalid file format", content = @Content),
			@ApiResponse(responseCode = "500", description = "Server error", content = @Content) })
	@PostMapping(value = "/api/PE/import")
	public String importPassportEntity(
			@Parameter(description = "Name of the PE") @PathVariable String fileName) {
		return null;

	}

	@Operation(summary = "Exports Passport Template with/without Generic info")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully exported Passport Template", content = @Content),
			@ApiResponse(responseCode = "400", description = "Invalid file name", content = @Content),
			@ApiResponse(responseCode = "500", description = "Server error", content = @Content) })
	@GetMapping(value = "/api/template/export")
	public String exportPassportTemplate(
			@Parameter(description = "Name of the PE") @PathVariable String passportEntityName) {
		return null;

	}
	
	
	@Operation(summary = "Imports Passport Template")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully exported Passport Template", content = @Content),
			@ApiResponse(responseCode = "400", description = "Invalid file name", content = @Content),
			@ApiResponse(responseCode = "500", description = "Server error", content = @Content) })
	@GetMapping(value = "/api/template/import")
	public String importPassportTemplate(
			@Parameter(description = "FilePath of the template file") @PathVariable String fileName) {
		return null;

	}

}
