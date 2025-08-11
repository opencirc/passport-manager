package com.opencirc.api.passport.dto;

import java.sql.Timestamp;

/** Interface to map the result set to passportDto. */
public interface PassportDatasheetResultMapDto {

  /**
   * The ID of the passport.
   */
  String passportId();

  /**
   * The name of the passport.
   */
  String passportName();

  /**
   * The status of the passport.
   *
   * @see com.opencirc.api.passport.model.Passport.Status for possible values
   */
  String status();

  /**
   * The ID of the parent passport, if any.
   */
  String parentId();

  /**
   * The ID of the associated datasheet.
   */
  String datasheetId();

  /**
   * The datasheet data as a JSON string.
   */
  String data();

  /**
   * The data dictionary type.
   *
   * @see com.opencirc.api.passport.enums.DataDictionary for possible values
   */
  String dataDictionary();

  /**
   * The category of the datasheet.
   *
   * @see com.opencirc.api.passport.model.Datasheet.DataCategory for possible values
   */
  String dataCategory();

  /**
   * The ID of the user who created the passport.
   */
  String createdBy();

  /**
   * The creation timestamp.
   */
  Timestamp createdTime();
}
