package com.oc.api.passport.controller;

import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.oc.api.passport.model.PassportEntity;
import com.oc.api.passport.service.PassportEntityService;
import com.oc.api.passport.util.CommonUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 
 */
@RestController
@Tag(name = "Passport Entity", description = "Operations related to Passport Entity")
public class PassportEntityController {

	@Autowired
	PassportEntityService passportEntityService;

	@Operation(summary = "Create PassportEntity, validates and Persist")
	@PostMapping(value = "/api/passportEntity/create/", produces = { "application/text" }, consumes = {
			"application/json" })
	public String createPassportEntity(
			@RequestBody(description = "JSON template retrieved from external APIs, populated with actual data to create the PassportEntity", required = true, content = @Content(mediaType = "application/json")) JsonNode templateEntry,
			String dictionaryName) {
		System.out.println(templateEntry.toString());
		return passportEntityService.createTemplateEntry(templateEntry, CommonUtil.convertToLowercase(dictionaryName));

	}

	@Operation(summary = "Retrieves the PassportEntity")
	@GetMapping("/passports/active/")
	public JsonNode getActivePassportEntity(
			@Parameter(description = "Id of the Passport Entity", required = true) @RequestParam String passportEntityId)
			throws JsonMappingException, JsonProcessingException {
		return passportEntityService.getActivePassportEntity(passportEntityId);
	}

	@Operation(summary = "Retrieves the PassportEntity and all its descendants")
	@GetMapping("/passports/active/with-children/")
	public List<PassportEntity> getActivePassportEntitywithChildPE(
			@Parameter(description = "Id of the Passport Entity", required = true) @RequestParam String passportEntityId)
			throws JsonMappingException, JsonProcessingException {
		return passportEntityService.getActivePassportEntitywithChildPE(passportEntityId);
	}

	@Operation(summary = "Update the existing Passport entity")
	@PostMapping(value = "/api/passportEntity/update/", produces = { "application/text" }, consumes = {
			"application/json" })
	public String updatePassportEntity(
			@RequestBody(description = "Passport Entity retrieved from other API is filled with new data to update the existing PassportEntity", required = true, content = @Content(mediaType = "application/json")) JsonNode templateEntry,
			@Parameter(description = "Id of the Passport Entity", required = true) String passportEntityId)
			throws NoSuchAlgorithmException {
		System.out.println(templateEntry.toString());
		return passportEntityService.updatePassportEntity(templateEntry, passportEntityId);

	}

	@Operation(summary = "Create a template from the existing passport entity")
	@PostMapping(value = "/api/passportEntity/createTemplate/", produces = { "application/json" })
	public JsonNode createTemplateFromExistingPE(
			@Parameter(description = "Id of the Passport Entity", required = true) @RequestParam String passportEntityId,
			@Parameter(description = "Set to true if extracted template needs to persisted", schema = @Schema(defaultValue = "false")) @RequestParam(required = false) boolean saveTemplate,
			@Parameter(description = "Provide name to extracted template for future retrieval", schema = @Schema(nullable = true)) @RequestParam(required = false) String templateName) {
		try {
			return passportEntityService.createTemplateFromExistingPE(passportEntityId, saveTemplate, templateName);
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return null;
	}
}
