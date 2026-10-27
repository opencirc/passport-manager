package com.opencirc.api.passport.util;

import com.opencirc.api.passport.dto.CreatedByDto;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** Converts CreatedByDto objects into JSON strings and vice versa. */
@Converter(autoApply = true)
@Component
public class CreatedByDtoConverter implements AttributeConverter<CreatedByDto, String> {

  private final ObjectMapper objectMapper;

  /** Constructor. */
  public CreatedByDtoConverter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /** Converts a CreatedByDto object into its JSON string for storage in the database. */
  @Override
  public String convertToDatabaseColumn(CreatedByDto createdByDto) {
    if (createdByDto == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(createdByDto);
    } catch (JacksonException e) {
      throw new IllegalArgumentException("Error serializing" + " CreatedByDto to JSON", e);
    }
  }

  /** Converts a JSON string retrieved from the database into a CreatedByDto object. */
  @Override
  public CreatedByDto convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isBlank()) {
      return null;
    }
    try {
      return objectMapper.readValue(dbData, CreatedByDto.class);
    } catch (JacksonException e) {
      throw new IllegalArgumentException("Error deserializing" + " JSON to CreatedByDto", e);
    }
  }
}
