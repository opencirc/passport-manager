package com.opencirc.api.passport.mapping;

import com.opencirc.api.passport.enums.DataDictionary;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
public class DictionaryMapping {

  private final Map<String, Map<String, String>> dictionaries = new HashMap<>();

  /** DictionaryMapping constructor. */
  public DictionaryMapping() throws IOException {
    loadDictionaryMappings();
  }

  /** Loads the field names from dictionary mapping property file. */
  private void loadDictionaryMappings() throws IOException {
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    Resource[] resources = resolver.getResources("classpath:DataDictionary_Mapping/*.properties");

    for (Resource resource : resources) {
      String filename = resource.getFilename();
      if (filename != null && filename.endsWith(".properties")) {
        DataDictionary dictionary =
            DataDictionary.fromValue(
                filename.substring(0, filename.length() - ".properties".length()));
        Properties properties = new Properties();
        try (InputStream is = resource.getInputStream()) {
          properties.load(is);
        }
        Map<String, String> mapping = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
          String value = properties.getProperty(key);
          mapping.put(key, value);
        }
        dictionaries.put(dictionary.getValue(), mapping);
      }
    }
  }

  /** Maps the fetched dictionary mapping to the corresponding OpenCirc field names. */
  public Map<String, Object> mapDataDictionaryFieldToOpenCirc(
      Map<String, Object> dataDictionaryMap, DataDictionary dictionary) {
    Map<String, Object> result = new HashMap<>();
    Map<String, String> dictionaryMappings = dictionaries.get(dictionary.getValue());
    if (dataDictionaryMap != null && dictionaryMappings != null) {
      for (Map.Entry<String, String> mappingEntry : dictionaryMappings.entrySet()) {
        String mappingKey = mappingEntry.getKey();
        String mappingValue = mappingEntry.getValue();
        String[] mappedValues = mappingValue.split(",");
        for (String mappedValue : mappedValues) {
          mappedValue = mappedValue.trim();

          if (dataDictionaryMap.containsKey(mappedValue)) {
            result.put(mappingKey, dataDictionaryMap.get(mappedValue));
            break;
          }
        }
      }
    }
    return result;
  }
}
