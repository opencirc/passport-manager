package com.opencirc.api.passport.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.opencirc.api.passport.dto.CreatePassportRequestDto;
import com.opencirc.api.passport.dto.PassportDto;
import com.opencirc.api.passport.dto.UpdateDataRequestDto;
import com.opencirc.api.passport.enums.DataDictionary;
import com.opencirc.api.passport.enums.DataDictionaryPlatform;
import com.opencirc.api.passport.exception.InvalidInputException;
import com.opencirc.api.passport.exception.JsonValidationException;
import com.opencirc.api.passport.service.PassportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint for operations related to passport. */
@RestController
@Tag(name = "Passport", description = "Operations related to Passport")
public class PassportController {

  /** Injecting PassportService class. */
  @Autowired private PassportService passportService;

  /**
   * Endpoint to create a passport.
   *
   * @param data
   * @param dictionaryName
   * @return the status
   * @throws JsonValidationException
   * @throws InvalidInputException
   */
  @Operation(summary = "Creates Passport and validates it")
  @PostMapping(
      value = "/api/passport/dictionary/{platform}/{dictionary}",
      produces = {"application/json"},
      consumes = {"application/json"})
  public ResponseEntity<PassportDto> createPassportUsingDictionary(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description =
                  "JSON template retrieved from external APIs, "
                      + "populated with actual data to create the Passport")
          @RequestBody
          CreatePassportRequestDto data,
      @Parameter(description = "Dictionary Platform", required = true, in = ParameterIn.PATH)
          @PathVariable("platform")
          String platform,
      @Parameter(description = "Dictionary", required = true, in = ParameterIn.PATH)
          @PathVariable("dictionary")
          String dictionaryName)
      throws InvalidInputException, JsonValidationException {
    return ResponseEntity.ok(
        passportService.createPassportUsingDictionary(
            DataDictionaryPlatform.fromValue(platform),
            DataDictionary.fromValue(dictionaryName),
            data));
  }

  /**
   * Endpoint to fetch the passport.
   *
   * @param passportId
   * @return the passport in json
   */
  @Operation(summary = "Retrieves the Passport")
  @GetMapping("/api/passport/{passportId}")
  public ResponseEntity<PassportDto> getPassport(
      @Parameter(description = "ID of the Passport", required = true, in = ParameterIn.PATH)
          @PathVariable
          String passportId)
      throws JsonProcessingException {
    return ResponseEntity.ok(passportService.getPassport(passportId));
  }

  /**
   * Endpoint to fetch the specified passport and its descendants.
   *
   * @param passportId
   * @return the passport as JSON
   * @throws JsonValidationException
   */
  @Operation(summary = "Get Passport and its children for the given ID")
  @GetMapping("/api/passport/{passportId}/children")
  public ResponseEntity<List<PassportDto>> getWithChildren(
      @Parameter(description = "ID of the Passport", required = true, in = ParameterIn.PATH)
          @PathVariable
          String passportId)
      throws JsonProcessingException, JsonValidationException {
    return ResponseEntity.ok(passportService.getPassportChildren(passportId));
  }

  /**
   * Endpoint to fetch the immediate children of the specified passport.
   *
   * @param passportId
   * @return the passport as JSON
   * @throws JsonValidationException
   */
  @Operation(summary = "Get all passports with the given parent ID")
  @GetMapping("/api/passport/{passportId}/immediateChildren")
  public ResponseEntity<List<PassportDto>> getImmediateChildren(
      @Parameter(description = "ID of the Passport", required = true, in = ParameterIn.PATH)
          @PathVariable
          String passportId)
      throws JsonProcessingException, JsonValidationException {
    return ResponseEntity.ok(passportService.getImmediateChildren(passportId));
  }

  /**
   * Endpoint to fetch all the root passports.
   *
   * @return the passports without parent
   * @throws JsonValidationException
   */
  @Operation(summary = "Get all the root passports available")
  @GetMapping("/api/passport/root")
  public ResponseEntity<List<PassportDto>> getRootPassports() {
    return ResponseEntity.ok(passportService.getRootPassports());
  }

  /**
   * Endpoint to update the properties for the given passport and property group.
   *
   * @return the updated passport
   * @throws JsonValidationException
   */
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
      throws InvalidInputException, JsonValidationException {

    return ResponseEntity.ok(passportService.updateData(passportId, request));
  }

  /**
   * Endpoint to fetch the list of passports corresponds to the input code.
   *
   * @param platform
   * @param code
   * @return the list of passports
   * @throws JsonValidationException
   */
  @Operation(summary = "Get all passports with the given code from the specified platform")
  @GetMapping("/api/passport/{platform}/{code}/all")
  public ResponseEntity<List<PassportDto>> listPassportsByCategory(
      @Parameter(description = "ID of the Passport", required = true, in = ParameterIn.PATH)
          @PathVariable
          String platform,
      @Parameter(description = "Code", required = true, in = ParameterIn.PATH) @PathVariable
          String code)
      throws JsonProcessingException, JsonValidationException {
    return ResponseEntity.ok(passportService.listPassportsByCode(platform, code));
  }
}
