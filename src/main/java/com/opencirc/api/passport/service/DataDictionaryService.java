package com.opencirc.api.passport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencirc.api.passport.adapter.DictionaryAdapterFactory;
import com.opencirc.api.passport.adapter.PlatformAdapter;
import com.opencirc.api.passport.enums.Platform;
import com.opencirc.api.passport.exception.JsonValidationException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Data dictionary service class.
 */
@Service
public class DataDictionaryService {

  /** Injecting DictionaryAdapterFactory class. */
  @Autowired private DictionaryAdapterFactory dictionaryAdapterFactory;

  /**
   * Search and retrieves the class based on the text.
   */
  public List<Map<String, String>> searchClassesByText(Platform platform, String text) {
    PlatformAdapter<?> adapter = dictionaryAdapterFactory.getAdapter(platform);
    return adapter.listClass(text);
  }

  /**
   * Search and retrieves the class along with the properties.
   */
  public <T> T createClassTemplate(Platform platform, String uri, boolean withProperties)
      throws JsonValidationException, JsonProcessingException {
    PlatformAdapter<T> adapter = dictionaryAdapterFactory.getAdapter(platform);
    return adapter.createClassTemplate(uri, withProperties);
  }

  /**
   * Retrieves the list of properties.
   */
  public List<Map<String, String>> listProperties(Platform dictionary, String text) {
    PlatformAdapter<?> adapter = dictionaryAdapterFactory.getAdapter(dictionary);
    return adapter.listProperties(text);
  }

  /**
   * Creates the template with the listed properties.
   */
  public ObjectNode createTemplateWithProperties(
      Platform platform, List<String> propertiesUriList) throws JsonValidationException {
    PlatformAdapter<?> adapter = dictionaryAdapterFactory.getAdapter(platform);
    return adapter.getPropertyTemplateWithDetails(propertiesUriList);
  }
}
