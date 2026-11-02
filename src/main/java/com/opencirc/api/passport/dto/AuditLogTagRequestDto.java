package com.opencirc.api.passport.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Payload for attaching or transitioning the validation tag of an audit log entry. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogTagRequestDto {

  @Schema(allowableValues = {"PENDING", "APPROVED", "REJECTED"})
  @NotBlank
  private String tag;

  /** Email of the human engineer, certifier or auditor validating the state. */
  @NotBlank @Email private String email;
}
