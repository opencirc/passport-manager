package com.oc.api.passport.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.oc.api.passport.service.TemplateService;
import com.oc.api.passport.util.CommonUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 
 */
@RestController
@Tag(name = "Templates", description = "Operations related to templates")
public class TemplateController {

	@Autowired
	private TemplateService templateService;

	/**
	 * Returns list of classes fetched from bsDD
	 */
	@GetMapping(value = "/api/classes/search/{searchText}/{ddLibrary}", produces = { "application/json" })
	@Operation(summary = "Get list of classes for the query text")
	public List<Map<String, String>> listClassesByText(
			@Parameter(description = "The text to search for, minimum 3 characters", required = true) @PathVariable String searchText, @PathVariable String ddLibrary) {
		return templateService.searchClassesByText(searchText, ddLibrary);
	}

	/**
	 * Returns list of classes fetched from bsDD
	 */
	@Operation(summary = "Get class from DD for the requested class code")
	@GetMapping(value = "/api/classes/template/", produces = { "application/json" })
	public JsonNode getClassTemplateWithProperties(
			@Parameter(description = "Class Code", example = "EC000001") @RequestParam String code, @Parameter(description = "Name of library", required = true, example = "bsdd") @RequestParam String ddLibrary) {
		return templateService.getClassTemplatewithPropDetails(code, ddLibrary);
	}


	@Operation(summary = "Lists all the properties name and its URI matching the text")
	@GetMapping(value = "/api/listProperties/{searchText}/{ddLibrary}", produces = { "application/json" })
	public List<Map<String, String>> listProperties(
			@Parameter(description = "The text to search for, minimum 3 characters", required = true, example = "EN 338") @PathVariable String searchText,@Parameter(description = "Name of library", required = true, example = "bsdd") @PathVariable String ddLibrary) {
		return templateService.listProperties(searchText, CommonUtil.convertToLowercase(ddLibrary));

	}

	// This method is to get the details of the selected properties from Dictionary
	@Operation(summary = "Create template with selected properties")
	@PostMapping(value = "/api/createTemplateWithProperties/", produces = { "application/json" }, consumes = { "application/json"})
	public JsonNode createTemplateWithProperties(
			@Parameter(description = "List of Property name and its code", required = true, example = "{\"EN 318\": \"0b205c05-881f-44f3-83ca-568927baacfe\",\r\n"
					+ "    \"EN 338\": \"0b44b46f-03ac-42c4-a500-408d5d175c0f\"}") @RequestBody List<String> properties,@Parameter(description = "Name of library", required = true, example = "bsdd")  @RequestParam String ddLibrary) {
		return templateService.createTemplateWithProperties(properties, CommonUtil.convertToLowercase(ddLibrary));

	}

	@GetMapping(value = "/api/clear/", produces = { "application/json" })
	public String clearcache() {
		 templateService.clearCache();
		 return "success";

	}
	
	@GetMapping(value = "/api/lookCache/", produces = { "application/json" })
	public Map<String, Object> lookCache() {
		return templateService.lookCache();
		 
	}
}
