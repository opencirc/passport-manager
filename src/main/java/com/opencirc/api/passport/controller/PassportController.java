package com.opencirc.api.passport.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.opencirc.api.passport.context.UserContext;
import com.opencirc.api.passport.dto.AddDatasheetsToPassportUsingPlatformRequestDto;
import com.opencirc.api.passport.dto.CreatePassportUsingPlatformRequestDto;
import com.opencirc.api.passport.dto.DataDictionaryTreeStructureDto;
import com.opencirc.api.passport.dto.PassportDto;
import com.opencirc.api.passport.dto.UpdateDataRequestDto;
import com.opencirc.api.passport.enums.DataDictionary;
import com.opencirc.api.passport.enums.Platform;
import com.opencirc.api.passport.exception.InvalidDataDictionaryException;
import com.opencirc.api.passport.exception.InvalidInputException;
import com.opencirc.api.passport.exception.JsonValidationException;
import com.opencirc.api.passport.model.Datasheet;
import com.opencirc.api.passport.service.PassportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Controller for operations related to passports. */
@RestController
@Tag(name = "Passport", description = "Operations related to passports")
public class PassportController {

  @Autowired private PassportService passportService;

  @Autowired private UserContext userContext;

  /** Creates a passport using information from the provided platform. */
  @Operation(summary = "Creates a passport using information from the provided platform.")
  @PostMapping(
      value = "/api/passport/platform/{platform}",
      produces = {"application/json"},
      consumes = {"application/json"})
  public ResponseEntity<PassportDto> createPassportUsingPlatform(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Configuration for passport creation")
          @Valid
          @RequestBody
          CreatePassportUsingPlatformRequestDto data,
      @PathVariable
          @Parameter(description = "Dictionary platform", required = true, in = ParameterIn.PATH)
          String platform)
      throws InvalidInputException, JsonValidationException, JsonProcessingException {
    return ResponseEntity.ok(
        passportService.createPassportUsingPlatform(
            Platform.fromValue(platform), data, userContext.getCurrentUser()));
  }

  /** Creates multiple passports using information from the provided platforms. */
  @Operation(summary = "Creates multiple passports using information from the provided platforms.")
  @PostMapping(
      value = "/api/passport/platform/{platform}/batch",
      produces = {"application/json"},
      consumes = {"application/json"})
  public ResponseEntity<List<PassportDto>> batchCreatePassportsUsingPlatform(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Configuration for multiple passport creation")
          @Valid
          @RequestBody
          List<CreatePassportUsingPlatformRequestDto> data,
      @PathVariable
          @Parameter(description = "Dictionary platform", required = true, in = ParameterIn.PATH)
          String platform)
      throws InvalidInputException, JsonValidationException, JsonProcessingException {
    return ResponseEntity.ok(
        passportService.batchCreatePassportsUsingPlatform(
            Platform.fromValue(platform), data, userContext.getCurrentUser()));
  }

  /**
   * Creates a datasheet and adds it to the passport using information from the provided platform.
   */
  @Operation(
      summary =
          "Creates a set of datasheets using the provided platform and adds them to the passport.")
  @PostMapping(
      value = "/api/passport/{passportId}/datasheet",
      produces = {"application/json"},
      consumes = {"application/json"})
  public ResponseEntity<PassportDto> addDatasheetsToPassportUsingPlatform(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Configuration for adding datasheets to passport")
          @Valid
          @RequestBody
          AddDatasheetsToPassportUsingPlatformRequestDto data,
      @PathVariable @Parameter(description = "Passport ID", required = true, in = ParameterIn.PATH)
          String passportId)
      throws InvalidInputException, JsonValidationException, JsonProcessingException {
    return ResponseEntity.ok(
        passportService.addDatasheetsToPassportUsingPlatform(
            passportId,
            Platform.fromValue(data.getPlatform()),
            data.getPlatformId(),
            Datasheet.DataCategory.fromValue(data.getDataCategory()),
            userContext.getCurrentUser(),
            true));
  }

  /** Fetch a passport. */
  @Operation(summary = "Retrieves the Passport")
  @GetMapping("/api/passport/{passportId}")
  public ResponseEntity<PassportDto> getPassport(
      @Parameter(description = "ID of the Passport", required = true, in = ParameterIn.PATH)
          @PathVariable
          String passportId) {
    return ResponseEntity.ok(passportService.getPassport(passportId));
  }

  /** Fetch the specified passport and its descendants. */
  @Operation(summary = "Get Passport and its children for the given ID")
  @GetMapping("/api/passport/{passportId}/children")
  public ResponseEntity<List<PassportDto>> getWithChildren(
      @Parameter(description = "ID of the Passport", required = true, in = ParameterIn.PATH)
          @PathVariable
          String passportId) {
    return ResponseEntity.ok(passportService.getPassportChildren(passportId));
  }

  /** Fetch the immediate children of the specified passport. */
  @Operation(summary = "Get all passports with the given parent ID")
  @GetMapping("/api/passport/{passportId}/immediateChildren")
  public ResponseEntity<List<PassportDto>> getImmediateChildren(
      @Parameter(description = "ID of the Passport", required = true, in = ParameterIn.PATH)
          @PathVariable
          String passportId) {
    return ResponseEntity.ok(passportService.getImmediateChildren(passportId));
  }

  /** Fetch all the root passports. */
  @Operation(summary = "Get all the root passports available")
  @GetMapping("/api/passport/root")
  public ResponseEntity<List<PassportDto>> getRootPassports() {
    return ResponseEntity.ok(passportService.getRootPassports());
  }

  /** Update the properties for the given passport and property group. */
  @Operation(summary = "Updates the property in the datasheet")
  @PutMapping(
      value = "/api/passport/{passportId}/data",
      produces = {"application/json"},
      consumes = {"application/json"})
  public ResponseEntity<PassportDto> updateData(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Property values to be updated in the datasheet")
          @Valid
          @RequestBody
          UpdateDataRequestDto request,
      @Parameter(description = "ID of the Passport", required = true, in = ParameterIn.PATH)
          @PathVariable
          String passportId)
      throws InvalidInputException {

    return ResponseEntity.ok(passportService.updateData(passportId, request));
  }

  /** Fetch the list of passports corresponding to the specified platform and code. */
  @Operation(summary = "Get all passports with the given code")
  @GetMapping("/api/passport/byCode/{code}")
  public ResponseEntity<List<PassportDto>> listPassportsByCode(
      @Parameter(description = "Code", required = true, in = ParameterIn.PATH) @PathVariable
          String code) {
    return ResponseEntity.ok(passportService.listPassportsByCode(code));
  }

  /** Get the tree structure of the dictionary. */
  @GetMapping("/api/passport/platform/{platform}/dictionary/{dictionary}/treeStructure")
  public ResponseEntity<List<DataDictionaryTreeStructureDto>> getDictionaryTreeStructure(
      @Parameter(description = "Platform name", required = true, in = ParameterIn.PATH)
          @PathVariable
          String platform,
      @Parameter(description = "Dictionary name", required = true, in = ParameterIn.PATH)
          @PathVariable
          String dictionary)
      throws IOException, InvalidDataDictionaryException {
    return ResponseEntity.ok(
        passportService.getDictionaryTreeStructure(
            Platform.fromValue(platform), DataDictionary.fromValue(dictionary)));
  }
}
