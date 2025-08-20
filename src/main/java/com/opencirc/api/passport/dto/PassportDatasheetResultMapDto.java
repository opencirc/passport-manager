package com.opencirc.api.passport.dto;

import java.sql.Timestamp;

/**
 * Interface to map the result set to passportDto.
 */
public interface PassportDatasheetResultMapDto {

    /**
     * @return the ID of the passport
     */
    String getPassportId();

    /**
     * @return the name of the passport
     */
    String getPassportName();

    /**
     * @return the status of the passport
     */
    String getStatus();

    /**
     * @return the parent ID of the passport (if any)
     */
    String getParentId();

    /**
     * @return the ID of the associated datasheet
     */
    String getDatasheetId();

    /**
     * @return the JSON data of the datasheet as a string
     */
    String getData();

    /**
     * @return the data dictionary type (e.g., BSDD or Lexicon)
     */
    String getDataDictionary();

    /**
     * @return the category of the datasheet (e.g., UNIQUE or GENERIC)
     */
    String getDataCategory();

    /**
     * @return the user id who created the passport or datasheet
     */
    String getCreatedById();

    /**
     * @return the user information who created the passport or datasheet
     */
    String getCreatedBy();

    /**
     * @return the creation timestamp
     */
    Timestamp getCreatedTime();
}
