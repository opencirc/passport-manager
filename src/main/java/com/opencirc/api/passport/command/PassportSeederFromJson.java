package com.opencirc.api.passport.command;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.opencirc.api.passport.config.AppProperties;
import com.opencirc.api.passport.dao.UserRepository;
import com.opencirc.api.passport.dto.CreatePassportRequestDto;
import com.opencirc.api.passport.dto.PassportDto;
import com.opencirc.api.passport.enums.DataDictionary;
import com.opencirc.api.passport.model.Datasheet.DataCategory;
import com.opencirc.api.passport.model.User;
import com.opencirc.api.passport.service.PassportService;

import lombok.extern.slf4j.Slf4j;

/**
 * Seeder that creates passports from pre-filled templates stored in JSON file.
 */
@Component
@Slf4j
public class PassportSeederFromJson {

    /**
     * Application properties class.
     */
    private final AppProperties appProperties;

    /**
     * Service to get user details.
     */
    private final UserRepository userRepository;

    /**
     * Service responsible for creating passports.
     */
    private final PassportService passportService;

    /**
     * ObjectMapper.
     */
    private final ObjectMapper objectMapper;

    /**
     * Stores the templates.
     */
    private final Map<String, JsonNode> templatesByUri = new ConcurrentHashMap<>();

    /**
     * List to store the uri.
     */
    private List<String> uriList;

    /**
     * Constructor-based dependency injection.
     *
     * @param appProperties
     * @param userRepository
     * @param passportService
     * @param objectMapper
     */
    public PassportSeederFromJson(AppProperties appProperties,
                                  UserRepository userRepository,
                                  PassportService passportService,
                                  ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.userRepository = userRepository;
        this.passportService = passportService;
        this.objectMapper = objectMapper;
        loadTemplatesFromFile();
        this.uriList = List.copyOf(templatesByUri.keySet()); // Cache the URI list
    }

    /**
     * Load filled templates from resources/templates/bsdd_templates.json.
     */
    private void loadTemplatesFromFile() {
        try (InputStream is = getClass()
                .getResourceAsStream("/templates/bsdd_templates.json")) {
            if (is == null) {
                throw new IllegalStateException("Template file "
                        + "not found in resources/templates");
            }
            ArrayNode arrayNode = (ArrayNode) objectMapper.readTree(is);
            arrayNode.forEach(node -> templatesByUri.put(node
                    .get("uri").asText(), node.deepCopy()));
            log.info("Loaded {} templates from JSON file", templatesByUri.size());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load templates from JSON", e);
        }
    }

    /**
     * Seed passports using the loaded templates.
     */
    public void seed() {
        User user = userRepository.findAll().stream()
                    .findFirst()
                .orElseThrow(() -> new IllegalStateException("No users"
                        + " found in the database. Seed users first."));

        for (int i = 0; i < appProperties.getChildrenPerLevel(); i++) {
            String uri = uriList.get(i % uriList.size());
            createPassportRecursive(1, String.valueOf(i + 1),
                    uri, i, null, String.valueOf(user.getId()));
            }
            log.info("Passport seeding from JSON templates completed.");
    }

    private void createPassportRecursive(int level, String nameSuffix, String uri,
                                         int uriIndex, String parentId, String userId) {
        if (level > appProperties.getMaximumLevel()) {
            return;
        }

        JsonNode templateNode = templatesByUri.get(uri).deepCopy();

        CreatePassportRequestDto request = new CreatePassportRequestDto();
        request.setDatasheetData(templateNode);
        request.setDataCategory(DataCategory.GENERIC.getValue());
        request.setPassportName("Passport" + nameSuffix);
        request.setCreatedBy(userId);
        // TODO: Uncomment if parentId is used
        // request.setParentId(parentId);
        request.setCreatedTime(LocalDateTime.now());

        PassportDto createdPassport = passportService
                .createPassportUsingDictionary(DataDictionary.BSDD, request);

        // Recursively create child passports
        for (int i = 0; i < appProperties.getChildrenPerLevel(); i++) {
            int nextUriIndex = (uriIndex + i + 1) % uriList.size();
            createPassportRecursive(level + 1, nameSuffix + "." + (i + 1),
                    uriList.get(nextUriIndex),
                    nextUriIndex, createdPassport.getId(), userId);
        }
    }


}
