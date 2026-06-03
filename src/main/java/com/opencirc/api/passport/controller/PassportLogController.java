package com.opencirc.api.passport.controller;

import com.opencirc.api.passport.dao.PassportLogRepository;
import com.opencirc.api.passport.model.PassportLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller for Passport Logs. */
@RestController
@RequestMapping("/api/passport")
@RequiredArgsConstructor
@Tag(name = "Passport Logs", description = "Endpoints for retrieving passport audit logs")
public class PassportLogController {

  private final PassportLogRepository passportLogRepository;

  /**
   * Retrieves all logs for a specific passport.
   *
   * @param passportId the ID of the passport
   * @return a list of passport logs
   */
  @GetMapping("/{passportId}/logs")
  @Operation(
      summary = "Get logs for a passport",
      description = "Retrieves the audit trail for a specific passport")
  public ResponseEntity<List<PassportLog>> getLogsByPassportId(@PathVariable String passportId) {
    List<PassportLog> logs = passportLogRepository.findByPassportId(passportId);
    return ResponseEntity.ok(logs);
  }
}
