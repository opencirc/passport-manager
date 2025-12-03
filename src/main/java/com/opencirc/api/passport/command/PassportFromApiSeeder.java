package com.opencirc.api.passport.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.opencirc.api.passport.config.AppProperties;
import com.opencirc.api.passport.dao.UserRepository;
import com.opencirc.api.passport.dto.*;
import com.opencirc.api.passport.enums.Platform;
import com.opencirc.api.passport.exception.JsonValidationException;
import com.opencirc.api.passport.model.Datasheet.DataCategory;
import com.opencirc.api.passport.model.User;
import com.opencirc.api.passport.service.PassportService;
import com.opencirc.api.passport.service.PlatformService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Seeder for generating sample passports based on predefined URIs and data dictionary templates.
 */
@Component
@Slf4j
public class PassportFromApiSeeder {
  private final AppProperties appProperties;
  private final UserRepository userRepository;
  private final PassportService passportService;
  private final Platform platform = Platform.BSDD;

  /** Constructor-based dependency injection. */
  public PassportFromApiSeeder(
      PlatformService platformService,
      PassportService passportService,
      AppProperties appProperties,
      UserRepository userRepository) {
    this.passportService = passportService;
    this.appProperties = appProperties;
    this.userRepository = userRepository;
  }

  /** Seeds sample passports recursively using the predefined URI list. */
  public void seed() {
    try {

      User user =
          userRepository
              .findFirstByOrderByIdAsc()
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "No users found in the database. "
                              + "Please seed users before running passport seeding."));

      List<String> uris = appProperties.getUriList();
      if (uris == null || uris.isEmpty()) {
        throw new IllegalStateException("URI list cannot be empty for passport seeding");
      }
      for (int index = 0; index < appProperties.getChildrenPerLevel(); index++) {
        String uri = uris.get(index % uris.size());
        createPassportRecursive(1, String.valueOf(index + 1), uri, index, null, UserDto.from(user));
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

  /** Recursively creates passports and their child passports. */
  private void createPassportRecursive(
      int level, String nameSuffix, String uri, int uriIndex, String parentId, UserDto author)
      throws JsonValidationException, JsonProcessingException {
    if (level > appProperties.getMaximumLevel()) {
      return;
    }

    CreatePassportRequestDto request = new CreatePassportRequestDto();
    request.setPlatformId(uri);
    request.setDataCategory(DataCategory.GENERIC.getValue());
    request.setName("Passport" + nameSuffix);
    request.setParentId(parentId);

    PassportDto createdPassport =
        passportService.createPassportUsingPlatform(platform, request, author);

    List<String> uris = appProperties.getUriList();
    for (int index = 0; index < appProperties.getChildrenPerLevel(); index++) {
      int nextUriIndex = (uriIndex + index + 1) % uris.size();
      String nextUri = uris.get(nextUriIndex);
      createPassportRecursive(
          level + 1,
          nameSuffix + "." + (index + 1),
          nextUri,
          nextUriIndex,
          createdPassport.getId(),
          author);
    }
  }
}
