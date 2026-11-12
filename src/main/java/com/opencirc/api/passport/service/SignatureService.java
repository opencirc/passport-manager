package com.opencirc.api.passport.service;

import com.opencirc.api.passport.constants.AppConstants;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

/**
 * Signs and verifies audit log validation tags. The canonical payload is {@code
 * log_id:target_id:email:tag:sha256(diff)} and the signature is its hex HMAC-SHA256 keyed by the
 * raw secret of the signing API key. Any change to the diff, the target, the validator or the state
 * invalidates the signature.
 */
@Service
public class SignatureService {

  private static final String HMAC_ALGORITHM = "HmacSHA256";

  private final JsonDiffService jsonDiffService;

  /** Constructor. */
  public SignatureService(JsonDiffService jsonDiffService) {
    this.jsonDiffService = jsonDiffService;
  }

  /** The deterministic composite string that is signed. */
  public String canonicalPayload(
      String logId, String targetId, String email, String tag, JsonNode diff) {
    return logId + ":" + targetId + ":" + email + ":" + tag + ":" + sha256Hex(diffJson(diff));
  }

  /** Signs the canonical payload with the API key secret. */
  public String sign(
      String logId, String targetId, String email, String tag, JsonNode diff, String secret) {
    return hmacHex(canonicalPayload(logId, targetId, email, tag, diff), secret);
  }

  /** Whether the stored signature matches a fresh signature over the same payload. */
  public boolean verify(
      String signature,
      String logId,
      String targetId,
      String email,
      String tag,
      JsonNode diff,
      String secret) {
    if (signature == null) {
      return false;
    }
    String expected = sign(logId, targetId, email, tag, diff, secret);
    return MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
  }

  /** Canonical JSON of the diff, independent of member order and whitespace. */
  public String diffJson(JsonNode diff) {
    return jsonDiffService.canonicalize(diff);
  }

  /** Hex SHA-256 of a string. */
  public static String sha256Hex(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance(AppConstants.SHA_256);
      return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }

  private static String hmacHex(String payload, String secret) {
    if (secret == null || secret.isEmpty()) {
      throw new IllegalArgumentException("A signing secret is required");
    }
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
      return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException("HMAC-SHA256 is not available", e);
    }
  }
}
