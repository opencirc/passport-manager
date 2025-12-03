package com.opencirc.api.passport.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencirc.api.passport.adapter.DictionaryAdapterFactory;
import com.opencirc.api.passport.adapter.PlatformAdapter;
import com.opencirc.api.passport.enums.Platform;
import com.opencirc.api.passport.exception.JsonValidationException;
import com.opencirc.api.passport.service.DataDictionaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

@Command(group = "Data Dictionary Commands")
public class DataDictionaryCommands {

  /** Injecting ObjectMapper. */
  private final ObjectMapper objectMapper;

  /** Injecting DataDictionaryService. */
  private final DataDictionaryService dataDictionaryService;

  /** Injecting DictionaryAdapterFactory. */
  private final DictionaryAdapterFactory dictionaryAdapterFactory;

  /**
   * Instantiating DataDictionaryCommands.
   *
   * @param objectMapper
   * @param dataDictionaryService
   * @param dictionaryAdapterFactory
   */
  @Autowired
  public DataDictionaryCommands(
      ObjectMapper objectMapper,
      DataDictionaryService dataDictionaryService,
      DictionaryAdapterFactory dictionaryAdapterFactory) {
    this.objectMapper = objectMapper;
    this.dataDictionaryService = dataDictionaryService;
    this.dictionaryAdapterFactory = dictionaryAdapterFactory;
  }

  /**
   * Shell command to fetch template from the required data dictionary.
   *
   * @param dictionaryPlatform
   * @param type
   * @param uri
   * @param raw
   * @return template
   */
  @Command(description = "Fetch template from Data Dictionary.")
  public String fetchTemplate(
      @Option(longNames = "dictionaryType", required = true) String dictionaryPlatform,
      @Option(longNames = "type", required = true) String type,
      @Option(longNames = "uri", required = true) String uri,
      @Option(longNames = "raw", defaultValue = "false") boolean raw) {

    try {
      if (raw) {
        return generateRawTemplate(Platform.fromValue(dictionaryPlatform), uri, type);
      } else {
        return generateProcessedTemplate(Platform.fromValue(dictionaryPlatform), uri, type);
      }
    } catch (Exception e) {
      return "Error fetching template: " + e.getMessage();
    }
  }

  /**
   * Gets the processed template.
   *
   * @param dictionaryPlatform
   * @param type
   * @param uri
   * @return template
   */
  private String generateProcessedTemplate(Platform dictionaryPlatform, String uri, String type)
      throws JsonValidationException, JsonProcessingException {

    Object response = null;
    response = dataDictionaryService.createClassTemplate(dictionaryPlatform, uri, true);
    return formatJsonResponse(response);
  }

  /**
   * Gets the raw template fetched from given data dictionary.
   *
   * @param dictionaryPlatform
   * @param type
   * @param uri
   * @return template
   */
  private String generateRawTemplate(Platform dictionaryPlatform, String uri, String type)
      throws JsonProcessingException {
    PlatformAdapter<?> adapter = dictionaryAdapterFactory.getAdapter(dictionaryPlatform);
    return formatJsonResponse(adapter.fetchRawTemplate(uri, type));
  }

  /**
   * Formats the json.
   *
   * @param response string
   * @return formatted template
   */
  private String formatJsonResponse(Object response) throws JsonProcessingException {
    return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
  }
}
