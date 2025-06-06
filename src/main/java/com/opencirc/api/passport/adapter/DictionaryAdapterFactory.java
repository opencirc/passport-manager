package com.opencirc.api.passport.adapter;

import com.opencirc.api.passport.adapter.bsdd.BsDDAdapter;
import com.opencirc.api.passport.enums.DataDictionary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.opencirc.api.passport.exception.InvalidInputException;


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
     * @param dictionary The dictionary library (e.g., "bsdd", "define").
     * @return The corresponding implementation.
     * @throws IllegalArgumentException
     */
    public DictionaryAdapter getAdapter(DataDictionary dictionary)
           throws InvalidInputException {
        switch (dictionary) {
            case DataDictionary.BSDD:
                return bsddAdapter;

            default:
                throw new InvalidInputException("Invalid dictionary name: " + dictionary);

        }
    }
}
