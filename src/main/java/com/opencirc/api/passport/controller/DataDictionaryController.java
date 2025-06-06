package com.opencirc.api.passport.controller;

import java.util.List;
import java.util.Map;

import com.opencirc.api.passport.enums.DataDictionary;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.opencirc.api.passport.exception.JsonValidationException;
import com.opencirc.api.passport.service.DataDictionaryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 *
 */
@RestController
@Tag(name = "Templates", description = "Operations related to templates")
public class DataDictionaryController {

    /**
     * Injecting DataDictionaryService class.
     */
    @Autowired
    private DataDictionaryService dataDictionaryService;

    /**
     * Returns list of classes fetched from bsDD.
     *
     * @param dictionary
     * @param query
     *
     * @return list of class and its details
     */
    @GetMapping(value = "/api/data-dictionary/{dictionary}/class/search/{query}", produces = {
            "application/json" })
    @Operation(summary = "Get list of classes for the query text")
    public List<Map<String, String>> searchClass(
            @Parameter(description = "The dictionary",
                    required = true)
            @PathVariable String dictionary,
            @Parameter(description = "The text to search for",
                    required = true)
            @PathVariable String query) {
        return dataDictionaryService.searchClassesByText(DataDictionary.valueOf(dictionary), query);
    }

    /**
     * Returns list of classes fetched from bsDD.
     *
     * @param dictionary
     * @param classUri
     * @param withProperties
     * @return the template with all the relevant properties
     */
    @Operation(summary = "Get class from DD for the requested uri")
    @GetMapping(value = "/api/data-dictionary/{dictionary}/class/{classUri}/", produces = {
            "application/json" })
    public JsonNode getClass(
            @Parameter(description = "Name of dictionary", required = true, example = "bsdd", in = ParameterIn.PATH)
            @PathVariable String dictionary,
            @Parameter(description = "URI for the classification",
            example = "https://identifier.buildingsmart.org/uri/"
                    + "molio/cciconstruction/1.0/class/A-A__")
            @PathVariable String classUri,
            @Parameter(description = "Whether to return the class with properties", example = "bsdd", in = ParameterIn.QUERY)
            @RequestParam Boolean withProperties)
            throws JsonValidationException, JsonProcessingException {
        return dataDictionaryService.createClassTemplate(DataDictionary.valueOf(dictionary), classUri, withProperties);
    }

    /**
     * Returns list of properties fetched from bsDD.
     *
     * @param dictionary
     * @param query
     * @return the template with all the relevant properties
     */
    @Operation(summary = "Lists all the properties name and its URI matching the text")
    @GetMapping(value = "/api/data-dictionary/{dictionary}/property/search/{query}", produces = {
            "application/json" })
    public List<Map<String, String>> listProperties(
            @Parameter(description = "The dictionary",
                    required = true, example = "bsdd")
            @PathVariable String dictionary,
            @Parameter(description = "The text to search for",
                    required = true)
            @PathVariable String query) {
        return dataDictionaryService.listProperties(DataDictionary.valueOf(dictionary), query);

    }

    /**
     * Create template with selected properties.
     *
     * @TODO this must be changed
     * @param dictionary
     * @param propertiesUriList
     * @return the template with all the relevant properties
     */
    @Operation(summary = "Create template with selected properties")
    @PostMapping(value = "/api/template/properties", produces = {
            "application/json" }, consumes = { "application/json" })
    public JsonNode createTemplateWithProperties(
            @Parameter(description = "Name of library", required = true, example = "bsdd")
            @PathVariable String dictionary,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description =
            "List of Property URI", required = true)
            @RequestBody List<String> propertiesUriList)
            throws JsonValidationException {
        return dataDictionaryService.createTemplateWithProperties(DataDictionary.valueOf(dictionary), propertiesUriList);

    }


}
