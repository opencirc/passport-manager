package com.oc.api.passport.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.oc.api.passport.adapter.DictionaryAdapter;
import com.oc.api.passport.adapter.DictionaryAdapterFactory;

@Service
public class TemplateService {

	@Autowired
	private CacheService cacheService;
	
	@Autowired
	private DictionaryAdapterFactory dictionaryAdapterFactory;

	public JsonNode searchClassesByText(String text, String ddLibrary) {
		DictionaryAdapter adapter = dictionaryAdapterFactory.getAdapter(ddLibrary);
		return adapter.listClass(text);
	}

	public JsonNode getClassTemplatewithPropDetails(String uri, String ddLibrary) {
		DictionaryAdapter adapter = dictionaryAdapterFactory.getAdapter(ddLibrary);
		JsonNode classTemplate = adapter.getClassTemplatewithPropDetails(uri);
		return classTemplate;
	}

	public List<Map<String, String>> listProperties(String text, String ddLibrary) {
		List<Map<String, String>> properties = cacheService.searchProperties(text, ddLibrary);
		System.out.println(properties.toString());
		if (properties == null || properties.isEmpty()) {
			DictionaryAdapter adapter = dictionaryAdapterFactory.getAdapter(ddLibrary);
			properties = adapter.listProperties(text);
	        if (properties != null && !properties.isEmpty()) {
	            cacheService.storePropertiesInRedis(ddLibrary, properties);
	        }
		}
		return properties;
	}

	public JsonNode createTemplateWithProperties(Map<String, String> properties, String ddLibrary) {
		DictionaryAdapter adapter = dictionaryAdapterFactory.getAdapter(ddLibrary);
		JsonNode template = adapter.getPropertyTemplatewithDetails(properties);
		return template;
	}

}
