package com.opencirc.api.passport.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Converts Map objects into JSON strings and vice versa. */
@Converter(autoApply = true)
@Component
public class JsonMapConverter implements AttributeConverter<Map<String, Object>, String> {

  private final ObjectMapper objectMapper;

  /** Constructor. */
  public JsonMapConverter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /** Converts a Map into its JSON string for storage in the database. */
  @Override
  public String convertToDatabaseColumn(Map<String, Object> attribute) {
    if (attribute == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(attribute);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Error serializing Map to JSON", e);
    }
  }

  /** Converts a JSON string retrieved from the database into a Map. */
  @Override
  public Map<String, Object> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isBlank()) {
      return null;
    }
    try {
      return objectMapper.readValue(dbData, new TypeReference<Map<String, Object>>() {});
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Error deserializing JSON to Map", e);
    }
  }
}
