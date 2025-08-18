package com.opencirc.api.passport.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {

    /**
     * Injected BCryptPasswordEncoder used for hashing and verification.
     */
    private final BCryptPasswordEncoder encoder;

    /**
     * Constructor.
     * @param encoder BCryptPasswordEncoder bean injected by Spring
     */
    public PasswordService(BCryptPasswordEncoder encoder) {
        this.encoder = encoder;
    }

    /**
     * Hashes the given password.
     *
     * @param rawPassword
     * @return hashedPassword
     */
    public String hashPassword(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    /**
     * Verifies the given password.
     *
     * @param rawPassword
     * @param hashedPassword
     * @return the status
     */
    public boolean verifyPassword(String rawPassword, String hashedPassword) {
        return encoder.matches(rawPassword, hashedPassword);
    }
}
