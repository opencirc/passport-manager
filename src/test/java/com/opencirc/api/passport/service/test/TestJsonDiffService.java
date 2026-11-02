package com.opencirc.api.passport.service.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.opencirc.api.passport.service.JsonDiffService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;

public class TestJsonDiffService {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final JsonDiffService service = new JsonDiffService(objectMapper);

  private JsonNode json(String text) {
    return objectMapper.readTree(text);
  }

  private static List<String> ops(ArrayNode diff) {
    List<String> result = new ArrayList<>();
    for (JsonNode op : diff) {
      result.add(
          op.get("op").asText()
              + " "
              + op.get("path").asText()
              + (op.has("value") ? " " + op.get("value") : ""));
    }
    return result;
  }

  @Test
  void emitsReplaceAddAndRemoveForObjectMembers() {
    ArrayNode diff =
        service.diff(
            json("{\"data\":{\"thickness\":100,\"colour\":\"red\"}}"),
            json("{\"data\":{\"thickness\":150,\"fireRating\":\"EI30\"}}"));

    assertEquals(
        List.of(
            "remove /data/colour", "add /data/fireRating \"EI30\"", "replace /data/thickness 150"),
        ops(diff));
  }

  @Test
  void emitsNothingForIdenticalDocuments() {
    assertEquals(0, service.diff(json("{\"a\":[1,2]}"), json("{\"a\":[1,2]}")).size());
    assertEquals(0, service.diff(null, null).size());
  }

  @Test
  void treatsAbsentStatesAsWholeDocumentAddOrRemove() {
    assertEquals(List.of("add  {\"id\":\"p1\"}"), ops(service.diff(null, json("{\"id\":\"p1\"}"))));
    assertEquals(List.of("remove "), ops(service.diff(json("{\"id\":\"p1\"}"), null)));
  }

  @Test
  void diffsArraysElementWiseWhenLengthsMatchAndReplacesOtherwise() {
    assertEquals(
        List.of("replace /tags/1 \"b2\""),
        ops(service.diff(json("{\"tags\":[\"a\",\"b\"]}"), json("{\"tags\":[\"a\",\"b2\"]}"))));
    assertEquals(
        List.of("replace /tags [\"a\"]"),
        ops(service.diff(json("{\"tags\":[\"a\",\"b\"]}"), json("{\"tags\":[\"a\"]}"))));
  }

  @Test
  void escapesJsonPointerTokens() {
    assertEquals(List.of("add /a~1b~0c 1"), ops(service.diff(json("{}"), json("{\"a/b~c\":1}"))));
  }

  @Test
  void canonicalFormIsIndependentOfMemberOrderAndWhitespace() {
    String first = service.canonicalize(json("{ \"b\": 1, \"a\": {\"y\": true, \"x\": null} }"));
    String second = service.canonicalize(json("{\"a\":{\"x\":null,\"y\":true},\"b\":1}"));
    assertEquals(second, first);
    assertTrue(first.startsWith("{\"a\":{\"x\":null,\"y\":true},\"b\":1}"));
  }
}
