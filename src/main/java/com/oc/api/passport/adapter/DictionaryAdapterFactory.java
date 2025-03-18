package com.oc.api.passport.adapter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Factory class for providing the appropriate implementation
 * based on the specified dictionary library.
 */
@Component
public class DictionaryAdapterFactory {

    /**
     * Injecting BsDDAdapter.
     */
    @Autowired
    private BsDDAdapter bsddAdapter;

    /**
     * Returns the appropriate instance based on the given dictionary name.
     *
     * @param ddLibrary The name of the dictionary library (e.g., "bsdd", "define").
     * @return The corresponding implementation.
     * @throws IllegalArgumentException
     */
    public DictionaryAdapter getAdapter(String ddLibrary) {
        switch (ddLibrary.toLowerCase()) {
        case "bsdd":
            return bsddAdapter;
        case "define":
            return null;

        default:
            throw new IllegalArgumentException(
                    "Invalid dictionary name: " + ddLibrary);
        }
    }
}
