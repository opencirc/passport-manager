package com.oc.api.passport.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.oc.api.passport.config.Properties;
import com.oc.api.passport.mapping.DictionaryMapping;

@Service
public class ExternalApiService {

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private CacheService cacheService;

	@Autowired
	private DictionaryMapping dictionaryMapping;

	@Autowired
	private Properties props;



	public JsonNode fetchClassList(String text, String ddLibrary) {

		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(props.getBsDDClassSearchTextURL())
				.queryParam("SearchText", text).queryParam("limit", 20);
		String url = uriBuilder.toUriString();
		ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);

		return response.getBody();

	}
	
	public List<Map<String, String>> fetchPropertyList(String text, String dataDictionaryName) {

		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(props.getBsDDTextSearchURL())
				.queryParam("SearchText", text).queryParam("TypeFilter", "Property").queryParam("limit", 20);
		String url = uriBuilder.toUriString();
		System.out.println(url);
		ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
		JsonNode responseBody = response.getBody();
		System.out.println(response);
		List<Map<String, String>> propertyList = new ArrayList<>();
		    
		if (responseBody != null && responseBody.has("properties") && responseBody.get("properties").isArray()) {
	        for (JsonNode node : responseBody.get("properties")) {
	                Map<String, String> propertyMap = new HashMap<>();
	                
	                
	                // Add the key and uri to the property map
	                propertyMap.put("name", node.path("name").asText());
	                propertyMap.put("uri", node.path("uri").asText());

	                 propertyList.add(propertyMap);


	        }
	    }

		return propertyList;

	}

	/**
	 * @param uri
	 * @return
	 */
	public ObjectNode getClassTemplate(String uri) {

		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(props.getBsDDClassDetailsURL())
				.queryParam("Uri", uri).queryParam("IncludeClassProperties", true);

		String url = uriBuilder.toUriString();

		ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
		String responseBody = response.getBody();

		JsonNode rootNode = null;
		ObjectNode rootObject = null;
		try {
			rootNode = objectMapper.readTree(responseBody);
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}

		if (rootNode.isObject()) {
			rootObject = (ObjectNode) rootNode;

			// Add new field at class level
			rootObject.put("Data Category", "");

			ArrayNode classProperties = (ArrayNode) rootObject.get("classProperties");
			for (JsonNode propertyNode : classProperties) {
				if (propertyNode.isObject()) {
					ObjectNode propertyObject = (ObjectNode) propertyNode;

					propertyObject.put("Actual Value", "");
					propertyObject.put("Data Category", "");
				}
			}

			String modifiedJsonString = null;
			try {
				modifiedJsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootObject);
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println(modifiedJsonString);
		}

		return rootObject;

	}

	public ObjectNode fetchPropertiesWithDetail(Map<String, String> properties, String dataDictionaryName) {

		ObjectNode template = objectMapper.createObjectNode();
		ArrayNode propertiesArray = objectMapper.createArrayNode();
		template.put("TemplateName", "");
		template.put("Data Category", "");

		for (Map.Entry<String, String> entry : properties.entrySet()) {

			String uri = entry.getValue();

			UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(props.getBsDDPropertiesWithDetailURL())
					.queryParam("uri", uri);
			String url = uriBuilder.toUriString();
			Map<String, Object> response = restTemplate.getForObject(url, Map.class);
			System.out.println(response.toString());
			formPropertyTemplate(propertiesArray, response, dataDictionaryName);
		}
		template.set("properties", propertiesArray);
		System.out.println(template.toPrettyString());
		return template;

	}

	/**
	 * @param propertiesArray
	 * @param responseBody
	 */
	private void formPropertyTemplate(ArrayNode propertiesArray, Map<String, Object> response, String dataDictionaryName) {
		Map<String, String> mappedPropTemplate = new HashMap<String, String>();

	//	mappedPropTemplate = dictionaryMapping.mapDDFieldtoOC(response, dataDictionaryName);
		ObjectNode propertyTemplateNode = objectMapper.valueToTree(mappedPropTemplate);
		propertiesArray.add(propertyTemplateNode);

	}

}
