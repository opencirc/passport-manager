package com.opencirc.api.passport.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class JsonNodeConverter implements AttributeConverter<JsonNode, String> {

  /** Injecting ObjectMapper class. */
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /**
   * Converts as column to fit in table.
   *
   * @param attribute
   * @return text
   */
  @Override
  public String convertToDatabaseColumn(JsonNode attribute) {
    try {
      return attribute != null ? OBJECT_MAPPER.writeValueAsString(attribute) : null;
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Error converting JsonNode to String", e);
    }
  }

  /**
   * Converts to json node.
   *
   * @param dbData
   * @return text in json format
   */
  @Override
  public JsonNode convertToEntityAttribute(String dbData) {
    try {
      return dbData != null ? OBJECT_MAPPER.readTree(dbData) : null;
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Error converting String to JsonNode", e);
    }
  }
}
