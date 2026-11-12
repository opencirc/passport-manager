package com.opencirc.api.passport.command;

import com.opencirc.api.passport.auth.principal.ApiKeyPrincipal;
import com.opencirc.api.passport.dto.CreatePassportUsingPlatformRequestDto;
import com.opencirc.api.passport.enums.Platform;
import com.opencirc.api.passport.exception.JsonValidationException;
import com.opencirc.api.passport.model.Datasheet;
import com.opencirc.api.passport.service.ApiKeyService;
import com.opencirc.api.passport.service.PassportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.CommandGroup;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

/** Passport commands entry point. Passports are created on behalf of an API key. */
@Component
@CommandGroup(name = "Passport commands")
@Slf4j
public class PassportCommand {
  private final PassportService passportService;

  private final ApiKeyService apiKeyService;

  /** PassportCommand. */
  @Autowired
  public PassportCommand(PassportService passportService, ApiKeyService apiKeyService) {
    this.passportService = passportService;
    this.apiKeyService = apiKeyService;
  }

  /** Shell command to create a child passport using a platform, acting as the given API key. */
  @Command(description = "Create a child passport using platform, acting as an API key.")
  public String createPassportUsingPlatform(
      @Option(longName = "apiKeyId", required = true) String apiKeyId,
      @Option(longName = "parentId", required = true) String parentId,
      @Option(longName = "platform", required = true) String platform,
      @Option(longName = "platformId", required = true) String platformId,
      @Option(longName = "name", required = true) String name,
      @Option(longName = "dataCategory", defaultValue = "generic") String dataCategory)
      throws JsonValidationException {

    CreatePassportUsingPlatformRequestDto data = new CreatePassportUsingPlatformRequestDto();
    data.setPlatformId(platformId);
    data.setName(name);
    data.setParentId(parentId);
    data.setDataCategory(Datasheet.DataCategory.fromValue(dataCategory).getValue());
    ApiKeyPrincipal actor = apiKeyService.actorFor(apiKeyId);
    return passportService
        .createPassportUsingPlatform(Platform.fromValue(platform), data, actor)
        .getId();
  }
}
