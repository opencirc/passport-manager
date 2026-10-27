package com.opencirc.api.passport.command;

import com.opencirc.api.passport.adapter.PlatformAdapter;
import com.opencirc.api.passport.adapter.PlatformAdapterFactory;
import com.opencirc.api.passport.enums.Platform;
import com.opencirc.api.passport.exception.JsonValidationException;
import com.opencirc.api.passport.service.PlatformService;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.CommandGroup;
import org.springframework.shell.core.command.annotation.Option;
import tools.jackson.databind.ObjectMapper;

/** Data Dictionary commands. */
@CommandGroup(name = "Data Dictionary commands")
public class DataDictionaryCommand {

  /** Injecting ObjectMapper. */
  private final ObjectMapper objectMapper;

  /** Injecting PlatformService. */
  private final PlatformService platformService;

  /** Injecting PlatformAdapterFactory. */
  private final PlatformAdapterFactory platformAdapterFactory;

  /** Instantiating DataDictionaryCommand. */
  public DataDictionaryCommand(
      ObjectMapper objectMapper,
      PlatformService platformService,
      PlatformAdapterFactory platformAdapterFactory) {
    this.objectMapper = objectMapper;
    this.platformService = platformService;
    this.platformAdapterFactory = platformAdapterFactory;
  }

  /** Shell command to fetch a template from the required data dictionary. */
  @Command(description = "Fetch template from Data Dictionary.")
  public String fetchTemplate(
      @Option(longName = "platform", required = true) String platform,
      @Option(longName = "code", required = true) String code,
      @Option(longName = "raw", defaultValue = "false") boolean raw) {

    try {
      if (raw) {
        return generateRawTemplate(Platform.fromValue(platform), code);
      } else {
        return generateDatasheetsFromPlatformId(Platform.fromValue(platform), code);
      }
    } catch (Exception e) {
      return "Error fetching template: " + e.getMessage();
    }
  }

  /** Gets the processed template. */
  private String generateDatasheetsFromPlatformId(Platform dictionaryPlatform, String platformId)
      throws JsonValidationException {

    var response = platformService.generateDatasheetsFromPlatformId(dictionaryPlatform, platformId);
    return formatJsonResponse(response);
  }

  /** Gets the raw template fetched from the given data dictionary. */
  private String generateRawTemplate(Platform dictionaryPlatform, String platformId) {
    PlatformAdapter adapter = platformAdapterFactory.getAdapter(dictionaryPlatform);
    return formatJsonResponse(adapter.fetchRawTemplate(platformId));
  }

  /** Formats the json. */
  private String formatJsonResponse(Object response) {
    return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
  }
}
