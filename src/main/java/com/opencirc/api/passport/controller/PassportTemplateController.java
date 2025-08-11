package com.opencirc.api.passport.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.opencirc.api.passport.dto.PassportTemplateDto;
import com.opencirc.api.passport.service.PassportTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint for operations related to passport. */
@RestController
@Tag(name = "Passport template", description = "Operations related to Passport templates")
public class PassportTemplateController {

  /** Injecting PassportTemplateService class. */
  @Autowired private PassportTemplateService passportTemplateService;

  /**
   * Endpoint to create a template from the existing passport.
   *
   * @param passportId The ID of the passport
   * @param dryRun Whether to persist the extracted template or not
   * @param templateName The name to be given to the extracted template
   * @return the template
   */
  @Operation(summary = "Create a template from the existing passport")
  @PostMapping(
      value = "/api/passportTemplate/{passportId}",
      produces = {"application/json"})
  public ResponseEntity<PassportTemplateDto> createTemplateFromPassport(
      @Parameter(description = "ID of the passport", required = true) @PathVariable
          String passportId,
      @Parameter(
              description = "Set to true if extracted template does not need" + "to be persisted")
          @RequestParam
          boolean dryRun,
      @Parameter(description = "Provide name to extracted" + "template for future retrieval")
          @RequestBody
          String templateName)
      throws JsonProcessingException {
    return ResponseEntity.ok(
        passportTemplateService.createTemplateFromPassport(passportId, dryRun, templateName));
  }

  /**
   * Endpoint to retrieve passport template.
   *
   * @param passportTemplateId The ID of the passport template
   * @return the template
   */
  @Operation(summary = "Retrieves the requested passport template")
  @GetMapping("/api/passportTemplate/{passportTemplateId}")
  public ResponseEntity<PassportTemplateDto> getTemplate(
      @Parameter(description = "Passport template ID", required = true) @PathVariable
          String passportTemplateId)
      throws JsonProcessingException {
    return ResponseEntity.ok(passportTemplateService.getPassportTemplate(passportTemplateId));
  }

  /**
   * Endpoint to list all passport templates.
   *
   * @return the template
   */
  @Operation(summary = "Lists all the persisted passport templates")
  @GetMapping("/api/passportTemplate/all")
  public ResponseEntity<List<PassportTemplateDto>> getAllTemplates()
      throws JsonProcessingException {
    return ResponseEntity.ok(passportTemplateService.getAllPassportTemplates());
  }
}
