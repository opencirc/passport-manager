package com.opencirc.api.passport.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencirc.api.passport.enums.DataDictionary;
import com.opencirc.api.passport.exception.JsonValidationException;
import com.opencirc.api.passport.service.DataDictionaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint for operations related to data dictionary. */
@RestController
@Tag(name = "Data Dictionary", description = "Operations related to template from Data Dictionary")
public class DataDictionaryController {

  /** Injecting DataDictionaryService class. */
  @Autowired private DataDictionaryService dataDictionaryService;

  /**
   * Returns list of classes fetched from bsdd.
   *
   * @param dictionaryName
   * @param query
   * @return list of class and its details
   */
  @GetMapping(
      value = "/api/dataDictionary/{dictionary}/class/search/{query}",
      produces = {"application/json"})
  @Operation(summary = "Get list of classes for the query text")
  public List<Map<String, String>> searchClass(
      @Parameter(description = "The dictionary", required = true) @PathVariable("dictionary")
          String dictionaryName,
      @Parameter(description = "The text to search for", required = true) @PathVariable
          String query) {
    return dataDictionaryService.searchClassesByText(
        DataDictionary.fromValue(dictionaryName), query);
  }

  /**
   * Returns Class Template with or without properties from the data dictionary using the provided
   * URI.
   *
   * @param dictionaryName
   * @param classUri
   * @param withProperties
   * @return the template with all the relevant properties
   */
  @Operation(summary = "Get class from DD for the requested uri")
  @PostMapping(
      value = "/api/dataDictionary/{dictionary}/class",
      produces = {"application/json"})
  public Object getClass(
      @Parameter(
              description = "Name of dictionary",
              required = true,
              example = "bsdd",
              in = ParameterIn.PATH)
          @PathVariable("dictionary")
          String dictionaryName,
      @Parameter(
              description = "URI for the classification",
              example =
                  "https://identifier.buildingsmart.org/uri/"
                      + "molio/cciconstruction/1.0/class/A-A__")
          @RequestBody
          String classUri,
      @Parameter(
              description = "Whether to return the class with properties",
              in = ParameterIn.QUERY)
          @RequestParam
          Boolean withProperties)
      throws JsonValidationException, JsonProcessingException {
    return dataDictionaryService.createClassTemplate(
        DataDictionary.fromValue(dictionaryName), classUri, withProperties);
  }

  /**
   * Returns list of properties fetched from bsdd.
   *
   * @param dictionaryName
   * @param query
   * @return the template with all the relevant properties
   */
  @Operation(summary = "Lists all the properties name and its URI matching the text")
  @GetMapping(
      value = "/api/dataDictionary/{dictionary}/property/search/{query}",
      produces = {"application/json"})
  public List<Map<String, String>> listProperties(
      @Parameter(description = "The dictionary", required = true, example = "bsdd")
          @PathVariable("dictionary")
          String dictionaryName,
      @Parameter(description = "The text to search for", required = true) @PathVariable
          String query) {
    return dataDictionaryService.listProperties(DataDictionary.fromValue(dictionaryName), query);
  }

  /**
   * Create template with selected properties.
   *
   * @param dictionaryName
   * @param propertiesUriList
   * @return the template with all the relevant properties
   */
  @Operation(summary = "Create template with selected properties")
  @PostMapping(
      value = "/api/dataDictionary/{dictionary}/properties",
      produces = {"application/json"},
      consumes = {"application/json"})
  public ObjectNode createTemplateWithProperties(
      @Parameter(description = "Name of library", required = true, example = "bsdd")
          @PathVariable("dictionary")
          String dictionaryName,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "List of Property URI",
              required = true)
          @RequestBody
          List<String> propertiesUriList)
      throws JsonValidationException {
    return dataDictionaryService.createTemplateWithProperties(
        DataDictionary.fromValue(dictionaryName), propertiesUriList);
  }
}
