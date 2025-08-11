package com.opencirc.api.passport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencirc.api.passport.config.AppProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ExternalApiService {

  /** Injecting RestTemplate class. */
  @Autowired private RestTemplate restTemplate;

  /** Injecting ObjectMapper class. */
  @Autowired private ObjectMapper objectMapper;

  /** Injecting Properties class. */
  @Autowired private AppProperties props;

  /**
   * Retrieves the class.
   *
   * @param text
   * @param ddLibrary
   * @return classlist in json format
   */
  public JsonNode fetchClassList(String text, String ddLibrary) {

    UriComponentsBuilder uriBuilder =
        UriComponentsBuilder.fromHttpUrl(props.getBsddClassSearchTextUrl())
            .queryParam("SearchText", text)
            .queryParam("limit", 20);
    String url = uriBuilder.toUriString();
    ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);

    return response.getBody();
  }

  /**
   * Retrieves the properties.
   *
   * @param text
   * @param dataDictionaryName
   * @return properties in json format
   */
  public List<Map<String, String>> fetchPropertyList(String text, String dataDictionaryName) {

    UriComponentsBuilder uriBuilder =
        UriComponentsBuilder.fromHttpUrl(props.getBsddTextSearchUrl())
            .queryParam("SearchText", text)
            .queryParam("TypeFilter", "Property")
            .queryParam("limit", 20);
    String url = uriBuilder.toUriString();
    ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
    JsonNode responseBody = response.getBody();
    List<Map<String, String>> propertyList = new ArrayList<>();

    if (responseBody != null
        && responseBody.has("properties")
        && responseBody.get("properties").isArray()) {
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
   * Fetches the class template.
   *
   * @param uri
   * @return properties in json format
   */
  public ObjectNode getClassTemplate(String uri) {

    UriComponentsBuilder uriBuilder =
        UriComponentsBuilder.fromHttpUrl(props.getBsddClassDetailsUrl())
            .queryParam("Uri", uri)
            .queryParam("IncludeClassProperties", true);

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
      ArrayNode classProperties = (ArrayNode) rootObject.get("classProperties");
      for (JsonNode propertyNode : classProperties) {
        if (propertyNode.isObject()) {
          ObjectNode propertyObject = (ObjectNode) propertyNode;

          propertyObject.put("actualValue", "");
        }
      }

      try {
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootObject);
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }
    }

    return rootObject;
  }

  /**
   * Retrieves the properties with all the details.
   *
   * @param properties
   * @param dataDictionaryName
   * @return properties in json format
   */
  public ObjectNode fetchPropertiesWithDetail(
      Map<String, String> properties, String dataDictionaryName) {

    ObjectNode template = objectMapper.createObjectNode();
    ArrayNode propertiesArray = objectMapper.createArrayNode();
    for (Map.Entry<String, String> entry : properties.entrySet()) {

      String uri = entry.getValue();

      UriComponentsBuilder uriBuilder =
          UriComponentsBuilder.fromHttpUrl(props.getBsddPropertiesWithDetailUrl())
              .queryParam("uri", uri);
      String url = uriBuilder.toUriString();
      Map<String, Object> response = restTemplate.getForObject(url, Map.class);
      formPropertyTemplate(propertiesArray, response, dataDictionaryName);
    }
    template.set("properties", propertiesArray);
    return template;
  }

  /**
   * Formulate template for properties.
   *
   * @param propertiesArray
   * @param response
   * @param dataDictionaryName
   */
  private void formPropertyTemplate(
      ArrayNode propertiesArray, Map<String, Object> response, String dataDictionaryName) {
    Map<String, String> mappedPropTemplate = new HashMap<String, String>();
    ObjectNode propertyTemplateNode = objectMapper.valueToTree(mappedPropTemplate);
    propertiesArray.add(propertyTemplateNode);
  }
}
