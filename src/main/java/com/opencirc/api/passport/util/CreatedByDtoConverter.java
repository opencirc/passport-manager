package com.opencirc.api.passport.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencirc.api.passport.dto.CreatedByDto;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

@Converter(autoApply = true)
@Component
public class CreatedByDtoConverter implements AttributeConverter<CreatedByDto, String> {

  /** Injecting ObjectMapper bean. */
  private final ObjectMapper objectMapper;

  /**
   * Constructor.
   *
   * @param objectMapper
   */
  public CreatedByDtoConverter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Converts a CreatedByDto object into its JSON string for storage in the database.
   *
   * @param createdByDto the {@link CreatedByDto} object to convert; may be {@code null}
   * @return the JSON string representation of the object, or null if the attribute is null
   * @throws IllegalArgumentException if serialization fails
   */
  @Override
  public String convertToDatabaseColumn(CreatedByDto createdByDto) {
    if (createdByDto == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(createdByDto);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Error serializing" + " CreatedByDto to JSON", e);
    }
  }

  /**
   * Converts a JSON string retrieved from the database into a CreatedByDto object.
   *
   * @param dbData the JSON string stored in the database;
   * @return the deserialized {@link CreatedByDto} object, or {@code null} if the input is {@code
   *     null} or empty
   * @throws IllegalArgumentException if deserialization fails
   */
  @Override
  public CreatedByDto convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isBlank()) {
      return null;
    }
    try {
      return objectMapper.readValue(dbData, CreatedByDto.class);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Error deserializing" + " JSON to CreatedByDto", e);
    }
  }
}
