package com.opencirc.api.passport.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.opencirc.api.passport.dao.UserRepository;
import com.opencirc.api.passport.dto.CreatePassportRequestDto;
import com.opencirc.api.passport.dto.UserDto;
import com.opencirc.api.passport.enums.Platform;
import com.opencirc.api.passport.exception.JsonValidationException;
import com.opencirc.api.passport.model.Datasheet;
import com.opencirc.api.passport.model.User;
import com.opencirc.api.passport.service.PassportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.stereotype.Component;

/** Passport commands entry point. */
@Component
@Command(group = "Passport commands")
@Slf4j
public class PassportCommand {
  private final PassportService passportService;

  private final UserRepository userRepository;

  /** PassportCommand. */
  @Autowired
  public PassportCommand(PassportService passportService, UserRepository userRepository) {
    this.passportService = passportService;
    this.userRepository = userRepository;
  }

  /** Shell command to create a passport using a platform. */
  @Command(description = "Create passport using platform.")
  public String createPassportUsingPlatform(
      @Option(longNames = "userId", required = true) String userId,
      @Option(longNames = "platform", required = true) String platform,
      @Option(longNames = "platformId", required = true) String platformId,
      @Option(longNames = "name", required = true) String name,
      @Option(longNames = "dataCategory", defaultValue = "generic") String dataCategory)
      throws JsonValidationException, JsonProcessingException {

    CreatePassportRequestDto data = new CreatePassportRequestDto();
    data.setPlatformId(platformId);
    data.setName(name);
    data.setDataCategory(Datasheet.DataCategory.fromValue(dataCategory).getValue());
    User user = userRepository.findById(userId).orElseThrow();
    return passportService
        .createPassportUsingPlatform(Platform.fromValue(platform), data, UserDto.from(user))
        .getId();
  }
}
