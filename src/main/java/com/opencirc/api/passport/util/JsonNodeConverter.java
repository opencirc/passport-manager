package com.opencirc.api.passport.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Component
@Converter(autoApply = true)
public class JsonNodeConverter implements AttributeConverter<JsonNode, String> {

    /**
     * ObjectMapper bean.
     */
    private final ObjectMapper objectMapper;

    /**
     * Initialising JsonNodeConverter.
     * @param objectMapper
     */
    @Autowired
    public JsonNodeConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Fallback ObjectMapper bean.
     */
    private static final ObjectMapper FALLBACK_MAPPER = JsonMapper.builder().build();


    /**
     * Converts as column to fit in table.
     *
     * @param attribute
     * @return text
     */
    @Override
    public String convertToDatabaseColumn(JsonNode attribute) {
        try {
            ObjectMapper mapper = (objectMapper != null ? objectMapper : FALLBACK_MAPPER);
            return attribute != null ? mapper.writeValueAsString(attribute) : null;
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
            ObjectMapper mapper = (objectMapper != null ? objectMapper : FALLBACK_MAPPER);
            return dbData != null ? mapper.readTree(dbData) : null;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "Error converting String to JsonNode", e);
        }
    }
}
