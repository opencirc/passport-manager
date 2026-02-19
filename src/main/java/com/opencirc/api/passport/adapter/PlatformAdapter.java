package com.opencirc.api.passport.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.opencirc.api.passport.dto.DataDictionaryTreeStructureDto;
import com.opencirc.api.passport.enums.DataDictionary;
import com.opencirc.api.passport.exception.InvalidDataDictionaryException;
import com.opencirc.api.passport.exception.JsonValidationException;
import com.opencirc.api.passport.model.Datasheet;
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
   * Generates a set of datasheets using the given platform ID. We can tell the service whether we
   * are invoking this method directly from an endpoint or as part of another flow, because the
   * different scenarios affect the way the datasheet is created.
   */
  List<Datasheet> generateDatasheetsFromPlatformId(String platformId, boolean invokedExternally)
      throws JsonValidationException, JsonProcessingException;

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
