package com.opencirc.api.passport.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencirc.api.passport.enums.TemplateType;
import com.opencirc.api.passport.exception.JsonValidationException;
import java.util.List;
import java.util.Map;

/**
 * Interface of Dictionary Adapter.
 */
public interface DictionaryAdapter<T> {

  /**
   * Retrieves a list of classes matching the given search text.
   */
  List<Map<String, String>> listClass(String text);

  /**
   * Fetches the class template along with its property details based on the given URI.
   */
  T createClassTemplate(String code, boolean addProperties)
      throws JsonValidationException, JsonProcessingException;

  /**
   * Retrieves a list of properties matching the given search text.
   */
  List<Map<String, String>> listProperties(String text);

  /**
   * Retrieves property details for a given list of property URIs.
   */
  ObjectNode getPropertyTemplateWithDetails(List<String> properties) throws JsonValidationException;

  /**
   * Validates the given JSON template.
   */
  void validatePassportData(JsonNode jsonNode) throws JsonValidationException;

  /**
   * Validates the given URI.
   */
  boolean validateUri(String uri);

  /**
   * Displays the template from the dictionary without any processing.
   */
  JsonNode fetchRawTemplate(String uri, TemplateType type) throws JsonProcessingException;
}
