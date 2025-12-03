package com.opencirc.api.passport.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencirc.api.passport.adapter.PlatformAdapter;
import com.opencirc.api.passport.adapter.PlatformAdapterFactory;
import com.opencirc.api.passport.enums.Platform;
import com.opencirc.api.passport.exception.JsonValidationException;
import com.opencirc.api.passport.service.PlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

@Command(group = "Data Dictionary Commands")
public class DataDictionaryCommands {

  /** Injecting ObjectMapper. */
  private final ObjectMapper objectMapper;

  /** Injecting PlatformService. */
  private final PlatformService platformService;

  /** Injecting PlatformAdapterFactory. */
  private final PlatformAdapterFactory platformAdapterFactory;

  /** Instantiating DataDictionaryCommands. */
  @Autowired
  public DataDictionaryCommands(
      ObjectMapper objectMapper,
      PlatformService platformService,
      PlatformAdapterFactory platformAdapterFactory) {
    this.objectMapper = objectMapper;
    this.platformService = platformService;
    this.platformAdapterFactory = platformAdapterFactory;
  }

  /** Shell command to fetch template from the required data dictionary. */
  @Command(description = "Fetch template from Data Dictionary.")
  public String fetchTemplate(
      @Option(longNames = "platform", required = true) String platform,
      @Option(longNames = "code", required = true) String code,
      @Option(longNames = "raw", defaultValue = "false") boolean raw) {

    try {
      if (raw) {
        return generateRawTemplate(Platform.fromValue(platform), code);
      } else {
        return generateDatasheetFromPlatformId(Platform.fromValue(platform), code);
      }
    } catch (Exception e) {
      return "Error fetching template: " + e.getMessage();
    }
  }

  /** Gets the processed template. */
  private String generateDatasheetFromPlatformId(Platform dictionaryPlatform, String code)
      throws JsonValidationException, JsonProcessingException {

    Object response;
    response = platformService.generateDatasheetFromPlatformId(dictionaryPlatform, code);
    return formatJsonResponse(response);
  }

  /** Gets the raw template fetched from the given data dictionary. */
  private String generateRawTemplate(Platform dictionaryPlatform, String code)
      throws JsonProcessingException {
    PlatformAdapter adapter = platformAdapterFactory.getAdapter(dictionaryPlatform);
    return formatJsonResponse(adapter.fetchRawTemplate(code));
  }

  /** Formats the json. */
  private String formatJsonResponse(Object response) throws JsonProcessingException {
    return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
  }
}
