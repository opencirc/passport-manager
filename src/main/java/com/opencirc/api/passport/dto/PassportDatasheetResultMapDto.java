package com.opencirc.api.passport.dto;

import java.time.LocalDateTime;

/** Interface to map the result set to passportDto. */
public interface PassportDatasheetResultMapDto {

  /** Returns the ID of the passport. */
  String getPassportId();

  /** Returns the name of the passport. */
  String getPassportName();

  /** Returns the status of the passport. */
  String getStatus();

  /** Returns the parent ID of the passport (if any). */
  String getParentId();

  /** Returns the ID of the associated datasheet. */
  String getDatasheetId();

  /** Returns the JSON data of the datasheet as a string. */
  String getData();

  /** Returns the data dictionary type (e.g., BSDD or Lexicon). */
  String getDataDictionary();

  /** Returns the category of the datasheet (e.g., UNIQUE or GENERIC). */
  String getDataCategory();

  /** Returns the user id who created the passport. */
  String getPassportCreatedById();

  /** Returns the JSON (as text) containing user information who created the passport. */
  String getPassportCreatedBy();

  /** Returns the creation timestamp of the passport. */
  LocalDateTime getPassportCreatedTime();

  /** Returns the user id who created the datasheet. */
  String getDatasheetCreatedById();

  /** Returns the JSON (as text) containing user information who created the datasheet. */
  String getDatasheetCreatedBy();

  /** Returns the creation timestamp of the datasheet. */
  LocalDateTime getDatasheetCreatedTime();
}
