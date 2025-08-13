package com.opencirc.api.passport.mapping;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencirc.api.passport.enums.DataDictionary;

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
     * Injecting ObjectMapper.
     */
    private final ObjectMapper objectMapper;

    /**
     * DictionaryMapping constructor.
     * @param mapper
     */
    public DictionaryMapping(ObjectMapper mapper) throws IOException {
        this.objectMapper = mapper;
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
     *
     * @param dictionaryName
     * @return the dictionary mappings for the requested dictionary
     */
    public Map<String, String> getDictionaryMapping(DataDictionary dictionaryName) {
        return dictionaries.get(dictionaryName.getValue());
    }

    /**
     * Maps the keys of a template to their corresponding ISO standard keys.
     *
     * @param template
     * @param dictionaryName
     * @return an ObjectNode containing the mapped template
     */
    public ObjectNode mapTemplateFieldsToStandards(JsonNode template,
            DataDictionary dictionaryName) {
        ObjectNode resultNode = objectMapper.createObjectNode();
        Map<String, String> dictionaryMappings = getDictionaryMapping(dictionaryName);

        if (template != null && dictionaryMappings != null) {
            template.fields().forEachRemaining(entry -> {
                String originalKey = entry.getKey();
                String mappedKey = originalKey;

                for (Map.Entry<String, String> mappingEntry : dictionaryMappings
                        .entrySet()) {
                    String standardKey = mappingEntry.getKey();
                    String[] possibleKeys = mappingEntry.getValue().split(",");
                    for (String key : possibleKeys) {
                        if (key.trim().equals(originalKey)) {
                            mappedKey = standardKey;
                            break;
                        }
                    }
                    if (!mappedKey.equals(originalKey)) {
                        break;
                    }
                }

                resultNode.set(mappedKey, entry.getValue());
            });
        }

        return resultNode;
    }

}
