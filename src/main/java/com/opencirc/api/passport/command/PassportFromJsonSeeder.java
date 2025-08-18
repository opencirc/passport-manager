package com.opencirc.api.passport.command;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class PassportFromJsonSeeder {

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
    public PassportFromJsonSeeder(AppProperties appProperties,
                                  UserRepository userRepository,
                                  PassportService passportService,
                                  ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.userRepository = userRepository;
        this.passportService = passportService;
        this.objectMapper = objectMapper;
        loadTemplatesFromFile();
        this.uriList = templatesByUri.keySet().stream().sorted()
                .toList();
    }

    /**
     * Load filled templates from resources/templates/bsdd_templates.json.
     */
    private void loadTemplatesFromFile() {
        final String path = appProperties.getTemplatePath();
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalStateException(
                        "Template file not found at classpath location: " + path);
            }
            JsonNode root = objectMapper.readTree(is);
            if (root == null || !root.isArray()) {
                throw new IllegalStateException("Template file at " + path
                        + " must contain a JSON array of templates");
            }
            for (JsonNode node : root) {
                JsonNode uriNode = node.get("uri");
                if (uriNode != null && !uriNode.isNull()) {
                    templatesByUri.put(uriNode.asText(), node.deepCopy());
                } else {
                    log.warn("Skipping template without 'uri': {}", node);
                }
            }
            log.info("Loaded {} templates from JSON file", templatesByUri.size());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load templates from JSON", e);
        }
    }

    /**
     * Seed passports using the loaded templates.
     */
    public void seed() {
        User user = userRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException(
                        "No users" + " found in the database. Seed users first."));

        if (uriList == null || uriList.isEmpty()) {
            throw new IllegalStateException(
                    "No templates loaded; ensure template file at "
                            + appProperties.getTemplatePath()
                            + " is present on the classpath and non-empty.");
        }

        for (int i = 0; i < appProperties.getChildrenPerLevel(); i++) {
            String uri = uriList.get(i % uriList.size());
            createPassportRecursive(1, String.valueOf(i + 1), uri, i, null,
                    String.valueOf(user.getId()));
        }
        log.info("Passport seeding from JSON templates completed.");
    }

    private void createPassportRecursive(int level, String nameSuffix, String uri,
            int uriIndex, String parentId, String userId) {
        if (level > appProperties.getMaximumLevel()) {
            return;
        }

        JsonNode template = templatesByUri.get(uri);
        if (template == null) {
            throw new IllegalStateException("No template found for URI: " + uri);
        }
        JsonNode templateNode = template.deepCopy();

        CreatePassportRequestDto request = new CreatePassportRequestDto();
        request.setDatasheetData(templateNode);
        request.setDataCategory(DataCategory.GENERIC.getValue());
        request.setPassportName("Passport" + nameSuffix);
        request.setCreatedBy(userId);
        request.setParentId(parentId);
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
