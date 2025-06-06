package com.oc.api.passport.adapter.bsdd;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oc.api.passport.adapter.DictionaryAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.oc.api.passport.config.AppProperties;
import com.oc.api.passport.constants.AppConstants;
import com.oc.api.passport.exception.JsonValidationException;
import com.oc.api.passport.exception.InvalidInputException;
import com.oc.api.passport.mapping.DictionaryMapping;
import com.oc.api.passport.service.CacheService;

@Service
public class BsDDAdapter implements DictionaryAdapter {

    /**
     * Injecting Restemplate.
     */
    private final RestTemplate restTemplate;

    /**
     * Injecting Properties.
     */
    private final AppProperties props;

    /**
     * Instantiating BsddAdapter.
     *
     * @param injectedRestTemplate
     * @param properties
     */
    @Autowired
    public BsDDAdapter(RestTemplate injectedRestTemplate, AppProperties properties) {
        this.restTemplate = injectedRestTemplate;
        this.props = properties;
    }

    /**
     * Injecting ObjectMapper.
     */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Injecting DictionaryMapping.
     */
    @Autowired
    private DictionaryMapping dictionaryMapping;

    /**
     * Injecting CacheService.
     */
    @Autowired
    private CacheService cacheService;


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
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(props.getBsDDClassSearchTextURL())
                .queryParam(AppConstants.QP_BSDD_SEARCHTEXT, text)
                .queryParam(AppConstants.QP_BSDD_LIMIT, AppConstants.NUM_TWENTY);
        String url = uriBuilder.toUriString();
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(url,
                JsonNode.class);
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
    public JsonNode createClassTemplate(String uri, boolean addProperties)
            throws JsonValidationException, JsonProcessingException {

        JsonNode classTemplate = getClassTemplate(uri, addProperties);

        if (!(classTemplate instanceof ObjectNode)) {
            throw new JsonValidationException(
                    "Invalid response format for URI: " + uri);
        }

        ObjectNode rootObject = (ObjectNode) classTemplate;

        if (addProperties) {
            JsonNode classPropertiesNode = rootObject
                    .get(AppConstants.BSDD_FIELD_CLASSPROPERTIES);

            if (classPropertiesNode != null && classPropertiesNode.isArray()) {
                ArrayNode classProperties = (ArrayNode) classPropertiesNode;
                ArrayNode updatedProperties = objectMapper.createArrayNode();

                for (JsonNode propertyNode : classProperties) {
                    if (propertyNode.isObject()) {
                        try {
                            Map<String, Object> propertyMap = objectMapper.convertValue(
                                    propertyNode,
                                    new TypeReference<Map<String, Object>>() {
                                    });
                            formPropertyTemplate(updatedProperties, propertyMap, "bsDD");
                        } catch (IllegalArgumentException e) {
                            throw new JsonValidationException(
                                    "Skipping invalid propertyNode: {}" + propertyNode);
                        }
                    }
                }
                rootObject.set(AppConstants.BSDD_FIELD_CLASSPROPERTIES,
                        updatedProperties);
            }
        }

        return rootObject;
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
    private JsonNode getClassTemplate(String uri, boolean addProperties)
            throws JsonValidationException, JsonProcessingException {

        if (!validateUri(uri)) {
            throw new JsonValidationException("Invalid URI : " + uri);
        }
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(props.getBsDDClassDetailsURL())
                .queryParam(AppConstants.URI, uri);

        if (addProperties) {
            uriBuilder.queryParam(AppConstants.QP_BSDD_INCLUDECLASSPROP, true);
        }

        String url = uriBuilder.toUriString();

        // Check data from Redis cache
        JsonNode rootNode = cacheService.getClassTemplateFromCache(url);
        if (rootNode == null) {
            try {
                ResponseEntity<JsonNode> response = restTemplate.getForEntity(url,
                        JsonNode.class);
                if (response.getStatusCode().is2xxSuccessful()
                        && response.getBody() != null) {
                    rootNode = response.getBody();
                    cacheService.storeClassTemplateInCache(url, rootNode);
                } else {
                    throw new JsonValidationException(
                            "Failed to fetch class template. HTTP Status: "
                                    + response.getStatusCode());
                }
            } catch (RestClientException e) {
                throw new JsonValidationException(
                        "Error fetching class template: " + e.getMessage(), e);
            }
        }

        if (!(rootNode instanceof ObjectNode)) {
            throw new JsonValidationException(
                    "Unexpected format in class template for URI: " + uri);
        }

        ObjectNode rootObject = (ObjectNode) rootNode;
        rootObject.put(AppConstants.TEMPLATE_NAME, "");
        rootObject.put(AppConstants.DATA_CATEGORY_FIELD, "");
        return rootObject;
    }

    /**
     * Retrieves the property template with its details.
     *
     * @param uriList
     * @return property template in json format
     */
    @Override
    public JsonNode getPropertyTemplatewithDetails(List<String> uriList)
            throws JsonValidationException {
        ObjectNode template = objectMapper.createObjectNode();
        ArrayNode propertiesArray = objectMapper.createArrayNode();
        template.put("templateName", "");
        template.put("dataCategory", "");

        for (String uri : uriList) {
            System.out.println(uri);
            if (!validateUri(uri)) {
                throw new JsonValidationException("Invalid URI : " + uri);
            }
            UriComponentsBuilder uriBuilder = UriComponentsBuilder
                    .fromHttpUrl(props.getBsDDPropertiesWithDetailURL())
                    .queryParam(AppConstants.URI, uri);
            String url = uriBuilder.toUriString();
            Map<String, Object> response = restTemplate.getForObject(url,
                    Map.class);
            formPropertyTemplate(propertiesArray, response, "bsDD");
        }
        template.set("properties", propertiesArray);

        return template;
    }

    /**
     * Adding new field to make it the appropriate template format.
     *
     * @param propertiesArray    The array of properties.
     * @param response           The response data map.
     * @param dataDictionaryName The data dictionary name.
     */
    private void formPropertyTemplate(ArrayNode propertiesArray,
            Map<String, Object> response, String dataDictionaryName) {
        Map<String, Object> mappedPropTemplate = new HashMap<String, Object>();
        mappedPropTemplate = dictionaryMapping.mapDDFieldtoOC(response,
                dataDictionaryName);
        ObjectNode propertyTemplateNode = objectMapper
                .valueToTree(mappedPropTemplate);
        propertyTemplateNode.put(AppConstants.ACTUAL_VALUE, "");
        propertiesArray.add(propertyTemplateNode);

    }

    /**
     * Fetches the properties.
     *
     * @param text
     * @return list of properties
     */
    @Override
    public List<Map<String, String>> listProperties(String text) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(props.getBsDDTextSearchURL())
                .queryParam(AppConstants.QP_BSDD_SEARCHTEXT, text)
                .queryParam(AppConstants.QP_BSDD_TYPEFILTER, "Property")
                .queryParam("IncludeSearchDescriptions", "false")
                .queryParam("Offset", 0);
        // .queryParam(AppConstants.QP_BSDD_LIMIT, 20)
        String url = uriBuilder.build(false).toUriString();

        ResponseEntity<JsonNode> response = restTemplate.getForEntity(url,
                JsonNode.class);
        JsonNode responseBody = response.getBody();
        List<Map<String, String>> propertyList = new ArrayList<>();
        if (responseBody != null && responseBody.has("properties")
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
    public void validateTemplateEntry(JsonNode jsonNode)
            throws JsonValidationException {

        if (jsonNode == null || jsonNode.isNull()
                || jsonNode.isObject() && jsonNode.size() == 0) {
            throw new InvalidInputException("Input JSON node is null");
        }

        ArrayNode properties = null;
        System.out.println(jsonNode.toString());
        try {
            if (jsonNode.has("classType")
                    && "Class".equals(jsonNode.path("classType").asText())) {
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
                    errorMessages.add(
                            "Invalid property node found (not an object). Skipping...");
                    continue;
                }
                ObjectNode property = (ObjectNode) propertyNode;

                String propName = property.has("name") ? property.get("name").asText()
                        : null;
                String dataType = property.has("dataType")
                        ? property.get("dataType").asText()
                        : null;
                JsonNode actualValueNode = property.get("actualValue");
                JsonNode allowedValuesNode = property.get("allowedValues");

                Double maxExclusive = property.has("MaxExclusive")
                        ? property.get("MaxExclusive").asDouble()
                        : null;
                Double maxInclusive = property.has("MaxInclusive")
                        ? property.get("MaxInclusive").asDouble()
                        : null;
                Double minExclusive = property.has("MinExclusive")
                        ? property.get("MinExclusive").asDouble()
                        : null;
                Double minInclusive = property.has("MinInclusive")
                        ? property.get("MinInclusive").asDouble()
                        : null;

                if (actualValueNode != null && !actualValueNode.asText().isEmpty()) {

                    validateDataType(propName, dataType, actualValueNode, errorMessages);

                    if (allowedValuesNode != null && allowedValuesNode.isArray()) {
                        validateAllowedValues(propName, (ArrayNode) allowedValuesNode,
                                actualValueNode, errorMessages);
                    }
                    if ("Real".equals(dataType)) {
                        validateRangeChecks(propName, actualValueNode, maxExclusive,
                                maxInclusive, minExclusive, minInclusive, errorMessages);
                    }
                } else {
                    errorMessages.add(
                            "Missing or empty 'actualValue' for property: " + propName);
                }
            }
        }

        if (!errorMessages.isEmpty()) {
            throw new JsonValidationException(
                    "Validation failed with the following errors : " + "\n\t- "
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
            return uri.getScheme() != null && uri.getHost() != null
                    && uriString.startsWith(uriPrefix);
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private static void validateDataType(String propName, String dataType,
            JsonNode actualValueNode, List<String> errorMessages) {
        String actualValue = actualValueNode.asText();

        switch (dataType) {
        case "Integer":
            try {
                Integer.parseInt(actualValue);
            } catch (NumberFormatException e) {
                errorMessages.add(propName
                        + " : Invalid data type. Expected Integer, but got: "
                        + actualValue);
            }
            break;
        case "Boolean":
            if (!"true".equalsIgnoreCase(actualValue)
                    && !"false".equalsIgnoreCase(actualValue)) {
                errorMessages.add(propName
                        + " : Invalid data type. Expected Boolean, but got: "
                        + actualValue);
            }
            break;
        case "Real":
            try {
                Double.parseDouble(actualValue.replace("\"", ""));
                if (!actualValue.contains(".")) {
                    errorMessages.add(propName
                            + " : Invalid Real number. A valid Real number should contain"
                            + " a decimal point.");
                }

            } catch (NumberFormatException e) {
                errorMessages.add(propName
                        + " : Invalid data type. Expected Real (Double), but got: "
                        + actualValue);
            }
            break;
        case "String":
            if (!(actualValue instanceof String)) {
                errorMessages.add(propName
                        + " : Invalid data type. Expected String, but got: "
                        + actualValue);
            }
            break;
        case "Character":
            if (actualValue.length() != 1) {
                errorMessages.add(propName
                        + " : Invalid data type. Expected Character "
                        + "(Single character string), but got: "
                        + actualValue);
            }
            break;
        case "Time":
            if (!(actualValue instanceof String)) {
                errorMessages.add(propName
                        + " : Invalid data type. Expected Time (String), but got: "
                        + actualValue);
            }
            break;
        default:
            errorMessages.add(propName + " : Unknown data type: " + dataType);
        }
    }

    private static void validateAllowedValues(String propName,
            ArrayNode allowedValues, JsonNode actualValue,
            List<String> errorMessages) {
        boolean isValid = false;

        for (JsonNode allowedValueNode : allowedValues) {
            String value = allowedValueNode.get("value").asText();

            if (value.equals(actualValue.asText())) {
                isValid = true;
                break;
            }
        }

        if (!isValid) {
            errorMessages.add(propName + " : Actual value: " + actualValue
                    + " is not within the allowed values.");
        }
    }

    private static void validateRangeChecks(String propName, Object actualValue,
            Double maxExclusive, Double maxInclusive, Double minExclusive,
            Double minInclusive, List<String> errorMessages) {
        double realValue = 0.0;
        try {
            realValue = Double.parseDouble(
                    actualValue.toString().trim().replace("\"", ""));
        } catch (NumberFormatException e) {
            errorMessages.add(propName
                    + " : Invalid data type. Expected Real (Double), but got: "
                    + actualValue);
        }

        if (maxExclusive != null && realValue >= maxExclusive) {
            errorMessages.add(propName + " : Actual value: " + realValue
                    + " exceeds MaxExclusive limit: " + maxExclusive);
        }

        if (maxInclusive != null && realValue > maxInclusive) {
            errorMessages.add(propName + " : Actual value: " + realValue
                    + " exceeds MaxInclusive limit: " + maxInclusive);
        }

        if (minExclusive != null && realValue <= minExclusive) {
            errorMessages.add(propName + " : Actual value: " + realValue
                    + " is below MinExclusive limit: " + minExclusive);
        }

        if (minInclusive != null && realValue < minInclusive) {
            errorMessages.add(propName + " : Actual value: " + realValue
                    + " is below MinInclusive limit: " + minInclusive);
        }
    }

    /**
     * Displays the template from the dictionary without any processing.
     * @param uri
     * @param type
     * @return response
     * @throws JsonProcessingException
     */
    public JsonNode fetchRawTemplate(String uri, String type)
            throws JsonProcessingException {
        String uriPrefix = null;
        if (type.equalsIgnoreCase("class")) {
            uriPrefix = props.getBsDDClassDetailsURL();
        } else if (type.equalsIgnoreCase("property")) {
            uriPrefix = props.getBsDDPropertiesWithDetailURL();
        }

        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(uriPrefix).queryParam(AppConstants.URI, uri)
                .queryParam(AppConstants.QP_BSDD_INCLUDECLASSPROP, true);
        String url = uriBuilder.toUriString();
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(url,
                JsonNode.class);

        return response.getBody();
    }

}
