package com.opencirc.api.passport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.opencirc.api.passport.adapter.PlatformAdapter;
import com.opencirc.api.passport.adapter.PlatformAdapterFactory;
import com.opencirc.api.passport.enums.Platform;
import com.opencirc.api.passport.exception.JsonValidationException;
import com.opencirc.api.passport.model.Datasheet;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Platform service class for data dictionary operations. */
@Service
public class PlatformService {

  @Autowired private PlatformAdapterFactory platformAdapterFactory;

  /** Search and retrieves the class based on the text. */
  public List<Map<String, String>> searchClassesByText(Platform platform, String text) {
    PlatformAdapter adapter = platformAdapterFactory.getAdapter(platform);
    return adapter.listClass(text);
  }

  /** Search and retrieves the class along with the properties. */
  public Datasheet generateDatasheetFromPlatformId(Platform platform, String code)
      throws JsonValidationException, JsonProcessingException {
    var adapter = platformAdapterFactory.getAdapter(platform);
    return adapter.generateDatasheetFromPlatformId(code);
  }

  /** Retrieves the list of properties. */
  public List<Map<String, String>> listProperties(Platform dictionary, String text) {
    PlatformAdapter adapter = platformAdapterFactory.getAdapter(dictionary);
    return adapter.listProperties(text);
  }
}
