package com.opencirc.api.passport.adapter;

import com.opencirc.api.passport.dto.DataDictionaryTreeStructureDto;
import com.opencirc.api.passport.enums.DataDictionary;
import com.opencirc.api.passport.exception.InvalidDataDictionaryException;
import com.opencirc.api.passport.exception.JsonValidationException;
import com.opencirc.api.passport.model.DatasheetDefinition;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;

/** Interface of Dictionary Adapter. */
public interface PlatformAdapter {

  /** Retrieves a list of classes matching the given search text. */
  List<Map<String, String>> listClass(String text);

  /*
   * @TODO this is not abstract enough and will be left here for now for testing purposes,
   *   but this is not a good implementation.
   */
  /**
   * Fetches the datasheet definition(s) for the given platform ID. Element 0 is the definition for
   * the URI itself; when {@code addRelatedIfcEntities} is true, the definitions of its related IFC
   * entities are appended.
   */
  List<DatasheetDefinition> generateDatasheetsFromPlatformId(
      String platformId, boolean addRelatedIfcEntities) throws JsonValidationException;

  /**
   * Fetches and builds the datasheet definition for a single URI. The URIs of related IFC entities
   * are recorded on the definition but not fetched.
   */
  DatasheetDefinition generateDatasheetFromPlatformId(String platformId)
      throws JsonValidationException;

  /** Retrieves a list of properties matching the given search text. */
  List<Map<String, String>> listProperties(String text);

  /** Validates the given JSON template. */
  String validatePassportData(JsonNode jsonNode) throws JsonValidationException;

  /** Displays the template from the dictionary without any processing. */
  JsonNode fetchRawTemplate(String uri);

  /** Retrieves the tree structure of the dictionary. */
  List<DataDictionaryTreeStructureDto> getDictionaryTreeStructure(DataDictionary dictionary)
      throws IOException, InvalidDataDictionaryException;
}
