package com.opencirc.api.passport.command;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
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
public class PassportSeeder {

    /**
     * Injecting Properties class.
     */
    @Autowired
    private AppProperties appProperties;

    /**
     * Injecting UserRepository.
     */
    @Autowired
    private UserRepository userRepository;

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
     * List of URIs representing classes in the data dictionary.
     */
    private final List<String> uriList = Arrays.asList(
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EG000001",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000003",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000007",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000017",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000018",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000019",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000008",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000020",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000022",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000023",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000009",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000024",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000025",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000026",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000005",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000010",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000028",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000029",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000030",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000011",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000032",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000033",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000034",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000012",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000036",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000037",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000038",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000006",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000013",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000040",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000041",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000014",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000042",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000044",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000045",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000016",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000046",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000047",
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0/class/EC000038"

    );

    /**
     * Constructor-based dependency injection.
     *
     * @param dataDictionaryServiceParam
     * @param passportServiceParam
     * @param objectMapperParam
     */
    public PassportSeeder(DataDictionaryService dataDictionaryServiceParam,
            PassportService passportServiceParam, ObjectMapper objectMapperParam) {
        this.dataDictionaryService = dataDictionaryServiceParam;
        this.passportService = passportServiceParam;
        this.objectMapper = objectMapperParam;
    }

    /**
     * Seeds sample passports recursively using the predefined URI list.
     *
     * @throws RuntimeException if passport seeding fails.
     */
    public void seed() {
        try {

            //Will be updated when task (Remove username from OpenCirc) is implemented
            User user = userRepository.findByUsername("user@test.com");
            if (user == null) {
                throw new IllegalStateException("Seed user 'user@test.com' not found.");
            }
            for (int index = 0; index < appProperties.getChildrenPerLevel(); index++) {
                String uri = uriList.get(index % uriList.size());
                createPassportRecursive(1, String.valueOf(index + 1), uri, index, null,
                        String.valueOf(user.getId()));
            }
            log.info("Passport seeding completed.");
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
     * @throws Exception if passport creation fails.
     */
    private void createPassportRecursive(int level, String nameSuffix, String uri,
            int uriIndex, String parentId, String userId) throws Exception {
        if (level > appProperties.getMaximumLevel()) {
            return;
        }

        JsonNode templateNode = templateCache.computeIfAbsent(uri, currentUri -> {
            Object template = null;
            try {
                template = dataDictionaryService
                        .createClassTemplate(dictionary, currentUri, true);
            } catch (JsonProcessingException | JsonValidationException e) {
                throw new RuntimeException("Failed to create template for URI: " + currentUri, e);
            }
            return objectMapper.valueToTree(template);
        }).deepCopy();
        JsonNode datasheetData = filterAndFillProperties(
                objectMapper.valueToTree(templateNode));

        CreatePassportRequestDto request = new CreatePassportRequestDto();
        request.setDatasheetData(datasheetData);
        request.setDataCategory(DataCategory.GENERIC.getValue());
        request.setPassportName("Passport" + nameSuffix);
        request.setCreatedBy(userId);
        request.setCreatedTime(LocalDateTime.now());

        PassportDto createdPassport = passportService
                .createPassportUsingDictionary(dictionary, request);

        for (int index = 0; index < appProperties.getChildrenPerLevel(); index++) {
            int nextUriIndex = (uriIndex + index + 1) % uriList.size();
            String nextUri = uriList.get(nextUriIndex);
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
        if (!template.has("classProperties")) {
            return template;
        }

        List<JsonNode> props = new ArrayList<>();
        template.get("classProperties").forEach(props::add);

        List<JsonNode> selectedProperties = props.stream().limit(appProperties
                .getPropertyCountToSelect()).toList();

        ObjectNode objectNode = (ObjectNode) template;
        ArrayNode newProperty = objectMapper.createArrayNode();
        selectedProperties.forEach(newProperty::add);
        objectNode.set("classProperties", newProperty);

        for (JsonNode propertyNode : selectedProperties) {
            ObjectNode propertyObject = (ObjectNode) propertyNode;
            if (propertyNode.has("allowedValues")
                    && propertyNode.get("allowedValues").isArray()
                    && propertyNode.get("allowedValues").size() > 0) {
                JsonNode first = propertyNode.get("allowedValues").get(0);
                String allowedValue = first.has("value") ? first
                        .get("value").asText() : first.asText();
                propertyObject.put("actualValue", allowedValue);
            } else if (propertyNode.has("dataType")) {
                String dataType = propertyNode.get("dataType").asText().toLowerCase();
                switch (dataType) {
                case "boolean" -> propertyObject.put("actualValue", RANDOM.nextBoolean());
                case "string" -> propertyObject.put("actualValue", "testData");
                case "real" -> propertyObject.put("actualValue", RANDOM.nextDouble());
                case "integer" -> propertyObject.put("actualValue", RANDOM.nextInt(50));
                case "datetime" -> propertyObject.put("actualValue",
                        LocalDateTime.now().minusHours(RANDOM.nextInt(200))
                        .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                default -> propertyObject.put("actualValue", "testData");
                }
            }
        }

        return template;
    }
}
