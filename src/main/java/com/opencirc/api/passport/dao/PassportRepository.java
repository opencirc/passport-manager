package com.opencirc.api.passport.dao;

import com.opencirc.api.passport.dto.query.PassportDatasheetResultMapDto;
import com.opencirc.api.passport.model.Passport;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PassportRepository extends JpaRepository<Passport, String> {

  /**
   * Retrieves passport.
   *
   * @param id
   * @param status
   * @return passports
   */
  @Query(
      "SELECT DISTINCT p FROM Passport p "
          + "LEFT JOIN FETCH p.datasheetMappings dm "
          + "LEFT JOIN FETCH dm.datasheet d "
          + "LEFT JOIN FETCH d.datasheetProperties "
          + "WHERE p.id = :id AND p.status = :status")
  Optional<Passport> findPassport(@Param("id") String id, @Param("status") Passport.Status status);

  /**
   * Retrieves a passport with its descendants.
   *
   * @param passportId
   * @return the passport with its descendants
   */
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
                   ds.id AS datasheetId,
                   ds.platform AS platform,
                   ds.dictionary AS dictionary,
                   ds.code AS datasheetCode,
                   ds.name AS datasheetName,
                   ds.description AS datasheetDescription,
                   ds.platform_id AS datasheetPlatformId,
                   ds.data_category AS dataCategory,
                   ds.data AS data,
                   ds.created_by_id AS datasheetCreatedById,
                   ds.created_time AS datasheetCreatedTime,
                   dp.id AS datasheetPropertyId,
                   dp.code AS datasheetPropertyCode,
                   dp.platform_id AS datasheetPropertyPlatformId,
                   dp.group_tag AS datasheetPropertyGroupTag,
                   dp.property_type AS datasheetPropertyType,
                   dp.definition AS datasheetPropertyDefinition
            FROM PassportTree pt
            LEFT JOIN passport_datasheet_mappings pdm ON pt.id = pdm.passport_id
            LEFT JOIN datasheets ds ON pdm.datasheet_id = ds.id
            LEFT JOIN datasheet_property dp ON ds.id = dp.datasheet_id
            WHERE pt.status = 'ACTIVE'
            """,
      nativeQuery = true)
  Optional<List<PassportDatasheetResultMapDto>> findPassportWithDescendants(
      @Param("passport_id") String passportId);

  /**
   * Retrieves the immediate children of a passport.
   *
   * @param passportId
   * @return the passports whose parent_id matches the given passport id
   */
  @Query(
      value =
          """
            SELECT p.id AS passportId,
                   p.name AS passportName,
                   p.status AS status,
                   p.parent_id AS parentId,
                   p.created_by_id AS passportCreatedById,
                   p.created_by AS passportCreatedBy,
                   p.created_time AS passportCreatedTime,
                   ds.id AS datasheetId,
                   ds.platform AS platform,
                   ds.dictionary AS dictionary,
                   ds.code AS datasheetCode,
                   ds.name AS datasheetName,
                   ds.description AS datasheetDescription,
                   ds.platform_id AS datasheetPlatformId,
                   ds.data_category AS dataCategory,
                   ds.data AS data,
                   ds.created_by_id AS datasheetCreatedById,
                   ds.created_time AS datasheetCreatedTime,
                   dp.id AS datasheetPropertyId,
                   dp.datasheet_id AS datasheetPropertyDatasheetId,
                   dp.code AS datasheetPropertyCode,
                   dp.platform_id AS datasheetPropertyPlatformId,
                   dp.group_tag AS datasheetPropertyGroupTag,
                   dp.property_type AS datasheetPropertyType,
                   dp.definition AS datasheetPropertyDefinition

            FROM passports p
            LEFT JOIN passport_datasheet_mappings pdm ON p.id = pdm.passport_id
            LEFT JOIN datasheets ds ON pdm.datasheet_id = ds.id
            LEFT JOIN datasheet_property dp ON ds.id = dp.datasheet_id
            WHERE p.parent_id = :passport_id AND p.status = 'ACTIVE'
            """,
      nativeQuery = true)
  Optional<List<PassportDatasheetResultMapDto>> findImmediateChildren(
      @Param("passport_id") String passportId);

  /**
   * Deactivates the passport.
   *
   * @param passportId
   * @return status
   */
  @Modifying
  @Transactional
  @Query("UPDATE Passport p SET p.status = 'inactive'" + "WHERE p.id = :passport_id")
  int updateStatusToInactive(@Param("passport_id") String passportId);

  /**
   * Retrieves the parentId.
   *
   * @param passportId
   * @return status
   */
  @Transactional
  @Query("Select p.parentId from Passport p " + "WHERE p.id = :passport_id")
  String getParentId(@Param("passport_id") String passportId);

  /**
   * Retrieves passports without parent.
   *
   * @return passports
   */
  @Query(
      "SELECT DISTINCT p FROM Passport p "
          + "LEFT JOIN FETCH p.datasheetMappings dm "
          + "LEFT JOIN FETCH dm.datasheet d "
          + "LEFT JOIN FETCH d.datasheetProperties "
          + "WHERE p.status = 'ACTIVE' AND p.parentId IS NULL ")
  List<Passport> getRootPassports();
}
