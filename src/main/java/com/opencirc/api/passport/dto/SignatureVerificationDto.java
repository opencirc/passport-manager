package com.opencirc.api.passport.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Result of re-verifying the signature of an audit log tag. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignatureVerificationDto {

  private String logId;

  private String apiKeyId;

  private String tag;

  private String email;

  private boolean valid;

  /** Why verification failed, when it did. */
  private String reason;
}
