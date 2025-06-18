package com.opencirc.api.passport.adapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.opencirc.api.passport.adapter.bsdd.BsDDAdapter;
import com.opencirc.api.passport.enums.DataDictionary;

/**
 * Factory class for providing the appropriate implementation based on the
 * specified dictionary library.
 */
@Component
public class DictionaryAdapterFactory {

    private final Map<DataDictionary, DictionaryAdapter<?>> adapterMap;

    public DictionaryAdapterFactory(List<DictionaryAdapter<?>> adapters) {
        adapterMap = new HashMap<>();
        adapterMap.put(DataDictionary.BSDD,
                findAdapter(adapters, BsDDAdapter.class));

    }

    /**
     * Returns the appropriate instance based on the given dictionary name.
     *
     * @param dictionary The dictionary library
     * @return The corresponding implementation.
     * @throws IllegalArgumentException
     */
    @SuppressWarnings("unchecked")
    public <T> DictionaryAdapter<T> getAdapter(DataDictionary dictionary) {
        DictionaryAdapter<?> adapter = adapterMap.get(dictionary);
        if (adapter == null) {
            throw new UnsupportedOperationException(
                    "No adapter found for dictionary: " + dictionary);
        }
        return (DictionaryAdapter<T>) adapter;
    }

    private <T extends DictionaryAdapter<?>> T findAdapter(
            List<DictionaryAdapter<?>> adapters, Class<T> genericClass) {
        return adapters.stream().filter(genericClass::isInstance).map(genericClass::cast).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Adapter not found: " + genericClass.getSimpleName()));
    }
}
