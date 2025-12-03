package com.opencirc.api.passport.util;

import jakarta.persistence.AttributeConverter;

public abstract class GenericEnumConverter<E extends Enum<E>>
    implements AttributeConverter<E, String> {

  private final Class<E> enumClass;

  /** Creates a new converter for the specified enum class. */
  protected GenericEnumConverter(Class<E> enumClass) {
    this.enumClass = enumClass;
  }

  /** Converts an enum to its database value by calling {@code getValue()} on the enum. */
  @Override
  public String convertToDatabaseColumn(E attribute) {
    if (attribute == null) {
      return null;
    }
    try {
      return (String) enumClass.getMethod("getValue").invoke(attribute);
    } catch (Exception e) {
      throw new IllegalStateException("Enum must have getValue() method", e);
    }
  }

  /**
   * Converts a database value back into an enum instance by calling {@code fromValue(String)} on
   * the enum.
   */
  @Override
  public E convertToEntityAttribute(String enumValue) {
    if (enumValue == null) {
      return null;
    }
    try {
      return (E) enumClass.getMethod("fromValue", String.class).invoke(null, enumValue);
    } catch (Exception e) {
      throw new IllegalStateException("Enum must have static fromValue(String) method", e);
    }
  }
}
