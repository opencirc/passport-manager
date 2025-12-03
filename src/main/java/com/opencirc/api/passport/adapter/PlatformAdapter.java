package com.opencirc.api.passport.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencirc.api.passport.dto.DataDictionaryTreeStructureDto;
import com.opencirc.api.passport.enums.DataDictionary;
import com.opencirc.api.passport.exception.InvalidDataDictionaryException;
import com.opencirc.api.passport.exception.JsonValidationException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Interface of Dictionary Adapter.
 *
 * @param <T> the type of dictionary used by the adapter
 */
public interface PlatformAdapter<T> {

  /** Retrieves a list of classes matching the given search text. */
  List<Map<String, String>> listClass(String text);

  /** Fetches the class template along with its property details based on the given URI. */
  T createClassTemplate(String code, boolean addProperties)
      throws JsonValidationException, JsonProcessingException;

  /** Retrieves a list of properties matching the given search text. */
  List<Map<String, String>> listProperties(String text);

  /** Retrieves property details for a given list of property URIs. */
  ObjectNode getPropertyTemplateWithDetails(List<String> properties) throws JsonValidationException;

  /** Validates the given JSON template. */
  String validatePassportData(JsonNode jsonNode) throws JsonValidationException;

  /** Validates the given URI. */
  boolean validateUri(String uri);

  /** Displays the template from the dictionary without any processing. */
  JsonNode fetchRawTemplate(String uri, String type) throws JsonProcessingException;

  /** Retrieves the tree structure of the dictionary. */
  List<DataDictionaryTreeStructureDto> getDictionaryTreeStructure(DataDictionary dictionary)
      throws IOException, InvalidDataDictionaryException;
}
