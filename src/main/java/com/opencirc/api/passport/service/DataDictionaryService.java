package com.opencirc.api.passport.service;

import java.util.List;
import java.util.Map;

import com.opencirc.api.passport.enums.DataDictionary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.opencirc.api.passport.adapter.DictionaryAdapter;
import com.opencirc.api.passport.adapter.DictionaryAdapterFactory;
import com.opencirc.api.passport.exception.JsonValidationException;

@Service
public class DataDictionaryService {

    /**
     * Injecting CacheService class.
     */
    @Autowired
    private CacheService cacheService;

    /**
     * Injecting DictionaryAdapterFactory class.
     */
    @Autowired
    private DictionaryAdapterFactory dictionaryAdapterFactory;

    /**
     * Search and retrieves the class based on the text.
     *
     * @param dictionary
     * @param text
     * @return class Details
     */
    public List<Map<String, String>> searchClassesByText(DataDictionary dictionary,
            String text) {
        DictionaryAdapter adapter = dictionaryAdapterFactory
                .getAdapter(dictionary);
        List<Map<String, String>> classMap = adapter.listClass(text);
      //  cacheService.storePropertiesInRedis(dictionary, classMap);
        return classMap;
    }

    /**
     * Search and retrieves the class along with the properties.
     *
     * @param dictionary
     * @param uri
     * @param withProperties
     * @return class with properties in json format
     * @throws JsonProcessingException
     */
    public JsonNode createClassTemplate(DataDictionary dictionary, String uri,
            boolean withProperties) throws JsonValidationException,
    JsonProcessingException {
        // Gets adapter instance
        DictionaryAdapter adapter = dictionaryAdapterFactory
                .getAdapter(dictionary);
        JsonNode classTemplate = adapter.createClassTemplate(uri, withProperties);
        return classTemplate;
    }

    /**
     * Retrieves the list of properties.
     *
     * @param dictionary
     * @param text
     * @return properties in json format
     */
    public List<Map<String, String>> listProperties(DataDictionary dictionary,
            String text) {
        List<Map<String, String>> properties = cacheService
                .searchProperties(dictionary, text);
        System.out.println(properties.toString());
        if (properties == null || properties.isEmpty()) {
            DictionaryAdapter adapter = dictionaryAdapterFactory
                    .getAdapter(dictionary);
            properties = adapter.listProperties(text);
            if (properties != null && !properties.isEmpty()) {
                cacheService.storePropertiesInRedis(dictionary, properties);
            }
        }
        return properties;
    }

    /**
     * Creates the template with the listed properties.
     *
     * @param dictionary
     * @param propertiesUriList
     * @return template with properties in json format
     */
    public JsonNode createTemplateWithProperties(DataDictionary dictionary,
            List<String> propertiesUriList) throws JsonValidationException {
        DictionaryAdapter adapter = dictionaryAdapterFactory
                .getAdapter(dictionary);
        return adapter.getPropertyTemplatewithDetails(propertiesUriList);

    }

    /**
     * Fetches the uri from the dictionary for the given property names.
     *
     * @param code
     * @param dictionary
     * @return URI of the property
     */
    private String fetchUriForProperty(String code, DataDictionary dictionary) {
        String uri = cacheService.getUriFomCode(dictionary, code);

        if (uri == null) {
            List<Map<String, String>> properties = listProperties(dictionary, code
            );
            for (Map<String, String> property : properties) {
                String propertyCode = property.get("code");
                if (code.equals(propertyCode)) {
                    uri = property.get("uri");
                    break;
                }
            }
        }
        return uri;
    }

    /**
     * Clears the cache.
     *
     */
    public void clearCache() {
        cacheService.clearCache();
    }

    /**
     * List the data from cache.
     * @return the data from cache
     */
    public Map<String, Object> lookCache() {
        return cacheService.lookCache();
    }
}
