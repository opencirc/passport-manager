package com.opencirc.api.passport.dao;

import com.opencirc.api.passport.dto.query.PassportDatasheetResultMapQueryResult;
import com.opencirc.api.passport.model.Passport;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/** Passport repository. */
public interface PassportRepository extends JpaRepository<Passport, String> {

  /** Retrieves a Passport. */
  @Query(
      "SELECT DISTINCT p FROM Passport p "
          + "LEFT JOIN FETCH p.datasheets d "
          + "LEFT JOIN FETCH d.definition def "
          + "LEFT JOIN FETCH def.properties "
          + "WHERE p.id = :id AND p.status = :status")
  Optional<Passport> findPassport(@Param("id") String id, @Param("status") Passport.Status status);

  /** Retrieves a passport with its descendants. */
  @Query(
      value =
          """
            WITH RECURSIVE PassportTree AS (
                SELECT pe.id, pe.name, pe.status, pe.parent_id,
                pe.created_by_id, pe.created_by,
                pe.created_time
                FROM passports pe
                WHERE pe.id = :passport_id

                UNION ALL

                SELECT child.id, child.name, child.status, child.parent_id,
                child.created_by_id, child.created_by, child.created_time
                FROM passports child
                INNER JOIN PassportTree parent ON child.parent_id = parent.id
            )
            SELECT pt.id AS passportId,
                   pt.name AS passportName,
                   pt.status AS status,
                   pt.parent_id AS parentId,
                   pt.created_by_id AS passportCreatedById,
                   pt.created_by AS passportCreatedBy,
                   pt.created_time AS passportCreatedTime,
                   pd.id AS datasheetId,
                   def.platform AS platform,
                   def.dictionary AS dictionary,
                   def.code AS datasheetCode,
                   def.name AS datasheetName,
                   def.description AS datasheetDescription,
                   def.platform_id AS datasheetPlatformId,
                   pd.data_category AS dataCategory,
                   pd.data AS data,
                   pd.created_by AS datasheetCreatedBy,
                   pd.created_by_id AS datasheetCreatedById,
                   pd.created_time AS datasheetCreatedTime,
                   dprop.id AS datasheetPropertyId,
                   dprop.code AS datasheetPropertyCode,
                   dprop.platform_id AS datasheetPropertyPlatformId,
                   dprop.group_tag AS datasheetPropertyGroupTag,
                   dprop.property_type AS datasheetPropertyType,
                   dprop.definition AS datasheetPropertyDefinition
            FROM PassportTree pt
            LEFT JOIN passport_datasheets pd ON pt.id = pd.passport_id
            LEFT JOIN datasheet_definitions def ON pd.definition_id = def.id
            LEFT JOIN datasheet_definition_properties dprop ON def.id = dprop.definition_id
            WHERE pt.status = 'active'
            """,
      nativeQuery = true)
  Optional<List<PassportDatasheetResultMapQueryResult>> findPassportWithDescendants(
      @Param("passport_id") String passportId);

  /** Retrieves the immediate children of a passport. */
  @Query(
      value =
          """
            SELECT DISTINCT p.id AS passportId,
                   p.name AS passportName,
                   p.status AS status,
                   p.parent_id AS parentId,
                   p.created_by_id AS passportCreatedById,
                   p.created_by AS passportCreatedBy,
                   p.created_time AS passportCreatedTime,
                   pd.id AS datasheetId,
                   def.platform AS platform,
                   def.dictionary AS dictionary,
                   def.code AS datasheetCode,
                   def.name AS datasheetName,
                   def.description AS datasheetDescription,
                   def.platform_id AS datasheetPlatformId,
                   pd.data_category AS dataCategory,
                   pd.data AS data,
                   pd.created_by_id AS datasheetCreatedById,
                   pd.created_by AS datasheetCreatedBy,
                   pd.created_time AS datasheetCreatedTime,
                   dprop.id AS datasheetPropertyId,
                   pd.id AS datasheetPropertyDatasheetId,
                   dprop.code AS datasheetPropertyCode,
                   dprop.platform_id AS datasheetPropertyPlatformId,
                   dprop.group_tag AS datasheetPropertyGroupTag,
                   dprop.property_type AS datasheetPropertyType,
                   dprop.definition AS datasheetPropertyDefinition

            FROM passports p
            LEFT JOIN passport_datasheets pd ON p.id = pd.passport_id
            LEFT JOIN datasheet_definitions def ON pd.definition_id = def.id
            LEFT JOIN datasheet_definition_properties dprop ON def.id = dprop.definition_id
            WHERE p.parent_id = :passport_id AND p.status = 'active'
            """,
      nativeQuery = true)
  Optional<List<PassportDatasheetResultMapQueryResult>> findImmediateChildren(
      @Param("passport_id") String passportId);

  /** Deactivates the passport. */
  @Modifying
  @Transactional
  @Query("UPDATE Passport p SET p.status = 'inactive' " + "WHERE p.id = :passport_id")
  int updateStatusToInactive(@Param("passport_id") String passportId);

  /** Retrieves the parentId. */
  @Transactional
  @Query("Select p.parentId from Passport p " + "WHERE p.id = :passport_id")
  String getParentId(@Param("passport_id") String passportId);

  /** Retrieves passports without parent. */
  @Query(
      "SELECT DISTINCT p FROM Passport p "
          + "LEFT JOIN FETCH p.datasheets d "
          + "LEFT JOIN FETCH d.definition def "
          + "LEFT JOIN FETCH def.properties "
          + "WHERE p.status = 'active' AND p.parentId IS NULL ")
  List<Passport> getRootPassports();

  /** Retrieves passports for the specified platform and code. */
  @Query(
      value =
          """
                SELECT
    p.id AS passportId,
    p.name AS passportName,
    p.status AS status,
    p.parent_id AS parentId,
    p.created_by_id AS passportCreatedById,
    p.created_by AS passportCreatedBy,
    p.created_time AS passportCreatedTime,
    pd.id AS datasheetId,
    def.platform AS platform,
    def.dictionary AS dictionary,
    def.code AS datasheetCode,
    def.name AS datasheetName,
    def.description AS datasheetDescription,
    def.platform_id AS datasheetPlatformId,
    pd.data_category AS dataCategory,
    pd.data AS data,
    pd.created_by_id AS datasheetCreatedById,
    pd.created_time AS datasheetCreatedTime,
    dprop.id AS datasheetPropertyId,
    pd.id AS datasheetPropertyDatasheetId,
    dprop.code AS datasheetPropertyCode,
    dprop.platform_id AS datasheetPropertyPlatformId,
    dprop.group_tag AS datasheetPropertyGroupTag,
    dprop.property_type AS datasheetPropertyType,
    dprop.definition AS datasheetPropertyDefinition
FROM passports p
LEFT JOIN passport_datasheets pd ON p.id = pd.passport_id
LEFT JOIN datasheet_definitions def ON pd.definition_id = def.id
LEFT JOIN datasheet_definition_properties dprop ON def.id = dprop.definition_id
                WHERE def.code = :code
                AND p.status = 'active'
                """,
      nativeQuery = true)
  Optional<List<PassportDatasheetResultMapQueryResult>> findPassportsByCode(String code);
}
