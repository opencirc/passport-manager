package com.opencirc.api.passport.util;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/** Util class for encryption and decryption. */
public class EncryptionUtil {

  /** Encryption algorithm. */
  private static final String ENCRYPTION_ALGORITHM = "AES";

  /** GeGenerates a secure key for JWT token signing. */
  public static String generateSecureKey() {
    try {
      byte[] keyBytes = new byte[32];
      new SecureRandom().nextBytes(keyBytes);
      return Base64.getEncoder().encodeToString(keyBytes);
    } catch (Exception e) {
      throw new RuntimeException("Error generating secure key", e);
    }
  }

  /** Derives and hashes the key with the help of encryption key. */
  public static SecretKeySpec deriveKey(String encryptionKey) throws Exception {
    byte[] keyBytes = encryptionKey.getBytes("UTF-8");
    MessageDigest sha = MessageDigest.getInstance("SHA-256");
    keyBytes = sha.digest(keyBytes);
    return new SecretKeySpec(keyBytes, ENCRYPTION_ALGORITHM);
  }

  /** Encrypts the data. */
  public static String encrypt(String data, String encryptionKey) throws Exception {
    SecretKeySpec secretKey = deriveKey(encryptionKey);
    Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
    cipher.init(Cipher.ENCRYPT_MODE, secretKey);
    byte[] encryptedBytes = cipher.doFinal(data.getBytes());
    return Base64.getEncoder().encodeToString(encryptedBytes);
  }

  /** Decrypts the data. */
  public static String decrypt(String encryptedData, String encryptionKey) throws Exception {
    SecretKeySpec secretKey = deriveKey(encryptionKey);
    Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
    cipher.init(Cipher.DECRYPT_MODE, secretKey);
    byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
    return new String(decryptedBytes);
  }
}
