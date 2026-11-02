package com.opencirc.api.passport.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Converts a list of strings into a JSON array string and vice versa. */
@Converter
@Component
public class StringListJsonConverter implements AttributeConverter<List<String>, String> {

  private final ObjectMapper objectMapper;

  /** Constructor. */
  public StringListJsonConverter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /** Converts a list of strings into its JSON string for storage in the database. */
  @Override
  public String convertToDatabaseColumn(List<String> attribute) {
    if (attribute == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(attribute);
    } catch (JacksonException e) {
      throw new IllegalArgumentException("Error serializing List<String> to JSON", e);
    }
  }

  /** Converts a JSON string retrieved from the database into a list of strings. */
  @Override
  public List<String> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isBlank()) {
      return null;
    }
    try {
      return objectMapper.readValue(dbData, new TypeReference<List<String>>() {});
    } catch (JacksonException e) {
      throw new IllegalArgumentException("Error deserializing JSON to List<String>", e);
    }
  }
}
