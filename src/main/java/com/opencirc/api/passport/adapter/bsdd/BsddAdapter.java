package com.opencirc.api.passport.adapter.bsdd;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencirc.api.passport.adapter.DictionaryAdapter;
import com.opencirc.api.passport.config.AppProperties;
import com.opencirc.api.passport.constants.AppConstants;
import com.opencirc.api.passport.dto.BsddClassTemplateDto;
import com.opencirc.api.passport.enums.DataDictionary;
import com.opencirc.api.passport.enums.TemplateType;
import com.opencirc.api.passport.exception.InvalidInputException;
import com.opencirc.api.passport.exception.JsonValidationException;
import com.opencirc.api.passport.mapping.DictionaryMapping;
import com.opencirc.api.passport.service.CacheService;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class BsddAdapter implements DictionaryAdapter<BsddClassTemplateDto> {
  private final RestTemplate restTemplate;

  private final AppProperties props;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private DictionaryMapping dictionaryMapping;

  @Autowired private CacheService cacheService;

  /**
   * Create an instance of the BsddAdapter.
   */
  @Autowired
  public BsddAdapter(RestTemplate injectedRestTemplate, AppProperties properties) {
    this.restTemplate = injectedRestTemplate;
    this.props = properties;
  }

  private static void validateDataType(
      String propName, String dataType, JsonNode actualValueNode, List<String> errorMessages) {
    String actualValue = actualValueNode.asText();

    switch (dataType) {
      case "Integer":
        try {
          Integer.parseInt(actualValue);
        } catch (NumberFormatException e) {
          errorMessages.add(
              propName + " : Invalid data type. Expected Integer, but got: " + actualValue);
        }
        break;
      case "Boolean":
        if (!"true".equalsIgnoreCase(actualValue) && !"false".equalsIgnoreCase(actualValue)) {
          errorMessages.add(
              propName + " : Invalid data type. Expected Boolean, but got: " + actualValue);
        }
        break;
      case "Real":
        try {
          Double.parseDouble(actualValue.replace("\"", ""));
          if (!actualValue.contains(".")) {
            errorMessages.add(
                propName
                    + " : Invalid Real number. A valid Real number should contain"
                    + " a decimal point.");
          }

        } catch (NumberFormatException e) {
          errorMessages.add(
              propName + " : Invalid data type. Expected Real (Double), but got: " + actualValue);
        }
        break;
      case "String":
        if (actualValue == null) {
          errorMessages.add(
              propName + " : Invalid data type. Expected String, but got: " + actualValue);
        }
        break;
      case "Character":
        if (actualValue.length() != 1) {
          errorMessages.add(
              propName
                  + " : Invalid data type. Expected Character "
                  + "(Single character string), but got: "
                  + actualValue);
        }
        break;
      case "Time":
        if (actualValue == null) {
          errorMessages.add(
              propName + " : Invalid data type. Expected Time (String), but got: " + actualValue);
        }
        break;
      default:
        errorMessages.add(propName + " : Unknown data type: " + dataType);
    }
  }

  private static void validateAllowedValues(
      String propName, ArrayNode allowedValues, JsonNode actualValue, List<String> errorMessages) {
    boolean isValid = false;

    for (JsonNode allowedValueNode : allowedValues) {
      String value = allowedValueNode.get("value").asText();

      if (value.equals(actualValue.asText())) {
        isValid = true;
        break;
      }
    }

    if (!isValid) {
      errorMessages.add(
          propName + " : Actual value: " + actualValue + " is not within the allowed values.");
    }
  }

  private static void validateRangeChecks(
      String propName,
      Object actualValue,
      Double maxExclusive,
      Double maxInclusive,
      Double minExclusive,
      Double minInclusive,
      List<String> errorMessages) {
    double realValue = 0.0;
    try {
      realValue = Double.parseDouble(actualValue.toString().trim().replace("\"", ""));
    } catch (NumberFormatException e) {
      errorMessages.add(
          propName + " : Invalid data type. Expected Real (Double), but got: " + actualValue);
    }

    if (maxExclusive != null && realValue >= maxExclusive) {
      errorMessages.add(
          propName
              + " : Actual value: "
              + realValue
              + " exceeds MaxExclusive limit: "
              + maxExclusive);
    }

    if (maxInclusive != null && realValue > maxInclusive) {
      errorMessages.add(
          propName
              + " : Actual value: "
              + realValue
              + " exceeds MaxInclusive limit: "
              + maxInclusive);
    }

    if (minExclusive != null && realValue <= minExclusive) {
      errorMessages.add(
          propName
              + " : Actual value: "
              + realValue
              + " is below MinExclusive limit: "
              + minExclusive);
    }

    if (minInclusive != null && realValue < minInclusive) {
      errorMessages.add(
          propName
              + " : Actual value: "
              + realValue
              + " is below MinInclusive limit: "
              + minInclusive);
    }
  }

  /**
   * Fetches a list of classes matching the search text.
   *
   * @param text The search text.
   * @return A list of maps containing class details.
   */
  @Override
  public List<Map<String, String>> listClass(String text) {

    if (props == null) {
      throw new IllegalStateException("Properties bean is not injected!");
    }
    UriComponentsBuilder uriBuilder =
        UriComponentsBuilder.fromHttpUrl(props.getBsddClassSearchTextUrl())
            .queryParam(AppConstants.QP_BSDD_SEARCHTEXT, text)
            .queryParam(AppConstants.QP_BSDD_LIMIT, 20);
    String url = uriBuilder.toUriString();
    ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
    JsonNode responseBody = response.getBody();

    List<Map<String, String>> classList = new ArrayList<>();
    if (responseBody != null && responseBody.path("totalCount").asInt() > 0) {
      for (JsonNode node : responseBody.get("classes")) {
        Map<String, String> classMap = new HashMap<>();
        classMap.put("name", node.path("name").asText());
        classMap.put("uri", node.path("uri").asText());
        classMap.put("code", node.path("referenceCode").asText());
        classList.add(classMap);
      }
    }
    return classList;
  }

  /**
   * Fetches a class template and optionally adds property details.
   */
  @Override
  public BsddClassTemplateDto createClassTemplate(String uri, boolean addProperties)
      throws JsonValidationException, JsonProcessingException {

    BsddClassTemplateDto classTemplateDto = getClassTemplate(uri, addProperties);

    if (addProperties
        && classTemplateDto.getClassProperties() != null
        && classTemplateDto.getClassProperties().isArray()) {

      ArrayNode classProperties = (ArrayNode) classTemplateDto.getClassProperties();
      ArrayNode updatedProperties = objectMapper.createArrayNode();

      for (JsonNode propertyNode : classProperties) {
        if (propertyNode.isObject()) {
          try {
            Map<String, Object> propertyMap =
                objectMapper.convertValue(
                    propertyNode, new TypeReference<Map<String, Object>>() {});
            formPropertyTemplate(updatedProperties, propertyMap, DataDictionary.BSDD);
          } catch (IllegalArgumentException e) {
            throw new JsonValidationException("Skipping invalid propertyNode: {}" + propertyNode);
          }
        }
      }
      classTemplateDto.setClassProperties(updatedProperties);
    }

    return classTemplateDto;
  }

  /**
   * Fetches class template with property details.
   */
  private BsddClassTemplateDto getClassTemplate(String uri, boolean addProperties)
      throws JsonValidationException, JsonProcessingException {

    if (!validateUri(uri)) {
      throw new InvalidInputException("Invalid URI : " + uri);
    }
    UriComponentsBuilder uriBuilder =
        UriComponentsBuilder.fromHttpUrl(props.getBsddClassDetailsUrl())
            .queryParam(AppConstants.URI, uri);

    if (addProperties) {
      uriBuilder.queryParam(AppConstants.QP_BSDD_INCLUDECLASSPROP, true);
    }

    String url = uriBuilder.toUriString();

    // Check data from Redis cache
    BsddClassTemplateDto classTemplateDto =
        cacheService.getClassTemplateFromCache(url, BsddClassTemplateDto.class);
    if (classTemplateDto == null) {
      try {
        ResponseEntity<BsddClassTemplateDto> response =
            restTemplate.getForEntity(url, BsddClassTemplateDto.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
          classTemplateDto = response.getBody();
          cacheService.storeClassTemplateInCache(url, classTemplateDto);
        } else {
          throw new JsonValidationException(
              "Failed to fetch class template. HTTP Status: " + response.getStatusCode());
        }
      } catch (RestClientException e) {
        throw new JsonValidationException("Error fetching class template: " + e.getMessage(), e);
      }
    }

    return classTemplateDto;
  }

  /**
   * Retrieves the property template with its details.
   */
  @Override
  public ObjectNode getPropertyTemplateWithDetails(List<String> uriList)
      throws JsonValidationException {
    ObjectNode template = objectMapper.createObjectNode();
    ArrayNode propertiesArray = objectMapper.createArrayNode();

    for (String uri : uriList) {
      if (!validateUri(uri)) {
        throw new JsonValidationException("Invalid URI : " + uri);
      }
      UriComponentsBuilder uriBuilder =
          UriComponentsBuilder.fromHttpUrl(props.getBsddPropertiesWithDetailUrl())
              .queryParam(AppConstants.URI, uri);
      String url = uriBuilder.toUriString();
      Map<String, Object> response = restTemplate.getForObject(url, Map.class);
      formPropertyTemplate(propertiesArray, response, DataDictionary.BSDD);
    }
    template.set("properties", propertiesArray);

    return template;
  }

  /**
   * Adding new field to make it the appropriate template format.
   */
  private void formPropertyTemplate(
      ArrayNode propertiesArray, Map<String, Object> response, DataDictionary dictionary) {
    Map<String, Object> mappedPropTemplate = new HashMap<String, Object>();
    mappedPropTemplate = dictionaryMapping.mapDataDictionaryFieldToOpenCirc(response, dictionary);
    ObjectNode propertyTemplateNode = objectMapper.valueToTree(mappedPropTemplate);
    propertyTemplateNode.put(AppConstants.ACTUAL_VALUE, "");
    propertiesArray.add(propertyTemplateNode);
  }

  /**
   * Fetches the properties.
   */
  @Override
  public List<Map<String, String>> listProperties(String text) {
    UriComponentsBuilder uriBuilder =
        UriComponentsBuilder.fromHttpUrl(props.getBsddTextSearchUrl())
            .queryParam(AppConstants.QP_BSDD_SEARCHTEXT, text)
            .queryParam(AppConstants.QP_BSDD_TYPEFILTER, "Property")
            .queryParam("IncludeSearchDescriptions", "false")
            .queryParam("Offset", 0);
    String url = uriBuilder.build(false).toUriString();

    ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
    JsonNode responseBody = response.getBody();
    List<Map<String, String>> propertyList = new ArrayList<>();
    if (responseBody != null
        && responseBody.has("properties")
        && responseBody.get("properties").isArray()) {
      for (JsonNode node : responseBody.get("properties")) {
        Map<String, String> propertyMap = new HashMap<>();
        propertyMap.put("name", node.path("name").asText());
        propertyMap.put("uri", node.path("uri").asText());
        propertyMap.put("code", node.path("code").asText());

        propertyList.add(propertyMap);
      }
    }
    return propertyList;
  }

  /**
   * Validates template entries against allowed values and ranges.
   *
   * @param jsonNode The JSON node containing template data.
   * @throws JsonValidationException If validation fails.
   */
  @Override
  public void validatePassportData(JsonNode jsonNode) throws JsonValidationException {

    if (jsonNode == null || jsonNode.isNull() || jsonNode.isObject() && jsonNode.size() == 0) {
      throw new InvalidInputException("Input JSON node is null");
    }

    ArrayNode properties = null;
    try {
      if (jsonNode.has("classType") && "Class".equals(jsonNode.path("classType").asText())) {
        if (jsonNode.has("classProperties")) {
          properties = (ArrayNode) jsonNode.get("classProperties");
        }

      } else if (jsonNode.has("properties")) {
        if (jsonNode.get("properties").isArray()) {
          properties = (ArrayNode) jsonNode.get("properties");
        }

      } else {
        throw new InvalidInputException("Invalid Template");
      }
    } catch (ClassCastException e) {
      throw new InvalidInputException("Expected an array node for properties");
    }

    List<String> errorMessages = new ArrayList<>();
    if (properties != null) {
      for (JsonNode propertyNode : properties) {
        if (propertyNode == null || !propertyNode.isObject()) {
          errorMessages.add("Invalid property node found (not an object). Skipping...");
          continue;
        }
        ObjectNode property = (ObjectNode) propertyNode;

        String propName = property.has("name") ? property.get("name").asText() : null;
        String dataType = property.has("dataType") ? property.get("dataType").asText() : null;
        JsonNode actualValueNode = property.get("actualValue");
        JsonNode allowedValuesNode = property.get("allowedValues");

        Double maxExclusive =
            property.has("MaxExclusive") ? property.get("MaxExclusive").asDouble() : null;
        Double maxInclusive =
            property.has("MaxInclusive") ? property.get("MaxInclusive").asDouble() : null;
        Double minExclusive =
            property.has("MinExclusive") ? property.get("MinExclusive").asDouble() : null;
        Double minInclusive =
            property.has("MinInclusive") ? property.get("MinInclusive").asDouble() : null;

        if (actualValueNode != null && !actualValueNode.asText().isEmpty()) {

          validateDataType(propName, dataType, actualValueNode, errorMessages);

          if (allowedValuesNode != null && allowedValuesNode.isArray()) {
            validateAllowedValues(
                propName, (ArrayNode) allowedValuesNode, actualValueNode, errorMessages);
          }
          if ("Real".equals(dataType)) {
            validateRangeChecks(
                propName,
                actualValueNode,
                maxExclusive,
                maxInclusive,
                minExclusive,
                minInclusive,
                errorMessages);
          }
        } else {
          errorMessages.add("Missing or empty 'actualValue' for property: " + propName);
        }
      }
    }

    if (!errorMessages.isEmpty()) {
      throw new JsonValidationException(
          "Validation failed with the following errors : "
              + "\n\t- "
              + String.join(",\n\t- ", errorMessages));
    }
  }

  /**
   * Validates whether the given URI is correctly formatted.
   *
   * @param uriString The URI string.
   * @return True if valid, false otherwise.
   */
  public boolean validateUri(String uriString) {
    if (uriString == null || uriString.trim().isEmpty()) {
      return false;
    }

    String uriPrefix = "https://identifier.buildingsmart.org/uri";
    try {
      URI uri = new URI(uriString);
      return uri.getScheme() != null && uri.getHost() != null && uriString.startsWith(uriPrefix);
    } catch (URISyntaxException e) {
      return false;
    }
  }

  /**
   * Displays the template from the dictionary without any processing.
   */
  public JsonNode fetchRawTemplate(String uri, TemplateType type) {
    String uriPrefix = switch (type) {
      case CLASS -> props.getBsddClassDetailsUrl();
      case PROPERTY -> props.getBsddPropertiesWithDetailUrl();
    };

    UriComponentsBuilder uriBuilder =
        UriComponentsBuilder.fromHttpUrl(uriPrefix)
            .queryParam(AppConstants.URI, uri)
            .queryParam(AppConstants.QP_BSDD_INCLUDECLASSPROP, true);
    String url = uriBuilder.toUriString();
    ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);

    return response.getBody();
  }
}
