package com.opencirc.api.passport.enums;

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
}
