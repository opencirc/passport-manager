package com.opencirc.api.passport.dto;

import java.sql.Timestamp;

/** Interface to map the result set to passportDto. */
public interface PassportDatasheetResultMapDto {

  /**
   * Returns the unique identifier of the passport.
   *
   * @return the ID of the passport
   */
  String getPassportId();

  /**
   * Returns the name of the passport.
   *
   * @return the name of the passport
   */
  String getPassportName();

  /**
   * Returns the status of the passport.
   *
   * @return the status of the passport
   */
  String getStatus();

  /**
   * Returns the parent Id of the passport.
   *
   * @return the parent ID of the passport (if any)
   */
  String getParentId();

  /**
   * Returns the ID of the associated datasheet.
   *
   * @return the ID of the associated datasheet
   */
  String getDatasheetId();

  /**
   * Returns the JSON data of the datasheet.
   *
   * @return the JSON data of the datasheet as a string
   */
  String getData();

  /**
   * Returns the type of data dictionary.
   *
   * @return the data dictionary type (e.g., BSDD or Lexicon)
   */
  String getDataDictionary();

  /**
   * Returns the category of the datasheet (e.g., UNIQUE or GENERIC).
   *
   * @return the category of the datasheet (e.g., UNIQUE or GENERIC)
   */
  String getDataCategory();

  /**
   * Returns the user who created the passport or datasheet.
   *
   * @return the user who created the passport or datasheet
   */
  String getCreatedBy();

  /**
   * Returns the timestamp when the passport or datasheet was created.
   *
   * @return the creation timestamp
   */
  Timestamp getCreatedTime();
}
