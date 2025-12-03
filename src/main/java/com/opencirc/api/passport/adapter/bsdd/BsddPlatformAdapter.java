package com.opencirc.api.passport.adapter.bsdd;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencirc.api.passport.adapter.PlatformAdapter;
import com.opencirc.api.passport.config.AppProperties;
import com.opencirc.api.passport.constants.AppConstants;
import com.opencirc.api.passport.dto.BsddClassTemplateDto;
import com.opencirc.api.passport.dto.DataDictionaryTreeStructureDto;
import com.opencirc.api.passport.enums.DataDictionary;
import com.opencirc.api.passport.enums.Platform;
import com.opencirc.api.passport.exception.InvalidInputException;
import com.opencirc.api.passport.exception.JsonValidationException;
import com.opencirc.api.passport.mapping.DictionaryMapping;
import com.opencirc.api.passport.service.CacheService;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class BsddPlatformAdapter implements PlatformAdapter<BsddClassTemplateDto> {

  /** Injecting Rest Template. */
  private final RestTemplate restTemplate;

  /** Injecting Properties. */
  private final AppProperties appProperties;

  /** Injecting ObjectMapper. */
  private final ObjectMapper objectMapper;

  /** Injecting DictionaryMapping. */
  private final DictionaryMapping dictionaryMapping;

  /** Injecting CacheService. */
  private final CacheService cacheService;

  /**
   * Instantiating BsddPlatformAdapter.
   *
   * @param injectedRestTemplate
   * @param appProperties
   * @param mapper
   * @param dictionaryMapping
   * @param cacheService
   */
  @Autowired
  public BsddPlatformAdapter(
      RestTemplate injectedRestTemplate,
      AppProperties appProperties,
      ObjectMapper mapper,
      DictionaryMapping dictionaryMapping,
      CacheService cacheService) {
    this.restTemplate = injectedRestTemplate;
    this.appProperties = appProperties;
    this.objectMapper = mapper;
    this.cacheService = cacheService;
    this.dictionaryMapping = dictionaryMapping;
  }

  /**
   * Fetches a list of classes matching the search text.
   *
   * @param text The search text.
   * @return A list of maps containing class details.
   */
  @Override
  public List<Map<String, String>> listClass(String text) {

    UriComponentsBuilder uriBuilder =
        UriComponentsBuilder.fromHttpUrl(appProperties.getBsddClassSearchTextUrl())
            .queryParam(AppConstants.QP_BSDD_SEARCHTEXT, text)
            .queryParam(AppConstants.QP_BSDD_LIMIT, AppConstants.BSDD_LIMIT);
    String url = uriBuilder.toUriString();
    ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
    JsonNode responseBody = response.getBody();

    List<Map<String, String>> classList = new ArrayList<>();
    int totalCount = responseBody.path("totalCount").asInt();
    if (responseBody != null && totalCount > 0) {
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
   * Fetches class template with property details.
   *
   * @param uri The class URI.
   * @return The class template as a JsonNode.
   * @throws JsonValidationException If the URI is invalid.
   * @throws JsonProcessingException
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
            formPropertyTemplate(updatedProperties, propertyNode);
          } catch (Exception e) {
            throw new JsonValidationException("Invalid propertyNode: " + propertyNode, e);
          }
        }
      }

      classTemplateDto.setClassProperties(updatedProperties);
    }

    return classTemplateDto;
  }

  /**
   * Fetches class template with property details.
   *
   * @param uri The class URI.
   * @param addProperties
   * @return The class template as a JsonNode.
   * @throws JsonValidationException If the URI is invalid.
   * @throws JsonProcessingException
   */
  private BsddClassTemplateDto getClassTemplate(String uri, boolean addProperties)
      throws JsonValidationException, JsonProcessingException {

    if (!validateUri(uri)) {
      throw new InvalidInputException("Invalid URI : " + uri);
    }
    UriComponentsBuilder uriBuilder =
        UriComponentsBuilder.fromHttpUrl(appProperties.getBsddClassDetailsUrl())
            .queryParam(AppConstants.URI, uri);

    if (addProperties) {
      uriBuilder.queryParam(AppConstants.QP_BSDD_INCLUDECLASSPROP, true);
    }

    String url = uriBuilder.toUriString();

    BsddClassTemplateDto classTemplateDto =
        cacheService.getCachedTemplate(url, BsddClassTemplateDto.class);
    if (classTemplateDto == null) {
      try {
        ResponseEntity<BsddClassTemplateDto> response =
            restTemplate.getForEntity(url, BsddClassTemplateDto.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
          classTemplateDto = response.getBody();
          cacheService.cacheTemplate(url, classTemplateDto);
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
   *
   * @param uriList
   * @return property template in json format
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
          UriComponentsBuilder.fromHttpUrl(appProperties.getBsddPropertiesWithDetailUrl())
              .queryParam(AppConstants.URI, uri);
      String url = uriBuilder.toUriString();

      JsonNode propertyTemplate = cacheService.getCachedTemplate(url, JsonNode.class);

      if (propertyTemplate == null) {
        try {
          ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
          if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new JsonValidationException(
                "Failed to fetch template. HTTP Status: " + response.getStatusCode());
          }
          propertyTemplate = response.getBody();
          cacheService.cacheTemplate(url, propertyTemplate);
        } catch (RestClientException e) {
          throw new JsonValidationException(
              "Error fetching property template: " + e.getMessage(), e);
        }
      }
      formPropertyTemplate(propertiesArray, propertyTemplate);
    }

    template.set("properties", propertiesArray);
    return template;
  }

  /**
   * Maps the given template to standard fields and adds an actual value field.
   *
   * @param propertiesArray
   * @param template
   */
  private void formPropertyTemplate(ArrayNode propertiesArray, JsonNode template) {
    ObjectNode mappedNode = dictionaryMapping.mapTemplateFieldsToStandards(template, Platform.BSDD);
    mappedNode.put(AppConstants.ACTUAL_VALUE, "");
    propertiesArray.add(mappedNode);
  }

  /**
   * Fetches the properties.
   *
   * @param text
   * @return list of properties
   */
  @Override
  public List<Map<String, String>> listProperties(String text) {
    UriComponentsBuilder uriBuilder =
        UriComponentsBuilder.fromHttpUrl(appProperties.getBsddTextSearchUrl())
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
   * @param propertyNode The JSON node containing template data.
   * @throws JsonValidationException If validation fails.
   */
  @Override
  public String validatePassportData(JsonNode propertyNode) throws JsonValidationException {

    String errorMessage = null;
    if (propertyNode == null || !propertyNode.isObject()) {
      return "Invalid property node found (not an object). Skipping...";
    }
    ObjectNode property = (ObjectNode) propertyNode;

    String propertyName = property.has("name") ? property.get("name").asText() : null;
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

      errorMessage = validateDataType(propertyName, dataType, actualValueNode);

      if (errorMessage == null && allowedValuesNode != null && allowedValuesNode.isArray()) {
        errorMessage =
            validateAllowedValues(propertyName, (ArrayNode) allowedValuesNode, actualValueNode);
      }
      if (errorMessage == null && "Real".equals(dataType)) {
        errorMessage =
            validateRangeChecks(
                propertyName,
                actualValueNode,
                maxExclusive,
                maxInclusive,
                minExclusive,
                minInclusive);
      }
    } else {
      return "Missing or empty 'actualValue' for property: " + propertyName;
    }
    return errorMessage;
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

  private static String validateDataType(
      String propertyName, String dataType, JsonNode actualValueNode) {
    String actualValue = actualValueNode.asText();
    String errorMessage = null;
    switch (dataType) {
      case "String":
        break;

      case "Integer":
        try {
          Integer.parseInt(actualValue);
        } catch (NumberFormatException e) {
          return propertyName + " : Invalid data type. Expected Integer, but got: " + actualValue;
        }
        break;
      case "Boolean":
        if (!"true".equalsIgnoreCase(actualValue) && !"false".equalsIgnoreCase(actualValue)) {
          return propertyName + " : Invalid data type. Expected Boolean, but got: " + actualValue;
        }
        break;
      case "Real":
        try {
          Double.parseDouble(actualValue.replace("\"", ""));
          if (!actualValue.contains(".")) {
            return propertyName
                + " : Invalid Real number. A valid Real number should contain"
                + " a decimal point.";
          }

        } catch (NumberFormatException e) {
          return propertyName
              + " : Invalid data type. Expected Real (Double), but got: "
              + actualValue;
        }
        break;
      case "Character":
        if (actualValue.length() != 1) {
          return propertyName
              + " : Invalid data type. Expected Character "
              + "(Single character string), but got: "
              + actualValue;
        }
        break;
      case "Time":
        if (!(actualValue instanceof String)) {
          return propertyName
              + " : Invalid data type. Expected Time (String), but got: "
              + actualValue;
        }
        boolean valid = false;

        try {
          LocalDate.parse(actualValue, DateTimeFormatter.ISO_LOCAL_DATE);
          valid = true;
        } catch (DateTimeParseException e1) {
          try {
            Instant.parse(actualValue);
            valid = true;
          } catch (DateTimeParseException e2) {
            try {
              OffsetDateTime.parse(actualValue, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
              valid = true;
            } catch (DateTimeParseException e3) {
              valid = false;
            }
          }
        }
        if (!valid) {
          return propertyName + " : Invalid ISO 8601 date/time format. Value: " + actualValue;
        }

        break;
      default:
        return propertyName + " : Unknown data type: " + dataType;
    }
    return errorMessage;
  }

  private static String validateAllowedValues(
      String propertyName, ArrayNode allowedValues, JsonNode actualValue) {
    boolean isValid = false;
    String errorMessage = null;
    for (JsonNode allowedValueNode : allowedValues) {
      String value = allowedValueNode.get("value").asText();

      if (value.equals(actualValue.asText())) {
        isValid = true;
        break;
      }
    }

    if (!isValid) {
      errorMessage =
          propertyName + " : Actual value: " + actualValue + " is not within the allowed values.";
    }
    return errorMessage;
  }

  private static String validateRangeChecks(
      String propertyName,
      Object actualValue,
      Double maxExclusive,
      Double maxInclusive,
      Double minExclusive,
      Double minInclusive) {
    double realValue = 0.0;
    String errorMessage = null;
    try {
      realValue = Double.parseDouble(actualValue.toString().trim().replace("\"", ""));
    } catch (NumberFormatException e) {
      errorMessage =
          propertyName + " : Invalid data type. Expected Real (Double), but got: " + actualValue;
    }

    if (maxExclusive != null && realValue >= maxExclusive) {
      errorMessage =
          propertyName
              + " : Actual value: "
              + realValue
              + " exceeds MaxExclusive limit: "
              + maxExclusive;
    }

    if (maxInclusive != null && realValue > maxInclusive) {
      errorMessage =
          propertyName
              + " : Actual value: "
              + realValue
              + " exceeds MaxInclusive limit: "
              + maxInclusive;
    }

    if (minExclusive != null && realValue <= minExclusive) {
      errorMessage =
          propertyName
              + " : Actual value: "
              + realValue
              + " is below MinExclusive limit: "
              + minExclusive;
    }

    if (minInclusive != null && realValue < minInclusive) {
      errorMessage =
          propertyName
              + " : Actual value: "
              + realValue
              + " is below MinInclusive limit: "
              + minInclusive;
    }
    return errorMessage;
  }

  /**
   * Displays the template from the dictionary without any processing.
   *
   * @param uri
   * @param type
   * @return response
   * @throws JsonProcessingException
   */
  public JsonNode fetchRawTemplate(String uri, String type) throws JsonProcessingException {
    String uriPrefix = null;
    if (type.equalsIgnoreCase("class")) {
      uriPrefix = appProperties.getBsddClassDetailsUrl();
    } else if (type.equalsIgnoreCase("property")) {
      uriPrefix = appProperties.getBsddPropertiesWithDetailUrl();
    } else {
      throw new IllegalArgumentException("Invalid type: " + type);
    }

    UriComponentsBuilder uriBuilder =
        UriComponentsBuilder.fromHttpUrl(uriPrefix)
            .queryParam(AppConstants.URI, uri)
            .queryParam(AppConstants.QP_BSDD_INCLUDECLASSPROP, true);
    String url = uriBuilder.toUriString();
    ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);

    return response.getBody();
  }

  /** Retrieves the tree structure of the dictionary. */
  public List<DataDictionaryTreeStructureDto> getDictionaryTreeStructure(DataDictionary dictionary)
      throws JsonValidationException, IOException {
    String cachePath =
        switch (dictionary) {
          case DataDictionary.TABLE6 -> appProperties.getTable6StructureOutputCachedPath();
          default -> throw new IllegalArgumentException("Unsupported dictionary: " + dictionary);
        };

    String templatePath =
        switch (dictionary) {
          case DataDictionary.TABLE6 -> appProperties.getTable6StructureJsonPath();
          default -> throw new IllegalArgumentException("Unsupported dictionary: " + dictionary);
        };

    Path outputPath = Paths.get(cachePath).toAbsolutePath();

    if (Files.exists(outputPath)) {
      return Arrays.asList(
          objectMapper.readValue(outputPath.toFile(), DataDictionaryTreeStructureDto[].class));
    }

    ClassPathResource templateResource = new ClassPathResource(templatePath);

    if (!templateResource.exists()) {
      throw new IllegalStateException("Template not found in classpath: " + templatePath);
    }

    JsonNode root = objectMapper.readTree(templateResource.getInputStream());
    JsonNode classes = root.get("Classes");

    if (classes == null) {
      throw new IllegalStateException("Template JSON has no 'Classes' field.");
    }

    Map<String, DataDictionaryTreeStructureDto> nodeMap = new LinkedHashMap<>();
    List<DataDictionaryTreeStructureDto> roots = new ArrayList<>();

    for (JsonNode classNode : classes) {
      String code = classNode.get("Code").asText();
      String name = classNode.get("Name").asText();
      nodeMap.put(code, new DataDictionaryTreeStructureDto(code, name, new ArrayList<>()));
    }

    for (JsonNode classNode : classes) {
      JsonNode codeNode = classNode.get("Code");
      if (codeNode == null || codeNode.isNull()) {
        throw new IllegalStateException("Class node missing required 'Code' field");
      }
      String code = codeNode.asText();

      JsonNode parentCodeNode = classNode.get("ParentClassCode");
      String parentCode =
          (parentCodeNode != null && !parentCodeNode.isNull()) ? parentCodeNode.asText() : null;

      DataDictionaryTreeStructureDto node = nodeMap.get(code);

      if (parentCode == null || parentCode.isBlank()) {
        roots.add(node);
      } else {
        DataDictionaryTreeStructureDto parent = nodeMap.get(parentCode);
        if (parent != null) {
          parent.addChild(node);
        } else {
          roots.add(node);
        }
      }
    }

    Files.createDirectories(outputPath.getParent());

    objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputPath.toFile(), roots);
    return roots;
  }
}
