package com.opencirc.api.passport.command;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencirc.api.passport.config.AppProperties;
import com.opencirc.api.passport.dao.UserRepository;
import com.opencirc.api.passport.dto.CreatePassportRequestDto;
import com.opencirc.api.passport.dto.PassportDto;
import com.opencirc.api.passport.enums.DataDictionary;
import com.opencirc.api.passport.exception.JsonValidationException;
import com.opencirc.api.passport.model.Datasheet.DataCategory;
import com.opencirc.api.passport.model.User;
import com.opencirc.api.passport.service.DataDictionaryService;
import com.opencirc.api.passport.service.PassportService;

import lombok.extern.slf4j.Slf4j;

/**
 * Seeder for generating sample passports based on predefined URIs and data
 * dictionary templates.
 */
@Component
@Slf4j
public class PassportSeederFromApi {

    /**
     * Application properties class.
     */
    private final AppProperties appProperties;

    /**
     * Service to get user details.
     */
    private final UserRepository userRepository;

    /**
     * Service to retrieve data dictionary templates.
     */
    private final DataDictionaryService dataDictionaryService;

    /**
     * Service responsible for creating passports.
     */
    private final PassportService passportService;

    /**
     * ObjectMapper.
     */
    private final ObjectMapper objectMapper;

    /**
     * Random number generator for generating property values.
     */
    private static final Random RANDOM = new Random();

    /**
     * The data dictionary used for seeding passport templates.
     */
    private final DataDictionary dictionary = DataDictionary.BSDD;

    /**
     * Cache the class templates.
     */
    private final Map<String, JsonNode> templateCache = new ConcurrentHashMap<>();

    /**
     * Constructor-based dependency injection.
     *
     * @param dataDictionaryServiceParam
     * @param passportServiceParam
     * @param objectMapperParam
     * @param appPropertiesParam
     * @param userRepositoryParam
     */
    public PassportSeederFromApi(DataDictionaryService dataDictionaryServiceParam,
            PassportService passportServiceParam, ObjectMapper objectMapperParam,
            AppProperties appPropertiesParam, UserRepository userRepositoryParam) {
        this.dataDictionaryService = dataDictionaryServiceParam;
        this.passportService = passportServiceParam;
        this.objectMapper = objectMapperParam;
        this.appProperties = appPropertiesParam;
        this.userRepository = userRepositoryParam;
    }

    /**
     * Seeds sample passports recursively using the predefined URI list.
     *
     * @throws RuntimeException if passport seeding fails.
     */
    public void seed() {
        try {

            User user = userRepository.findAll().stream().findFirst().orElseThrow(
                    () -> new IllegalStateException("No users found in the database. "
                            + "Please seed users before running passport seeding."));

            List<String> uris = appProperties.getUriList();
            if (uris == null || uris.isEmpty()) {
                throw new IllegalStateException(
                        "URI list cannot be empty for passport seeding");
            }
            for (int index = 0; index < appProperties.getChildrenPerLevel(); index++) {
                String uri = uris.get(index % uris.size());
                createPassportRecursive(1, String.valueOf(index + 1), uri, index, null,
                        String.valueOf(user.getId()));
            }
            log.info("Passport seeding completed.");
        } catch (RuntimeException e) {
            log.error("Passport seeding failed: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Passport seeding failed: {}", e.getMessage(), e);
            throw new RuntimeException("Passport seeding failed", e);
        }
    }

    /**
     * Recursively creates passports and their child passports.
     *
     * @param level
     * @param nameSuffix
     * @param uri
     * @param uriIndex
     * @param parentId
     * @param userId
     */
    private void createPassportRecursive(int level, String nameSuffix, String uri,
            int uriIndex, String parentId, String userId) {
        if (level > appProperties.getMaximumLevel()) {
            return;
        }

        JsonNode templateNode = templateCache.computeIfAbsent(uri, currentUri -> {
            Object template = null;
            try {
                template = dataDictionaryService.createClassTemplate(dictionary,
                        currentUri, true);
            } catch (JsonProcessingException | JsonValidationException e) {
                throw new RuntimeException(
                        "Failed to create template for URI: " + currentUri, e);
            }
            return objectMapper.valueToTree(template);
        }).deepCopy();
        JsonNode datasheetData = filterAndFillProperties(templateNode);

        CreatePassportRequestDto request = new CreatePassportRequestDto();
        request.setDatasheetData(datasheetData);
        request.setDataCategory(DataCategory.GENERIC.getValue());
        request.setPassportName("Passport" + nameSuffix);
        request.setCreatedBy(userId);
        request.setParentId(parentId);
        request.setCreatedTime(LocalDateTime.now());

        PassportDto createdPassport = passportService
                .createPassportUsingDictionary(dictionary, request);

        List<String> uris = appProperties.getUriList();
        for (int index = 0; index < appProperties.getChildrenPerLevel(); index++) {
            int nextUriIndex = (uriIndex + index + 1) % uris.size();
            String nextUri = uris.get(nextUriIndex);
            createPassportRecursive(level + 1, nameSuffix + "." + (index + 1), nextUri,
                    nextUriIndex, createdPassport.getId(), userId);
        }
    }

    /**
     * Restricts the number of properties in the template and fills them with sample
     * values.
     *
     * @param template
     * @return Modified JSON node with restricted and populated properties.
     */
    private JsonNode filterAndFillProperties(JsonNode template) {
        if (!template.isObject()) {
            log.warn("Template node is not an object; skipping property filtering");
            return template;
        }
        if (!template.has("classProperties")) {
            return template;
        }

        List<JsonNode> properties = new ArrayList<>();
        template.get("classProperties").forEach(properties::add);

        List<JsonNode> selectedProperties = properties.stream()
                .limit(appProperties.getPropertyCountToSelect()).toList();

        ObjectNode objectNode = (ObjectNode) template;
        ArrayNode newProperties = objectMapper.createArrayNode();
        selectedProperties.forEach(newProperties::add);
        objectNode.set("classProperties", newProperties);

        for (JsonNode propertyNode : selectedProperties) {
            if (!propertyNode.isObject()) {
                log.debug("Skipping non-object property node in classProperties");
                continue;
            }
            ObjectNode propertyObject = (ObjectNode) propertyNode;
            if (propertyNode.has("allowedValues")
                    && propertyNode.get("allowedValues").isArray()
                    && propertyNode.get("allowedValues").size() > 0) {
                JsonNode first = propertyNode.get("allowedValues").get(0);
                String allowedValue = first.has("value") ? first.get("value").asText()
                        : first.asText();
                propertyObject.put("actualValue", allowedValue);
            } else if (propertyNode.has("dataType")) {
                String dataType = propertyNode.get("dataType").asText().toLowerCase();
                switch (dataType) {
                case "boolean" -> propertyObject.put("actualValue", RANDOM.nextBoolean());
                case "string" -> propertyObject.put("actualValue", "testData");
                case "real" -> propertyObject.put("actualValue", RANDOM.nextDouble());
                case "integer" -> propertyObject.put("actualValue", RANDOM.nextInt(50));
                case "datetime" -> propertyObject.put("actualValue",
                        OffsetDateTime.now(java.time.ZoneOffset.UTC)
                        .minusHours(RANDOM.nextInt(200))
                        .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                default -> {
                    log.debug("Unknown data type '{}' for property, using "
                            + "default test data", dataType);
                    propertyObject.put("actualValue", "testData");
                }
                }
            }
        }

        return template;
    }
}
