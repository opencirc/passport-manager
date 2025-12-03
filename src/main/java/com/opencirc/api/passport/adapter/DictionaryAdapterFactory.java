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
public class DictionaryAdapterFactory {

  /** Map that has all the map name and its instances. */
  private final Map<Platform, PlatformAdapter<?>> adapterMap;

  /**
   * Instantiates DictionaryAdapterFactory.
   *
   * @param adapters
   */
  public DictionaryAdapterFactory(List<PlatformAdapter<?>> adapters) {
    adapterMap = new HashMap<>();
    adapterMap.put(Platform.BSDD, findAdapter(adapters, BsddPlatformAdapter.class));
  }

  /**
   * Returns the appropriate instance based on the given dictionary name.
   *
   * @param dictionaryPlatform The dictionary library
   * @param <T> The specific dictionary type
   * @return The corresponding dictionary instance.
   * @throws IllegalArgumentException
   */
  @SuppressWarnings("unchecked")
  public <T> PlatformAdapter<T> getAdapter(Platform dictionaryPlatform) {
    PlatformAdapter<?> adapter = adapterMap.get(dictionaryPlatform);
    if (adapter == null) {
      throw new UnsupportedOperationException(
          "No adapter found for dictionary: " + dictionaryPlatform);
    }
    return (PlatformAdapter<T>) adapter;
  }

  private <T extends PlatformAdapter<?>> T findAdapter(
      List<PlatformAdapter<?>> adapters, Class<T> genericClass) {
    return adapters.stream()
        .filter(genericClass::isInstance)
        .map(genericClass::cast)
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException("Adapter not found: " + genericClass.getSimpleName()));
  }
}
