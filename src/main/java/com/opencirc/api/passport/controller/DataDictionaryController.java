package com.opencirc.api.passport.controller;

import com.opencirc.api.passport.enums.Platform;
import com.opencirc.api.passport.service.PlatformService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Controller for operations related to a data dictionary. */
@RestController
@Tag(name = "Data Dictionary", description = "Operations related to template from Data Dictionary")
public class DataDictionaryController {

  @Autowired private PlatformService platformService;

  /** Returns a list of classes fetched from bsdd. */
  @GetMapping(
      value = "/api/dataDictionary/{platform}/class/search/{query}",
      produces = {"application/json"})
  @Operation(summary = "Get list of classes for the query text")
  public List<Map<String, String>> searchClass(
      @Parameter(description = "The platform", required = true) @PathVariable("platform")
          String platform,
      @Parameter(description = "The text to search for", required = true) @PathVariable
          String query) {
    return platformService.searchClassesByText(Platform.fromValue(platform), query);
  }

  /** Returns a list of properties fetched from bsdd. */
  @Operation(summary = "Lists all the properties name and its URI matching the text")
  @GetMapping(
      value = "/api/dataDictionary/{platform}/property/search/{query}",
      produces = {"application/json"})
  public List<Map<String, String>> listProperties(
      @Parameter(description = "The platform", required = true, example = "bsdd")
          @PathVariable("platform")
          String platform,
      @Parameter(description = "The text to search for", required = true) @PathVariable
          String query) {
    return platformService.listProperties(Platform.fromValue(platform), query);
  }
}
