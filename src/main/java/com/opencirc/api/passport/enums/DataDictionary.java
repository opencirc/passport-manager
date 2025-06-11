package com.opencirc.api.passport.enums;

import java.util.Arrays;

public enum DataDictionary {
    BSDD("bsdd"),
    LEXICON("lexicon");

    private final String value;

    DataDictionary(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
    
    public static DataDictionary fromValue(String value) {
        return Arrays.stream(DataDictionary.values())
                .filter(dd -> dd.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid dictionary: " + value));
    }
}
