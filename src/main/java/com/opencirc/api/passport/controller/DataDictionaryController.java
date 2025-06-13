package com.opencirc.api.passport.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.opencirc.api.passport.enums.DataDictionary;
import com.opencirc.api.passport.exception.JsonValidationException;
import com.opencirc.api.passport.service.DataDictionaryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 *
 */
@RestController
@Tag(name = "Data Dictionary", description = "Operations related to templates"
        + " from Data Dictionary")
public class DataDictionaryController {

    /**
     * Injecting DataDictionaryService class.
     */
    @Autowired
    private DataDictionaryService dataDictionaryService;

    /**
     * Returns list of classes fetched from bsDD.
     *
     * @param dictionaryName
     * @param query
     *
     * @return list of class and its details
     */
    @GetMapping(value = "/api/data-dictionary/{dictionary}/class/search/{query}",
            produces = {
            "application/json" })
    @Operation(summary = "Get list of classes for the query text")
    public List<Map<String, String>> searchClass(
            @Parameter(description = "The dictionary",
                    required = true)
            @PathVariable("dictionary") String dictionaryName,
            @Parameter(description = "The text to search for",
                    required = true)
            @PathVariable String query) {
        return dataDictionaryService.searchClassesByText(DataDictionary
                .fromValue(dictionaryName), query);
    }

    /**
     * Returns list of classes fetched from bsDD.
     *
     * @param dictionaryName
     * @param classUri
     * @param withProperties
     * @return the template with all the relevant properties
     */
    @Operation(summary = "Get class from DD for the requested uri")
    @PostMapping(value = "/api/data-dictionary/{dictionary}/class", produces = {
            "application/json" })
    public JsonNode getClass(
            @Parameter(description = "Name of dictionary", required = true,
            example = "bsdd", in = ParameterIn.PATH)
            @PathVariable("dictionary") String dictionaryName,
            @Parameter(description = "URI for the classification",
            example = "https://identifier.buildingsmart.org/uri/"
                    + "molio/cciconstruction/1.0/class/A-A__")
            @RequestBody String classUri,
            @Parameter(description = "Whether to return the class with properties",
            example = "bsdd", in = ParameterIn.QUERY)
            @RequestParam Boolean withProperties)
            throws JsonValidationException, JsonProcessingException {
        return dataDictionaryService.createClassTemplate(DataDictionary
                .fromValue(dictionaryName), classUri, withProperties);
    }

    /**
     * Returns list of properties fetched from bsDD.
     *
     * @param dictionaryName
     * @param query
     * @return the template with all the relevant properties
     */
    @Operation(summary = "Lists all the properties name and its URI matching the text")
    @GetMapping(value = "/api/data-dictionary/{dictionary}/property/search/{query}",
    produces = {"application/json" })
    public List<Map<String, String>> listProperties(
            @Parameter(description = "The dictionary",
                    required = true, example = "bsdd")
            @PathVariable("dictionary") String dictionaryName,
            @Parameter(description = "The text to search for",
                    required = true)
            @PathVariable String query) {
        return dataDictionaryService.listProperties(DataDictionary
                .fromValue(dictionaryName), query);

    }

    /**
     * Create template with selected properties.
     *
     * @TODO this must be changed
     * @param dictionaryName
     * @param propertiesUriList
     * @return the template with all the relevant properties
     */
    @Operation(summary = "Create template with selected properties")
    @PostMapping(value = "/api/template/properties/{dictionary}", produces = {
            "application/json" }, consumes = { "application/json" })
    public JsonNode createTemplateWithProperties(
            @Parameter(description = "Name of library", required = true, example = "bsdd")
            @PathVariable("dictionary") String dictionaryName,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description =
            "List of Property URI", required = true)
            @RequestBody List<String> propertiesUriList)
            throws JsonValidationException {
        return dataDictionaryService.createTemplateWithProperties(DataDictionary
                .fromValue(dictionaryName), propertiesUriList);

    }


}
