package com.oc.api.passport.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.oc.api.passport.adapter.DictionaryAdapter;
import com.oc.api.passport.adapter.DictionaryAdapterFactory;
import com.oc.api.passport.exception.BsDDJsonValidationException;

@Service
public class TemplateService {

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
     * @param text
     * @param ddLibrary
     * @return class Details
     */
    public List<Map<String, String>> searchClassesByText(String text,
            String ddLibrary) {
        DictionaryAdapter adapter = dictionaryAdapterFactory
                .getAdapter(ddLibrary);
        List<Map<String, String>> classMap = adapter.listClass(text);
      //  cacheService.storePropertiesInRedis(ddLibrary, classMap);
        return classMap;
    }

    /**
     * Search and retrieves the class along with the properties.
     *
     * @param uri
     * @param ddLibrary
     * @return class with properties in json format
     * @throws JsonProcessingException 
     */
    public JsonNode getClassTemplatewithPropDetails(String uri,
            String ddLibrary) throws BsDDJsonValidationException, JsonProcessingException {
        // Gets adapter instance
        DictionaryAdapter adapter = dictionaryAdapterFactory
                .getAdapter(ddLibrary);
        JsonNode classTemplate = adapter.createClassTemplate(uri, true);
        return classTemplate;
    }

    /**
     * Search and retrieves the class without any of the properties.
     *
     * @param uri
     * @param ddLibrary
     * @return class with properties in json format
     * @throws JsonProcessingException 
     */
    public JsonNode getClassTemplatewithoutPropDetails(String uri,
            String ddLibrary) throws BsDDJsonValidationException, JsonProcessingException {
        DictionaryAdapter adapter = dictionaryAdapterFactory
                .getAdapter(ddLibrary);
        JsonNode classTemplate = adapter.createClassTemplate(uri, false);
        return classTemplate;
    }
    /**
     * Retrieves the list of properties.
     *
     * @param text
     * @param ddLibrary
     * @return properties in json format
     */
    public List<Map<String, String>> listProperties(String text,
            String ddLibrary) {
        List<Map<String, String>> properties = cacheService
                .searchProperties(text, ddLibrary);
        System.out.println(properties.toString());
        if (properties == null || properties.isEmpty()) {
            DictionaryAdapter adapter = dictionaryAdapterFactory
                    .getAdapter(ddLibrary);
            properties = adapter.listProperties(text);
            if (properties != null && !properties.isEmpty()) {
                cacheService.storePropertiesInRedis(ddLibrary, properties);
            }
        }
        return properties;
    }

    /**
     * Creates the template with the listed properties.
     *
     * @param propertiesUriList
     * @param ddLibrary
     * @return template with properties in json format
     */
    public JsonNode createTemplateWithProperties(List<String> propertiesUriList,
            String ddLibrary) throws BsDDJsonValidationException {
        DictionaryAdapter adapter = dictionaryAdapterFactory
                .getAdapter(ddLibrary);
        return adapter.getPropertyTemplatewithDetails(propertiesUriList);

    }

    /**
     * Fetches the uri from the dictionary for the given property names.
     *
     * @param code
     * @param ddLibrary
     * @return URI of the property
     */
    private String fetchUriForProperty(String code, String ddLibrary) {
        String uri = cacheService.getURIfromCode(code, ddLibrary);

        if (uri == null) {
            List<Map<String, String>> properties = listProperties(code,
                    ddLibrary);
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
