package com.oc.api.passport.controller;

import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 
 */
@RestController
@Tag(name = "Passport Entity", description = "Operations related to Passport Entity")
public class PassportEntityController {

	@Autowired
	PassportEntityService passportEntityService;

	@Operation(summary = "Create TemplateEntry and validates it")
	@PostMapping(value = "/api/templateEntry/", produces = { "application/text" }, consumes = { "application/json" })
	public String createTemplateEntry(
			@Parameter(description = "Template JSON filled with actual data") @RequestBody JsonNode templateEntry,
			String dictionaryName) {
		System.out.println(templateEntry.toString());
		return passportEntityService.createTemplateEntry(templateEntry, CommonUtil.convertToLowercase(dictionaryName));

	}

	@GetMapping("/passports/active/")
	public PassportEntity getActivePassportEntity(@RequestParam String peId) throws JsonMappingException, JsonProcessingException {
		return passportEntityService.getActivePassportEntity(peId);
	}
	
	@GetMapping("/passports/active/with-children/")
	public List<PassportEntity> getActivePassportEntitywithChildPE(@RequestParam String peId) throws JsonMappingException, JsonProcessingException {
		return passportEntityService.getActivePassportEntitywithChildPE(peId);
	}
	
	@Operation(summary = "Update the existing Passport entity")
	@PostMapping(value = "/api/passportEntity/update/", produces = { "application/text" }, consumes = { "application/json" })
	public String updatePassportEntity(
			@Parameter(description = "Updated Json with new values") @RequestBody JsonNode templateEntry,
			String peId) throws NoSuchAlgorithmException {
		System.out.println(templateEntry.toString());
		return passportEntityService.updatePassportEntity(templateEntry, peId);

	}
	
	@Operation(summary = "Create a template from the existing passport entity")
	@PostMapping(value = "/api/passportEntity/createTemplate/", produces = { "application/json" })
	public JsonNode createTemplateFromExistingPE(@RequestParam String peId) {
		try {
			return passportEntityService.createTemplateFromExistingPE(peId);
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return null;
	}
}
