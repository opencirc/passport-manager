package com.opencirc.api.passport.adapter.bsdd;

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
import com.opencirc.api.passport.exception.JsonValidationException;
import com.opencirc.api.passport.model.Datasheet;
import com.opencirc.api.passport.model.DatasheetProperty;
import com.opencirc.api.passport.service.CacheService;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/** BSDD implementation of PlatformAdapter. */
@Service
public class BsddPlatformAdapter implements PlatformAdapter {

  private static final String IFC_IDENTIFIER_URL_PATTERN =
      "https://identifier.buildingsmart.org/uri/buildingsmart/ifc/4.3/class/%s";

  private final RestTemplate restTemplate;

  private final AppProperties appProperties;

  private final ObjectMapper objectMapper;

  private final CacheService cacheService;

  /** Constructor. */
  @Autowired
  public BsddPlatformAdapter(
      RestTemplate injectedRestTemplate,
      AppProperties appProperties,
      ObjectMapper mapper,
      CacheService cacheService) {
    this.restTemplate = injectedRestTemplate;
    this.appProperties = appProperties;
    this.objectMapper = mapper;
    this.cacheService = cacheService;
  }

  /** Fetches a list of classes matching the search text. */
  @Override
  public List<Map<String, String>> listClass(String text) {
    UriComponentsBuilder uriBuilder =
        UriComponentsBuilder.fromHttpUrl(appProperties.getBsddClassSearchByTextUrl())
            .queryParam(AppConstants.QP_BSDD_SEARCHTEXT, text)
            .queryParam(AppConstants.QP_BSDD_LIMIT, AppConstants.BSDD_LIMIT);
    String url = uriBuilder.toUriString();
    ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
    JsonNode responseBody = response.getBody();

    List<Map<String, String>> classList = new ArrayList<>();
    int totalCount = responseBody != null ? responseBody.path("totalCount").asInt() : 0;
    if (totalCount > 0) {
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
   * Fetches a class template with property details and returns a datasheet for it and its related
   * IFC entities.
   */
  @Override
  public List<Datasheet> generateDatasheetsFromPlatformId(String uri, boolean invokedExternally)
      throws JsonValidationException {

    BsddClassTemplateDto classTemplateDto = getClassTemplate(uri);
    Datasheet datasheet = generateDatasheetFromClassTemplateDto(classTemplateDto);
    List<Datasheet> datasheets = new ArrayList<>(Collections.singletonList(datasheet));

    var relatedIfcClassNames = classTemplateDto.getRelatedIfcEntityNames();
    if (!invokedExternally || relatedIfcClassNames == null || relatedIfcClassNames.isEmpty()) {
      return datasheets;
    }

    for (var relatedIfcClass : classTemplateDto.getRelatedIfcEntityNames()) {
      String ifcUri = String.format(IFC_IDENTIFIER_URL_PATTERN, relatedIfcClass);
      Datasheet relatedIfcDatasheet = generateDatasheetFromPlatformId(ifcUri);
      datasheets.add(relatedIfcDatasheet);
    }

    return datasheets;
  }

  /** Fetches class template with property details. */
  public Datasheet generateDatasheetFromPlatformId(String uri) throws JsonValidationException {
    BsddClassTemplateDto classTemplateDto = getClassTemplate(uri);
    return generateDatasheetFromClassTemplateDto(classTemplateDto);
  }

  /** Fetches class template with property details. */
  public Datasheet generateDatasheetFromClassTemplateDto(BsddClassTemplateDto classTemplateDto) {
    Datasheet datasheet = new Datasheet();
    datasheet.setPlatform(Platform.BSDD);
    datasheet.setDictionary(
        classTemplateDto.getReferenceCode().startsWith("Ifc")
            ? DataDictionary.IFC
            : DataDictionary.TABLE6);
    datasheet.setCode(classTemplateDto.getCode());
    datasheet.setName(classTemplateDto.getName());
    datasheet.setDescription(classTemplateDto.getDefinition());
    datasheet.setPlatformId(classTemplateDto.getUri());

    if (classTemplateDto.getClassProperties() != null) {
      datasheet.setDatasheetProperties(new HashSet<>());
      for (var classProperty : classTemplateDto.getClassProperties()) {
        if (classProperty.getPropertyCode() == null || classProperty.getPropertyCode().isBlank()) {
          continue;
        }

        DatasheetProperty property = new DatasheetProperty();
        property.setPlatformId(classProperty.getUri());
        property.setCode(classProperty.getPropertyCode());
        property.setGroupTag(classProperty.getPropertySet());
        property.setPropertyType(classProperty.getDataType());
        property.setDatasheet(datasheet);
        datasheet.getDatasheetProperties().add(property);
      }
    }

    return datasheet;
  }

  /** Fetches class template with property details. */
  private BsddClassTemplateDto getClassTemplate(String uri) throws JsonValidationException {
    URI requestUri =
        UriComponentsBuilder.fromHttpUrl(appProperties.getBsddClassDetailsUrl())
            .queryParam("Uri", uri)
            .queryParam("IncludeClassProperties", true)
            .build(false) // don't re-encode the nested URL parameter
            .toUri();

    String cacheKey = requestUri.toString();

    BsddClassTemplateDto classTemplateDto =
        cacheService.getCachedTemplate(cacheKey, BsddClassTemplateDto.class);
    if (classTemplateDto == null) {
      try {
        ResponseEntity<BsddClassTemplateDto> response =
            restTemplate.getForEntity(requestUri, BsddClassTemplateDto.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
          classTemplateDto = response.getBody();
          cacheService.cacheTemplate(cacheKey, classTemplateDto);
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

  /** Fetches the properties. */
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

  /** Validates template entries against allowed values and ranges. */
  @Override
  public String validatePassportData(JsonNode propertyNode) {
    String errorMessage;
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
      if (dataType == null) {
        return "Missing 'dataType' for property: " + propertyName;
      }
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

  /** Validates whether the given URI is correctly formatted. */
  public boolean validateUri(String uriString) {
    if (!uriString.startsWith("https://identifier.buildingsmart.org/uri")) {
      return false;
    }

    try {
      var uri =
          UriComponentsBuilder.fromHttpUrl(appProperties.getBsddClassDetailsUrl())
              .queryParam("Uri", uriString)
              .queryParam("IncludeClassProperties", true)
              .encode()
              .toUriString();
      return true;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** Validates the data type of the given actual value. */
  private static String validateDataType(
      String propertyName, String dataType, JsonNode actualValueNode) {
    String actualValue = actualValueNode.asText();
    switch (dataType) {
      case "String":
        break;

      case "Integer":
        try {
          Integer.parseInt(actualValue);
        } catch (NumberFormatException e) {
          return propertyName + ": Invalid data type. Expected Integer, but got: " + actualValue;
        }
        break;
      case "Boolean":
        if (!"true".equalsIgnoreCase(actualValue) && !"false".equalsIgnoreCase(actualValue)) {
          return propertyName + ": Invalid data type. Expected Boolean, but got: " + actualValue;
        }
        break;
      case "Real":
        try {
          Double.parseDouble(actualValue.replace("\"", ""));
          if (!actualValue.contains(".")) {
            return propertyName
                + ": Invalid Real number. A valid Real number should contain"
                + " a decimal point.";
          }

        } catch (NumberFormatException e) {
          return propertyName
              + ": Invalid data type. Expected Real (Double), but got: "
              + actualValue;
        }
        break;
      case "Character":
        if (actualValue.length() != 1) {
          return propertyName
              + ": Invalid data type. Expected Character "
              + "(Single character string), but got: "
              + actualValue;
        }
        break;
      case "Time":
        if (actualValue == null) {
          return propertyName + ": Invalid data type. Expected Time (String), but got null";
        }

        boolean valid;

        try {
          LocalDate.parse(actualValue, DateTimeFormatter.ISO_LOCAL_DATE);
          valid = true;
        } catch (DateTimeParseException parseException) {
          try {
            Instant.parse(actualValue);
            valid = true;
          } catch (DateTimeParseException secondParseException) {
            try {
              OffsetDateTime.parse(actualValue, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
              valid = true;
            } catch (DateTimeParseException thirdParseException) {
              valid = false;
            }
          }
        }

        if (!valid) {
          return propertyName + ": Invalid ISO 8601 date/time format. Value: " + actualValue;
        }

        break;
      default:
        return propertyName + ": Unknown data type: " + dataType;
    }
    return null;
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
          propertyName + ": Actual value: " + actualValue + " is not within the allowed values.";
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
          propertyName + ": Invalid data type. Expected Real (Double), but got: " + actualValue;
    }

    if (maxExclusive != null && realValue >= maxExclusive) {
      errorMessage =
          propertyName
              + ": Actual value: "
              + realValue
              + " exceeds MaxExclusive limit: "
              + maxExclusive;
    }

    if (maxInclusive != null && realValue > maxInclusive) {
      errorMessage =
          propertyName
              + ": Actual value: "
              + realValue
              + " exceeds MaxInclusive limit: "
              + maxInclusive;
    }

    if (minExclusive != null && realValue <= minExclusive) {
      errorMessage =
          propertyName
              + ": Actual value: "
              + realValue
              + " is below MinExclusive limit: "
              + minExclusive;
    }

    if (minInclusive != null && realValue < minInclusive) {
      errorMessage =
          propertyName
              + ": Actual value: "
              + realValue
              + " is below MinInclusive limit: "
              + minInclusive;
    }
    return errorMessage;
  }

  /** Displays the template from the dictionary without any processing. */
  public JsonNode fetchRawTemplate(String uri) {
    String uriPrefix = appProperties.getBsddClassDetailsUrl();
    UriComponentsBuilder uriBuilder =
        UriComponentsBuilder.fromHttpUrl(uriPrefix)
            .queryParam("Uri", uri)
            .queryParam("IncludeClassProperties", true);
    String url = uriBuilder.toUriString();
    ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);

    return response.getBody();
  }

  /** Retrieves the tree structure of the dictionary. */
  public List<DataDictionaryTreeStructureDto> getDictionaryTreeStructure(DataDictionary dictionary)
      throws IOException {
    String structurePathString =
        switch (dictionary) {
          case DataDictionary.TABLE6 -> appProperties.getTable6StructureJsonPath();
          case DataDictionary.IFC -> appProperties.getIfcStructureJsonPath();
        };

    Path structurePath = Paths.get(structurePathString).toAbsolutePath();
    if (!Files.exists(structurePath)) {
      createTreeStructure(dictionary);
    }

    return Arrays.asList(
        objectMapper.readValue(structurePath.toFile(), DataDictionaryTreeStructureDto[].class));
  }

  private void createTreeStructure(DataDictionary dictionary) throws IOException {
    String rawStructurePathString =
        switch (dictionary) {
          case DataDictionary.TABLE6 -> appProperties.getTable6RawStructureJsonPath();
          case DataDictionary.IFC -> appProperties.getIfcRawStructureJsonPath();
        };

    ClassPathResource templateResource = new ClassPathResource(rawStructurePathString);

    if (!templateResource.exists()) {
      throw new IllegalStateException("Template not found in classpath: " + rawStructurePathString);
    }

    JsonNode root = objectMapper.readTree(templateResource.getInputStream());
    JsonNode classes = root.get("Classes");

    if (classes == null) {
      throw new IllegalStateException("Template JSON has no 'Classes' field.");
    }

    Map<String, DataDictionaryTreeStructureDto> nodeMap = new LinkedHashMap<>();
    List<DataDictionaryTreeStructureDto> roots = new ArrayList<>();

    for (JsonNode classNode : classes) {
      var codeNode = classNode.get("Code");
      var nameNode = classNode.get("Name");
      if (codeNode == null || nameNode == null) {
        throw new IllegalStateException("Class node missing required 'Code' or 'Name' field");
      }
      var code = codeNode.asText();
      var name = nameNode.asText();
      if (code == null || name == null || code.isBlank() || name.isBlank()) {
        throw new IllegalStateException("Class node missing required 'Code' or 'Name' field");
      }
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
          parent.getChildren().add(node);
        } else {
          roots.add(node);
        }
      }
    }

    String structurePathString =
        switch (dictionary) {
          case DataDictionary.TABLE6 -> appProperties.getTable6StructureJsonPath();
          case DataDictionary.IFC -> appProperties.getIfcStructureJsonPath();
        };

    Path outputPath = Paths.get(structurePathString).toAbsolutePath();
    Path parent = outputPath.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }

    objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputPath.toFile(), roots);
  }
}
