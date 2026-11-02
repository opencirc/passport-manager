package com.opencirc.api.passport.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Computes RFC 6902 JSON Patch arrays between two JSON documents and produces a canonical
 * serialisation for hashing. Objects are diffed per member, arrays of equal length element by
 * element, everything else is replaced as a whole. Applying the patch to the previous state yields
 * the new state.
 */
@Service
public class JsonDiffService {

  private final ObjectMapper objectMapper;

  /** Constructor. */
  public JsonDiffService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /** The RFC 6902 operations turning {@code before} into {@code after}. */
  public ArrayNode diff(JsonNode before, JsonNode after) {
    ArrayNode operations = objectMapper.createArrayNode();
    diffInto(operations, "", normalise(before), normalise(after));
    return operations;
  }

  /** Compact JSON with object members sorted by name, stable across storage round trips. */
  public String canonicalize(JsonNode node) {
    return objectMapper.writeValueAsString(sorted(normalise(node)));
  }

  private void diffInto(ArrayNode operations, String path, JsonNode before, JsonNode after) {
    if (isAbsent(before) && isAbsent(after)) {
      return;
    }
    if (isAbsent(before)) {
      operations.add(operation("add", path).set("value", after));
      return;
    }
    if (isAbsent(after)) {
      operations.add(operation("remove", path));
      return;
    }
    if (Objects.equals(before, after)) {
      return;
    }
    if (before.isObject() && after.isObject()) {
      diffObjects(operations, path, (ObjectNode) before, (ObjectNode) after);
      return;
    }
    if (before.isArray() && after.isArray() && before.size() == after.size()) {
      for (int index = 0; index < before.size(); index++) {
        diffInto(operations, path + "/" + index, before.get(index), after.get(index));
      }
      return;
    }
    operations.add(operation("replace", path).set("value", after));
  }

  private void diffObjects(ArrayNode operations, String path, ObjectNode before, ObjectNode after) {
    List<String> beforeNames = sortedNames(before);
    for (String name : beforeNames) {
      if (!after.has(name)) {
        operations.add(operation("remove", path + "/" + escape(name)));
      }
    }
    for (String name : sortedNames(after)) {
      String memberPath = path + "/" + escape(name);
      if (!before.has(name)) {
        operations.add(operation("add", memberPath).set("value", after.get(name)));
      } else {
        diffInto(operations, memberPath, before.get(name), after.get(name));
      }
    }
  }

  private ObjectNode operation(String op, String path) {
    ObjectNode operation = objectMapper.createObjectNode();
    operation.put("op", op);
    operation.put("path", path);
    return operation;
  }

  private JsonNode sorted(JsonNode node) {
    if (node.isObject()) {
      Map<String, JsonNode> members = new TreeMap<>();
      Iterator<Map.Entry<String, JsonNode>> fields = node.properties().iterator();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> field = fields.next();
        members.put(field.getKey(), sorted(field.getValue()));
      }
      ObjectNode result = objectMapper.createObjectNode();
      members.forEach(result::set);
      return result;
    }
    if (node.isArray()) {
      ArrayNode result = objectMapper.createArrayNode();
      for (JsonNode element : node) {
        result.add(sorted(element));
      }
      return result;
    }
    return node;
  }

  private static List<String> sortedNames(ObjectNode node) {
    List<String> names = new ArrayList<>();
    node.properties().forEach(entry -> names.add(entry.getKey()));
    names.sort(null);
    return names;
  }

  private JsonNode normalise(JsonNode node) {
    return node == null ? objectMapper.nullNode() : node;
  }

  private static boolean isAbsent(JsonNode node) {
    return node == null || node.isNull() || node.isMissingNode();
  }

  /** Escapes a member name for use in a JSON Pointer (RFC 6901). */
  static String escape(String name) {
    return name.replace("~", "~0").replace("/", "~1");
  }
}
