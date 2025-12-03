package com.opencirc.api.passport.adapter;

import com.opencirc.api.passport.adapter.bsdd.BsddPlatformAdapter;
import com.opencirc.api.passport.enums.Platform;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Factory class for providing the appropriate implementation based on the specified dictionary
 * library.
 */
@Component
public class PlatformAdapterFactory {

  /** Map that has all the map name and its instances. */
  private final Map<Platform, PlatformAdapter> adapterMap;

  /** Instantiates PlatformAdapterFactory. */
  public PlatformAdapterFactory(List<PlatformAdapter> adapters) {
    adapterMap = new HashMap<>();
    adapterMap.put(Platform.BSDD, findAdapter(adapters, BsddPlatformAdapter.class));
  }

  /** Returns the appropriate instance based on the given dictionary name. */
  @SuppressWarnings("unchecked")
  public PlatformAdapter getAdapter(Platform dictionaryPlatform) {
    PlatformAdapter adapter = adapterMap.get(dictionaryPlatform);
    if (adapter == null) {
      throw new UnsupportedOperationException(
          "No adapter found for dictionary: " + dictionaryPlatform);
    }
    return adapter;
  }

  private <T extends PlatformAdapter> T findAdapter(
      List<PlatformAdapter> adapters, Class<T> genericClass) {
    return adapters.stream()
        .filter(genericClass::isInstance)
        .map(genericClass::cast)
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException("Adapter not found: " + genericClass.getSimpleName()));
  }
}
