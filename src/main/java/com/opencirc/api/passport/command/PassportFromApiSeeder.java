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
import com.opencirc.api.passport.dto.BsddClassTemplateDto;
import com.opencirc.api.passport.dto.CreatePassportRequestDto;
import com.opencirc.api.passport.dto.CreatedByDto;
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
public class PassportFromApiSeeder {

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
    private final Map<String, BsddClassTemplateDto> templateCache
    = new ConcurrentHashMap<>();

    /**
     * Constructor-based dependency injection.
     *
     * @param dataDictionaryService
     * @param passportService
     * @param objectMapper
     * @param appProperties
     * @param userRepository
     */
    public PassportFromApiSeeder(DataDictionaryService dataDictionaryService,
            PassportService passportService, ObjectMapper objectMapper,
            AppProperties appProperties, UserRepository userRepository) {
        this.dataDictionaryService = dataDictionaryService;
        this.passportService = passportService;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
        this.userRepository = userRepository;
    }

    /**
     * Seeds sample passports recursively using the predefined URI list.
     *
     * @throws RuntimeException if passport seeding fails.
     */
    public void seed() {
        try {

            User user = userRepository.findFirstByOrderByIdAsc().orElseThrow(
                    () -> new IllegalStateException("No users found in the database. "
                            + "Please seed users before running passport seeding."));

            CreatedByDto createdByDto = CreatedByDto.fromUser(user);
            List<String> uris = appProperties.getUriList();
            if (uris == null || uris.isEmpty()) {
                throw new IllegalStateException(
                        "URI list cannot be empty for passport seeding");
            }
            for (int index = 0; index < appProperties.getChildrenPerLevel(); index++) {
                String uri = uris.get(index % uris.size());
                createPassportRecursive(1, String.valueOf(index + 1), uri, index, null,
                        String.valueOf(user.getId()), createdByDto);
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
     * @param createdByDto
     */
    private void createPassportRecursive(int level, String nameSuffix, String uri,
            int uriIndex, String parentId, String userId, CreatedByDto createdByDto) {
        if (level > appProperties.getMaximumLevel()) {
            return;
        }

        BsddClassTemplateDto templateNode = templateCache.computeIfAbsent(uri,
                currentUri -> {
                    try {
                        return dataDictionaryService.createClassTemplate(dictionary,
                                currentUri, true);
                    } catch (JsonProcessingException | JsonValidationException e) {
                        throw new RuntimeException(
                                "Failed to create template for URI: " + currentUri, e);
                    }
                });
        filterAndFillProperties(templateNode);

        CreatePassportRequestDto request = new CreatePassportRequestDto();
        request.setDatasheetData(objectMapper.valueToTree(templateNode));
        request.setDataCategory(DataCategory.GENERIC.getValue());
        request.setPassportName("Passport" + nameSuffix);
        request.setCreatedById(userId);
        request.setCreatedBy(createdByDto);
        request.setParentId(parentId);

        request.setCreatedTime(LocalDateTime.now());

        PassportDto createdPassport = passportService
                .createPassportUsingDictionary(dictionary, request);

        List<String> uris = appProperties.getUriList();
        for (int index = 0; index < appProperties.getChildrenPerLevel(); index++) {
            int nextUriIndex = (uriIndex + index + 1) % uris.size();
            String nextUri = uris.get(nextUriIndex);
            createPassportRecursive(level + 1, nameSuffix + "." + (index + 1), nextUri,
                    nextUriIndex, createdPassport.getId(), userId, createdByDto);
        }
    }

    /**
     * Restricts the number of properties in the template and fills them with sample
     * values.
     *
     * @param template
     */
    private void filterAndFillProperties(BsddClassTemplateDto template) {

        if (template.getClassProperties() == null) {
            return;
        }

        List<JsonNode> properties = new ArrayList<>();
        template.getClassProperties().forEach(properties::add);

        List<JsonNode> selectedProperties = properties.stream()
                .limit(appProperties.getPropertyCountToSelect()).toList();

        ArrayNode newProperties = objectMapper.createArrayNode();
        selectedProperties.forEach(newProperties::add);
        template.setClassProperties(newProperties);

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
                case "datetime" -> propertyObject.put("actualValue", OffsetDateTime
                        .now(java.time.ZoneOffset.UTC).minusHours(RANDOM.nextInt(200))
                        .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                default -> {
                    log.debug("Unknown data type '{}' for property, using "
                            + "default test data", dataType);
                    propertyObject.put("actualValue", "testData");
                }
                }
            }
        }
    }
}
