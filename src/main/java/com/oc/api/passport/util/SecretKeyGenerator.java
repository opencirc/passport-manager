package com.oc.api.passport.util;

import io.jsonwebtoken.security.Keys;
import java.util.Base64;
import java.security.Key;

public class SecretKeyGenerator {
    public static void main(String[] args) {
        Key key = Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256);
        String encodedKey = Base64.getEncoder().encodeToString(key.getEncoded());
        System.out.println("Generated Secret Key: " + encodedKey);
    }
}