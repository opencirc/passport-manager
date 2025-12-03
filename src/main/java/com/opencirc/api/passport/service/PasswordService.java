package com.opencirc.api.passport.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {

  /** Injected BCryptPasswordEncoder used for hashing and verification. */
  private final BCryptPasswordEncoder encoder;

  /** Constructor. */
  public PasswordService(BCryptPasswordEncoder encoder) {
    this.encoder = encoder;
  }

  /** Hashes the given password. */
  public String hashPassword(String rawPassword) {
    return encoder.encode(rawPassword);
  }

  /** Verifies the given password. */
  public boolean verifyPassword(String rawPassword, String hashedPassword) {
    return encoder.matches(rawPassword, hashedPassword);
  }
}
