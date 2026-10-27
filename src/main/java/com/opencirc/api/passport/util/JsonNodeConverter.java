package com.opencirc.api.passport.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Component
@Converter(autoApply = true)
public class JsonNodeConverter implements AttributeConverter<JsonNode, String> {

  /** ObjectMapper bean. */
  private final ObjectMapper objectMapper;

  /** Initialising JsonNodeConverter. */
  public JsonNodeConverter(ObjectMapper mapper) {
    this.objectMapper = mapper;
  }

  /** Fallback ObjectMapper bean. */
  private static final ObjectMapper FALLBACK_MAPPER = JsonMapper.builder().build();

  /** Converts as column to fit in table. */
  @Override
  public String convertToDatabaseColumn(JsonNode attribute) {
    try {
      ObjectMapper mapper = (objectMapper != null ? objectMapper : FALLBACK_MAPPER);
      return attribute != null ? mapper.writeValueAsString(attribute) : null;
    } catch (JacksonException e) {
      throw new IllegalArgumentException("Error converting JsonNode to String", e);
    }
  }

  /** Converts to json node. */
  @Override
  public JsonNode convertToEntityAttribute(String dbData) {
    try {
      ObjectMapper mapper = (objectMapper != null ? objectMapper : FALLBACK_MAPPER);
      return dbData != null ? mapper.readTree(dbData) : null;
    } catch (JacksonException e) {
      throw new IllegalArgumentException("Error converting String to JsonNode", e);
    }
  }
}
