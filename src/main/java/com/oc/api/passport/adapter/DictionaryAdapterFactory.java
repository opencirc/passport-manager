package com.oc.api.passport.adapter;

import com.oc.api.passport.adapter.bsdd.BsDDAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.oc.api.passport.exception.InvalidInputException;


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
    public DictionaryAdapter getAdapter(String ddLibrary)
           throws InvalidInputException {
        switch (ddLibrary.toLowerCase()) {
            case "bsdd":
                return bsddAdapter;
            case "define":
                return null;

            default:
                throw new InvalidInputException("Invalid dictionary name: " + ddLibrary);

        }
    }
}
