package com.opencirc.api.passport.service.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.opencirc.api.passport.service.JsonDiffService;
import com.opencirc.api.passport.service.SignatureService;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class TestSignatureService {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final SignatureService service = new SignatureService(new JsonDiffService(objectMapper));

  private final JsonNode diff =
      objectMapper.readTree(
          "[{\"op\":\"replace\",\"path\":\"/data/fireRating\",\"value\":\"EI60\"}]");

  @Test
  void canonicalPayloadFollowsTheSpecifiedFormat() {
    String payload = service.canonicalPayload("log-1", "target-1", "a@b.org", "approved", diff);

    String expectedHash = SignatureService.sha256Hex(service.diffJson(diff));
    assertEquals("log-1:target-1:a@b.org:approved:" + expectedHash, payload);
  }

  @Test
  void signaturesAreDeterministicAndVerify() {
    String signature = service.sign("log-1", "target-1", "a@b.org", "approved", diff, "s3cret");

    assertEquals(64, signature.length());
    assertEquals(
        signature, service.sign("log-1", "target-1", "a@b.org", "approved", diff, "s3cret"));
    assertTrue(
        service.verify(signature, "log-1", "target-1", "a@b.org", "approved", diff, "s3cret"));
  }

  @Test
  void anyTamperingInvalidatesTheSignature() {
    String signature = service.sign("log-1", "target-1", "a@b.org", "approved", diff, "s3cret");
    JsonNode tamperedDiff =
        objectMapper.readTree(
            "[{\"op\":\"replace\",\"path\":\"/data/fireRating\",\"value\":\"EI30\"}]");

    assertFalse(
        service.verify(
            signature, "log-1", "target-1", "a@b.org", "approved", tamperedDiff, "s3cret"));
    assertFalse(
        service.verify(signature, "log-1", "target-1", "x@b.org", "approved", diff, "s3cret"));
    assertFalse(
        service.verify(signature, "log-1", "target-1", "a@b.org", "rejected", diff, "s3cret"));
    assertFalse(
        service.verify(signature, "log-1", "target-2", "a@b.org", "approved", diff, "s3cret"));
    assertFalse(
        service.verify(signature, "log-1", "target-1", "a@b.org", "approved", diff, "other"));
    assertFalse(service.verify(null, "log-1", "target-1", "a@b.org", "approved", diff, "s3cret"));
  }

  @Test
  void diffHashIgnoresMemberOrder() {
    JsonNode reordered =
        objectMapper.readTree(
            "[{\"value\":\"EI60\",\"path\":\"/data/fireRating\",\"op\":\"replace\"}]");
    assertEquals(
        service.sign("log-1", "target-1", "a@b.org", "approved", diff, "s3cret"),
        service.sign("log-1", "target-1", "a@b.org", "approved", reordered, "s3cret"));
    assertNotEquals(
        service.sign("log-1", "target-1", "a@b.org", "approved", diff, "s3cret"),
        service.sign("log-2", "target-1", "a@b.org", "approved", diff, "s3cret"));
  }

  @Test
  void refusesToSignWithoutSecret() {
    assertThrows(
        IllegalArgumentException.class,
        () -> service.sign("log-1", "target-1", "a@b.org", "approved", diff, ""));
  }
}
