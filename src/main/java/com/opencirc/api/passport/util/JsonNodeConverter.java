package com.opencirc.api.passport.util;

import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class JsonNodeConverter implements AttributeConverter<JsonNode, String> {

    /**
     * Injecting ObjectMapper bean.
     */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Converts as column to fit in table.
     *
     * @param attribute
     * @return text
     */
    @Override
    public String convertToDatabaseColumn(JsonNode attribute) {
        try {
            return attribute != null
                    ? objectMapper.writeValueAsString(attribute)
                    : null;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "Error converting JsonNode to String", e);
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
            return dbData != null ? objectMapper.readTree(dbData) : null;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "Error converting String to JsonNode", e);
        }
    }
}
