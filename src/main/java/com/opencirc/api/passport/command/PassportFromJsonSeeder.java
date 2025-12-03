package com.opencirc.api.passport.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencirc.api.passport.config.AppProperties;
import com.opencirc.api.passport.dao.UserRepository;
import com.opencirc.api.passport.dto.*;
import com.opencirc.api.passport.enums.DataDictionary;
import com.opencirc.api.passport.enums.Platform;
import com.opencirc.api.passport.exception.JsonValidationException;
import com.opencirc.api.passport.model.Datasheet.DataCategory;
import com.opencirc.api.passport.model.User;
import com.opencirc.api.passport.service.PassportService;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Seeder that creates passports from pre-filled templates stored in JSON file. */
@Component
@Slf4j
public class PassportFromJsonSeeder {

  /** Application properties class. */
  private final AppProperties appProperties;

  /** Service to get user details. */
  private final UserRepository userRepository;

  /** Service responsible for creating passports. */
  private final PassportService passportService;

  /** ObjectMapper. */
  private final ObjectMapper objectMapper;

  /** Stores the templates. */
  private final Map<String, BsddClassTemplateDto> templatesByUri = new ConcurrentHashMap<>();

  /** The platform in which data dictionary is present. */
  private final Platform platform = Platform.BSDD;

  /** The data dictionary used for seeding passport templates. */
  private final DataDictionary dictionary = DataDictionary.IFC;

  /** List to store the uri. */
  private List<String> uriList;

  /** Constructor-based dependency injection. */
  public PassportFromJsonSeeder(
      AppProperties appProperties,
      UserRepository userRepository,
      PassportService passportService,
      ObjectMapper objectMapper) {
    this.appProperties = appProperties;
    this.userRepository = userRepository;
    this.passportService = passportService;
    this.objectMapper = objectMapper;
    loadTemplatesFromFile();
    this.uriList = templatesByUri.keySet().stream().sorted().toList();
  }

  /** Load filled templates from resources/templates/bsdd_templates.json. */
  private void loadTemplatesFromFile() {
    final String path = appProperties.getTemplatePath();
    try (InputStream is = getClass().getResourceAsStream(path)) {
      if (is == null) {
        throw new IllegalStateException("Template file not found at classpath location: " + path);
      }
      JsonNode root = objectMapper.readTree(is);
      if (root == null || !root.isArray()) {
        throw new IllegalStateException(
            "Template file at " + path + " must contain a JSON array of templates");
      }
      for (JsonNode node : root) {
        JsonNode uriNode = node.get("uri");
        if (uriNode != null && !uriNode.isNull()) {
          BsddClassTemplateDto template =
              objectMapper.treeToValue(node, BsddClassTemplateDto.class);
          templatesByUri.put(uriNode.asText(), template);
        } else {
          log.warn("Skipping template without 'uri': {}", node);
        }
      }
      log.info("Loaded {} templates from JSON file", templatesByUri.size());
    } catch (Exception e) {
      throw new RuntimeException("Failed to load templates from JSON", e);
    }
  }

  /** Seed passports using the loaded templates. */
  public void seed() throws JsonValidationException, JsonProcessingException {
    User user =
        userRepository
            .findFirstByOrderByIdAsc()
            .orElseThrow(
                () ->
                    new IllegalStateException("No users found in the database. Seed users first."));

    CreatedByDto createdByDto = CreatedByDto.from(user);

    if (uriList == null || uriList.isEmpty()) {
      throw new IllegalStateException(
          "No templates loaded; ensure template file at "
              + appProperties.getTemplatePath()
              + " is present on the classpath and non-empty.");
    }

    for (int i = 0; i < appProperties.getChildrenPerLevel(); i++) {
      String uri = uriList.get(i % uriList.size());
      createPassportRecursive(1, String.valueOf(i + 1), uri, i, null, UserDto.from(user));
    }
    log.info("Passport seeding from JSON templates completed.");
  }

  /** Recursively creates passports and their child passports. */
  private void createPassportRecursive(
      int level, String nameSuffix, String uri, int uriIndex, String parentId, UserDto author)
      throws JsonValidationException, JsonProcessingException {
    if (level > appProperties.getMaximumLevel()) {
      return;
    }

    BsddClassTemplateDto template = templatesByUri.get(uri);
    if (template == null) {
      throw new IllegalStateException("No template found for URI: " + uri);
    }

    CreatePassportRequestDto request = new CreatePassportRequestDto();
    request.setPlatformId(uri);
    request.setDataCategory(DataCategory.GENERIC.getValue());
    request.setName("Passport" + nameSuffix);
    request.setParentId(parentId);
    PassportDto createdPassport =
        passportService.createPassportUsingPlatform(platform, request, author);

    // Recursively create child passports
    for (int i = 0; i < appProperties.getChildrenPerLevel(); i++) {
      int nextUriIndex = (uriIndex + i + 1) % uriList.size();
      createPassportRecursive(
          level + 1,
          nameSuffix + "." + (i + 1),
          uriList.get(nextUriIndex),
          nextUriIndex,
          createdPassport.getId(),
          author);
    }
  }
}
