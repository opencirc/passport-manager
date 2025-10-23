package com.opencirc.api.passport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencirc.api.passport.adapter.DictionaryAdapter;
import com.opencirc.api.passport.adapter.DictionaryAdapterFactory;
import com.opencirc.api.passport.enums.DataDictionaryPlatform;
import com.opencirc.api.passport.exception.JsonValidationException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataDictionaryService {

  /** Injecting DictionaryAdapterFactory class. */
  @Autowired private DictionaryAdapterFactory dictionaryAdapterFactory;

  /**
   * Search and retrieves the class based on the text.
   *
   * @param dictionaryPlatform
   * @param text
   * @return class Details
   */
  public List<Map<String, String>> searchClassesByText(
      DataDictionaryPlatform dictionaryPlatform, String text) {
    DictionaryAdapter<?> adapter = dictionaryAdapterFactory.getAdapter(dictionaryPlatform);
    List<Map<String, String>> classMap = adapter.listClass(text);
    return classMap;
  }

  /**
   * Search and retrieves the class along with the properties.
   *
   * @param dictionaryPlatform
   * @param uri
   * @param <T> The specific dictionary type
   * @param withProperties
   * @return class with properties in json format
   * @throws JsonProcessingException
   */
  public <T> T createClassTemplate(
      DataDictionaryPlatform dictionaryPlatform, String uri, boolean withProperties)
      throws JsonValidationException, JsonProcessingException {
    DictionaryAdapter<T> adapter = dictionaryAdapterFactory.getAdapter(dictionaryPlatform);
    return adapter.createClassTemplate(uri, withProperties);
  }

  /**
   * Retrieves the list of properties.
   *
   * @param dictionary
   * @param text
   * @return properties in json format
   */
  public List<Map<String, String>> listProperties(DataDictionaryPlatform dictionary, String text) {
    DictionaryAdapter<?> adapter = dictionaryAdapterFactory.getAdapter(dictionary);
    return adapter.listProperties(text);
  }

  /**
   * Creates the template with the listed properties.
   *
   * @param dictionaryPlatform
   * @param propertiesUriList
   * @return template with properties in json format
   */
  public ObjectNode createTemplateWithProperties(
      DataDictionaryPlatform dictionaryPlatform, List<String> propertiesUriList)
      throws JsonValidationException {
    DictionaryAdapter<?> adapter = dictionaryAdapterFactory.getAdapter(dictionaryPlatform);
    return adapter.getPropertyTemplateWithDetails(propertiesUriList);
  }
}
