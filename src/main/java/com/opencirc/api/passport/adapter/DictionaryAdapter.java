package com.opencirc.api.passport.adapter;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.opencirc.api.passport.exception.JsonValidationException;

/**
 * Interface of Dictionary Adapter.
 */
public interface DictionaryAdapter<T> {

    /**
     * Retrieves a list of classes matching the given search text.
     * @param text class name
     * @return list of classes
     */
    List<Map<String, String>> listClass(String text);

    /**
     * Fetches the class template along with its property details based on the given URI.
     * @param code
     * @param addProperties
     * @return JsonNode
     * @throws JsonValidationException
     */
    T createClassTemplate(String code, boolean addProperties)
            throws JsonValidationException, JsonProcessingException;


    /**
     * Retrieves a list of properties matching the given search text.
     * @param text - name of the property
     * @return list of properties
     */
    List<Map<String, String>> listProperties(String text);

    /**
     * Retrieves property details for a given list of property URIs.
     * @param properties
     * @return Jsonnode with proeprty details
     * @throws JsonValidationException
     */
    JsonNode getPropertyTemplatewithDetails(List<String> properties)
            throws JsonValidationException;

    /**
     * Validates the given JSON template.
     * @param jsonNode
     */
    void validatePassportData(JsonNode jsonNode)
            throws JsonValidationException;

    /**
     * Validates the given URI.
     * @param uri
     * @return true or false
     */
    boolean validateUri(String uri);

    /**
     * Displays the template from the dictionary without any processing.
     * @param uri
     * @param type
     * @return response
     * @throws JsonProcessingException
     */
    JsonNode fetchRawTemplate(String uri, String type)
            throws JsonProcessingException;
}
