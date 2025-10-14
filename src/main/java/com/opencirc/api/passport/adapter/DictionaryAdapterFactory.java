package com.opencirc.api.passport.adapter;

import com.opencirc.api.passport.adapter.bsdd.BsddAdapter;
import com.opencirc.api.passport.enums.DataDictionaryPlatform;
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
  private final Map<DataDictionaryPlatform, DictionaryAdapter<?>> adapterMap;

  /**
   * Instantiates DictionaryAdapterFactory.
   *
   * @param adapters
   */
  public DictionaryAdapterFactory(List<DictionaryAdapter<?>> adapters) {
    adapterMap = new HashMap<>();
    adapterMap.put(DataDictionaryPlatform.BSDD, findAdapter(adapters, BsddAdapter.class));
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
  public <T> DictionaryAdapter<T> getAdapter(DataDictionaryPlatform dictionaryPlatform) {
    DictionaryAdapter<?> adapter = adapterMap.get(dictionaryPlatform);
    if (adapter == null) {
      throw new UnsupportedOperationException(
          "No adapter found for dictionary: " + dictionaryPlatform);
    }
    return (DictionaryAdapter<T>) adapter;
  }

  private <T extends DictionaryAdapter<?>> T findAdapter(
      List<DictionaryAdapter<?>> adapters, Class<T> genericClass) {
    return adapters.stream()
        .filter(genericClass::isInstance)
        .map(genericClass::cast)
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException("Adapter not found: " + genericClass.getSimpleName()));
  }
}
