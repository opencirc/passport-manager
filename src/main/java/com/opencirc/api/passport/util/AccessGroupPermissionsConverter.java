package com.opencirc.api.passport.util;

import com.opencirc.api.passport.model.AccessGroupPermissions;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** Converts the access group permission profile into its JSON string and vice versa. */
@Converter(autoApply = true)
@Component
public class AccessGroupPermissionsConverter
    implements AttributeConverter<AccessGroupPermissions, String> {

  private final ObjectMapper objectMapper;

  /** Constructor. */
  public AccessGroupPermissionsConverter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /** Converts the permissions into their JSON string for storage in the database. */
  @Override
  public String convertToDatabaseColumn(AccessGroupPermissions permissions) {
    if (permissions == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(permissions);
    } catch (JacksonException e) {
      throw new IllegalArgumentException("Error serializing permissions to JSON", e);
    }
  }

  /** Converts a JSON string retrieved from the database into the permissions. */
  @Override
  public AccessGroupPermissions convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isBlank()) {
      return null;
    }
    try {
      return objectMapper.readValue(dbData, AccessGroupPermissions.class);
    } catch (JacksonException e) {
      throw new IllegalArgumentException("Error deserializing JSON to permissions", e);
    }
  }
}
