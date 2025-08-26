package com.opencirc.api.passport.util;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.zip.CRC32;

public final class SecretGenerator {

    /**
     * Allowed character from which random text be generated.
     */
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "abcdefghijklmnopqrstuvwxyz"
            + "0123456789";

    /**
     * Random class instantiation.
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * private constructor to prevents instantiation.
     */
    private SecretGenerator() {

    }

    /**
     * Generates a random alphanumeric string.
     *
     * @param length
     * @return random string
     * @throws IllegalArgumentException if length <= 0
     */
    public static String generateRandomString(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException(
                    "Random string length must be greater than 0");
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    /**
     * Generates an API token with the format: {prefix}_{randomString}_{crc32}.
     *
     * @param prefix
     * @param length
     * @param secretKey secret key used in CRC32 calculation
     * @return generated API token
     * @throws IllegalArgumentException if inputs are invalid
     */
    public static String generateApiToken(String prefix, int length, String secretKey) {

        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("Prefix must not be null or blank");
        }
        if (length <= 0 ) {
            throw new IllegalArgumentException("Random string length must be greater than 0");
        }
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalArgumentException("Secret key must not be null or blank");
        }

        String randomString = generateRandomString(length);
        long crc32 = computeCrc32(randomString + secretKey);

        return String.format("%s_%s_%08x", prefix, randomString, crc32);
    }

    /**
     * Computes a CRC32 checksum for the given input string.
     *
     * @param input
     * @return CRC32 checksum as a long
     * @throws NullPointerException if input is null
     */
    private static long computeCrc32(String input) {
        Objects.requireNonNull(input, "Input must not be null");
        CRC32 crc = new CRC32();
        crc.update(input.getBytes(StandardCharsets.UTF_8));
        return crc.getValue();
    }
}
