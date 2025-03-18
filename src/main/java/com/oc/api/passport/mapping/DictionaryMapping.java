package com.oc.api.passport.mapping;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * Dictionary mapping class.
 */
@Component
public class DictionaryMapping {

    /**
     * Added with dictionary mappings.
     */
    private Map<String, Map<String, String>> dictionaries = new HashMap<>();

    /**
     * DictionaryMapping constructor.
     */
    public DictionaryMapping() throws IOException {
        loadDictionaryMappings();
    }

    /**
     * Loads the field names from dictionary mapping property file.
     */
    private void loadDictionaryMappings() throws IOException {
        PathMatchingResourcePatternResolver resolver = new
                PathMatchingResourcePatternResolver();
        Resource[] resources = resolver
                .getResources("classpath:DataDictionary_Mapping/*.properties");

        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (filename != null && filename.endsWith(".properties")) {
                String dictionaryName = filename.substring(0,
                        filename.length() - ".properties".length());
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
     * @param dictionaryName
     * @return the dictionary mappings for the requested dictionary
     */
    public Map<String, String> getDictionaryMapping(String dictionaryName) {
        return dictionaries.get(dictionaryName);
    }

    /**
     * Maps the fetched dictionary mapping.
     * @param ddResponse
     * @param dictionaryName
     * @return the dictionary mappings
     */
    public Map<String, Object> mapDDFieldtoOC(Map<String, Object> ddResponse,
            String dictionaryName) {

        Map<String, Object> result = new HashMap<>();
        Map<String, String> dictionaryMappings = getDictionaryMapping(dictionaryName);
        if (ddResponse != null && dictionaryMappings != null) {
            for (Map.Entry<String, String> mappingEntry : dictionaryMappings.entrySet()) {
                String mappingKey = mappingEntry.getKey();
                String mappingValue = mappingEntry.getValue();
                if (ddResponse.containsKey(mappingValue)) {
                    result.put(mappingKey, ddResponse.get(mappingValue));
                }
            }
        }
        return result;
    }
// remove unused methods

    /*
     * private String getMappedValue(String key, Map<String, String>
     * dictionaryMappings) { return dictionaryMappings.get(key); }
     *
     * private int parseInteger(Object value) { try { return value != null ?
     * Integer.parseInt(String.valueOf(value)) : 0; } catch (NumberFormatException
     * e) { return 0; } }
     *
     * private boolean parseBoolean(Object value) { return value != null &&
     * Boolean.parseBoolean(String.valueOf(value)); }
     *
     * private List<String> getMappedList(String key, Map<String, Object>
     * ddResponse) { if (key != null && ddResponse.containsKey(key)) { Object value
     * = ddResponse.get(key); if (value instanceof List) { return (List<String>)
     * value; } else if (value instanceof String) { return Arrays.asList(((String)
     * value).split(",")); } } return null; }
     */
}
