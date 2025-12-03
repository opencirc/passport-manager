package com.opencirc.api.passport.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencirc.api.passport.enums.Platform;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/** Dictionary mapping class. */
@Component
public class DictionaryMapping {

  /** Added with dictionary mappings. */
  private final Map<String, Map<String, String>> dictionaries = new HashMap<>();

  /** Injecting ObjectMapper. */
  private final ObjectMapper objectMapper;

  /**
   * DictionaryMapping constructor.
   */
  public DictionaryMapping(ObjectMapper mapper) throws IOException {
    this.objectMapper = mapper;
    loadDictionaryMappings();
  }

  /** Stores the required Dictionary mappings. */
  private final Map<Platform, Map<String, String>> reverseCache = new HashMap<>();

  /** Loads the field names from a dictionary mapping property file. */
  private void loadDictionaryMappings() throws IOException {
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    Resource[] resources = resolver.getResources("classpath:DataDictionary_Mapping/*.properties");

    for (Resource resource : resources) {
      String filename = resource.getFilename();
      if (filename != null && filename.endsWith(".properties")) {
        String dictionaryName = filename.substring(0, filename.length() - ".properties".length());
        Properties properties = new Properties();
        try (InputStream is = resource.getInputStream()) {
          properties.load(is);
        }
        Map<String, String> mapping = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
          String value = properties.getProperty(key);
          mapping.put(key, value);
        }
        dictionaries.put(dictionaryName, mapping);
      }
    }
  }

  /**
   * Fetches the relevant dictionary mapping.
   */
  public Map<String, String> getDictionaryMapping(Platform dictionaryName) {
    return dictionaries.get(dictionaryName.getValue());
  }

  /**
   * Maps the keys of a template to their corresponding ISO standard keys.
   */
  public ObjectNode mapTemplateFieldsToStandards(JsonNode template, Platform dictionaryName) {
    ObjectNode resultNode = objectMapper.createObjectNode();
    Map<String, String> dictionaryMappings = getDictionaryMapping(dictionaryName);

    if (template != null && dictionaryMappings != null) {
      Map<String, String> reverseMapping = getReverseMapping(dictionaryName, dictionaryMappings);

      template
          .fields()
          .forEachRemaining(
              entry -> {
                String originalKey = entry.getKey();
                String mappedKey = reverseMapping.getOrDefault(originalKey, originalKey);
                resultNode.set(mappedKey, entry.getValue());
              });
    }

    return resultNode;
  }

  /**
   * Retrieves the mapping details for the specified data dictionary.
   */
  private Map<String, String> getReverseMapping(
      Platform dictionaryPlatform, Map<String, String> dictionaryMappings) {
    return reverseCache.computeIfAbsent(
        dictionaryPlatform,
        d -> {
          Map<String, String> map = new HashMap<>();
          for (Map.Entry<String, String> entry : dictionaryMappings.entrySet()) {
            String standardKey = entry.getKey();
            for (String alias : entry.getValue().split(",")) {
              map.put(alias.trim(), standardKey);
            }
          }
          return map;
        });
  }
}
