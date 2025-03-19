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
import com.oc.api.passport.exception.BsDDJsonValidationException;
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

    /**
     * Injecting TemplateService class.
     */
    @Autowired
    private TemplateService templateService;

    /**
     * Returns list of classes fetched from bsDD.
     *
     * @param searchText
     * @param ddLibrary
     *
     * @return list of class and its details
     */
    @GetMapping(value = "/api/classes/search/{searchText}/{ddLibrary}", produces = {
            "application/json" })
    @Operation(summary = "Get list of classes for the query text")
    public List<Map<String, String>> listClassesByText(
            @Parameter(description = "The text to search for, minimum 3 characters",
            required = true) @PathVariable String searchText,
            @PathVariable String ddLibrary) {
        return templateService.searchClassesByText(searchText, ddLibrary);
    }

    /**
     * Returns list of classes fetched from bsDD.
     *
     * @param uri
     * @param ddLibrary
     * @return the template with all the relevant properties
     * @throws BsDDJsonValidationException
     */
    @Operation(summary = "Get class from DD for the requested uri")
    @GetMapping(value = "/api/classes/template/", produces = {
            "application/json" })
    public JsonNode createClassTemplateWithProperties(
            @Parameter(description = "URI for the classification",
            example = "https://identifier.buildingsmart.org/uri/"
                    + "molio/cciconstruction/1.0/class/A-A__")
            @RequestParam String uri,
            @Parameter(description = "Name of library", required = true, example = "bsdd")
            @RequestParam String ddLibrary)
            throws BsDDJsonValidationException {
        return templateService.getClassTemplatewithPropDetails(uri, ddLibrary);
    }

    /**
     * Returns list of propeties fetched from bsDD.
     *
     * @param searchText
     * @param ddLibrary
     * @return the template with all the relevant properties
     * @throws BsDDJsonValidationException
     */
    @Operation(summary = "Lists all the properties name and its URI matching the text")
    @GetMapping(value = "/api/listProperties/{searchText}/{ddLibrary}", produces = {
            "application/json" })
    public List<Map<String, String>> listProperties(
            @Parameter(description = "The text to search for, minimum 3 characters",
            required = true, example = "EN 338") @PathVariable String searchText,
            @Parameter(description = "Name of library", required = true, example = "bsdd")
            @PathVariable String ddLibrary) {
        return templateService.listProperties(searchText,
                CommonUtil.convertToLowercase(ddLibrary));

    }

    /**
     * Create template with selected properties.
     *
     * @param propertiesUriList
     * @param ddLibrary
     * @return the template with all the relevant properties
     * @throws BsDDJsonValidationException
     */
    @Operation(summary = "Create template with selected properties")
    @PostMapping(value = "/api/createTemplateWithProperties/", produces = {
            "application/json" }, consumes = { "application/json" })
    public JsonNode createTemplateWithProperties(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description =
            "List of Property URI", required = true)
            @RequestBody List<String> propertiesUriList,
            @Parameter(description = "Name of library", required = true, example = "bsdd")
            @RequestParam String ddLibrary)
            throws BsDDJsonValidationException {
        return templateService.createTemplateWithProperties(propertiesUriList,
                CommonUtil.convertToLowercase(ddLibrary));

    }

}
