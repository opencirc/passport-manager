package com.oc.api.passport.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
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
	@PostMapping(value = "/api/templateEntry", produces = { "application/text" }, consumes = { "application/json"})
	public String createTemplateEntry(@Parameter(description = "Template JSON filled with actual data") @RequestBody JsonNode templateEntry, String dictionaryName) {
		System.out.println(templateEntry.toString());
		return passportEntityService.createTemplateEntry(templateEntry, CommonUtil.convertToLowercase(dictionaryName));

	}
	
	/*
	 * @GetMapping("/api/passportEntity/{peId}") public JsonNode
	 * getActivePassportEntity(@PathVariable String peId) { Optional<PassportEntity>
	 * passportEntity =
	 * passportEntityRepository.findByPassportEntityIdAndStatus(peId, "Active");
	 * 
	 * 
	 * }
	 */
}
