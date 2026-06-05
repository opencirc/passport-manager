package com.opencirc.api.passport.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.opencirc.api.passport.dto.DataDictionaryTreeStructureDto;
import com.opencirc.api.passport.enums.DataDictionary;
import com.opencirc.api.passport.exception.InvalidDataDictionaryException;
import com.opencirc.api.passport.exception.JsonValidationException;
import com.opencirc.api.passport.model.DatasheetDefinition;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Interface of Dictionary Adapter. */
public interface PlatformAdapter {

  /** Retrieves a list of classes matching the given search text. */
  List<Map<String, String>> listClass(String text);

  /*
   * @TODO this is not abstract enough and will be left here for now for testing purposes,
   *   but this is not a good implementation.
   */
  /**
   * Generates the datasheet definition(s) for the given platform ID. Element 0 is the definition
   * for the URI itself; when {@code addRelatedIfcEntities} is true, related IFC entity definitions
   * are appended and their URIs recorded on the main definition's {@code relatedPlatformIds}.
   */
  List<DatasheetDefinition> generateDatasheetsFromPlatformId(
      String platformId, boolean addRelatedIfcEntities)
      throws JsonValidationException, JsonProcessingException;

  /** Fetches and builds the datasheet definition for a single URI. */
  DatasheetDefinition generateDatasheetFromPlatformId(String uri) throws JsonValidationException;

  /** Retrieves a list of properties matching the given search text. */
  List<Map<String, String>> listProperties(String text);

  /** Validates the given JSON template. */
  String validatePassportData(JsonNode jsonNode) throws JsonValidationException;

  /** Displays the template from the dictionary without any processing. */
  JsonNode fetchRawTemplate(String uri) throws JsonProcessingException;

  /** Retrieves the tree structure of the dictionary. */
  List<DataDictionaryTreeStructureDto> getDictionaryTreeStructure(DataDictionary dictionary)
      throws IOException, InvalidDataDictionaryException;
}
