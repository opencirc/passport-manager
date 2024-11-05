package com.oc.api.passport.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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

	public List<Map<String, String>> searchClassesByText(String text, String ddLibrary) {
		DictionaryAdapter adapter = dictionaryAdapterFactory.getAdapter(ddLibrary);
		List<Map<String, String>> classMap = adapter.listClass(text);
		cacheService.storePropertiesInRedis(ddLibrary, classMap);
		return classMap;
	}

	public JsonNode getClassTemplatewithPropDetails(String code, String ddLibrary) {
		DictionaryAdapter adapter = dictionaryAdapterFactory.getAdapter(ddLibrary);
		String uri = cacheService.getURIfromCode(code, ddLibrary);
		System.out.println(uri);
		if(uri == null) {
			List<Map<String, String>> classMap = searchClassesByText(code, ddLibrary);
			uri = cacheService.getURIfromCode(code, ddLibrary);
			System.out.println("2nd" +uri);
			
		}
		JsonNode classTemplate = adapter.getClassTemplatewithPropDetails(uri);
		System.out.println(classTemplate);
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

	public JsonNode createTemplateWithProperties(List<String> propertyCodes, String ddLibrary) {
		DictionaryAdapter adapter = dictionaryAdapterFactory.getAdapter(ddLibrary);
		
		/*
		 * List<String> uriList = new ArrayList<String>(); List<Map<String, String>>
		 * properties= null; for (String code : propertyCodes) { String uri =
		 * cacheService.getURIfromCode(code, ddLibrary); if(uri!=null) {
		 * uriList.add(uri); } else { properties = listProperties(code, ddLibrary); }
		 * 
		 * 
		 * 
		 * }
		 * 
		 * JsonNode template = adapter.getPropertyTemplatewithDetails(uriList); return
		 * template;
		 */
	    
	    List<String> uriList = propertyCodes.stream()
	        .map(code -> fetchUriForProperty(code, ddLibrary))
	        .filter(Objects::nonNull)
	        .collect(Collectors.toList());
	    
	    return adapter.getPropertyTemplatewithDetails(uriList);
	    
	}
	
	private String fetchUriForProperty(String code, String ddLibrary) {
		String uri = cacheService.getURIfromCode(code, ddLibrary);

		if (uri == null) {
			List<Map<String, String>> properties = listProperties(code, ddLibrary);
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


	public void clearCache() {
		cacheService.clearCache();
	}
	
	public Map<String, Object> lookCache() {
		return cacheService.lookCache();
	}
}
