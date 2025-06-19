package com.opencirc.api.passport.enums;

import java.util.Arrays;

/**
 * Enum for defining the type of a template.
 */
public enum TemplateType {

    /**
     * class.
     */
    CLASS("class"),

    /**
     * property.
     */
    PROPERTY("property");

    /**
     * type in string.
     */
    private final String value;

    /**
     * Constructor.
     *
     * @param type
     */
    TemplateType(String type) {
        this.value = type;
    }

    /**
     * Gets the type value.
     *
     * @return the string representation of the type
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the string representation of the enum.
     *
     * @return the type as string
     */
    @Override
    public String toString() {
        return value;
    }

    /**
     * Parses a string value to its corresponding enum.
     *
     * @param value the string value to convert
     * @return the corresponding type enum
     * @throws IllegalArgumentException
     */
    public static TemplateType fromValue(String value) {
        return Arrays.stream(TemplateType.values())
                .filter(type -> type.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid type: " + value));
    }
}
