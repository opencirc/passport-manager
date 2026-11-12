package com.opencirc.api.passport.dao;

import com.opencirc.api.passport.dto.query.PassportDatasheetResultMapQueryResult;
import com.opencirc.api.passport.model.Passport;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/** Passport repository. Listings exclude archived passports unless stated otherwise. */
public interface PassportRepository extends JpaRepository<Passport, String> {

  /** Retrieves a passport in the given status with its datasheets and definitions. */
  @Query(
      "SELECT DISTINCT p FROM Passport p "
          + "LEFT JOIN FETCH p.datasheets d "
          + "LEFT JOIN FETCH d.definition def "
          + "LEFT JOIN FETCH def.properties "
          + "WHERE p.id = :id AND p.status = :status")
  Optional<Passport> findPassport(@Param("id") String id, @Param("status") Passport.Status status);

  /** Retrieves a passport of a project, in any status, with its datasheets and definitions. */
  @Query(
      "SELECT DISTINCT p FROM Passport p "
          + "LEFT JOIN FETCH p.datasheets d "
          + "LEFT JOIN FETCH d.definition def "
          + "LEFT JOIN FETCH def.properties "
          + "WHERE p.id = :id AND p.projectId = :projectId")
  Optional<Passport> findByIdInProject(
      @Param("id") String id, @Param("projectId") String projectId);

  /** Retrieves the root passport of a project. */
  Optional<Passport> findFirstByProjectIdAndParentIdIsNull(String projectId);

  /** Retrieves a passport of a project with its non-archived descendants. */
  @Query(
      value =
          """
            WITH RECURSIVE PassportTree AS (
                SELECT pe.id, pe.project_id, pe.name, pe.status, pe.parent_id,
                pe.created_by_id, pe.created_time
                FROM passports pe
                WHERE pe.id = :passport_id AND pe.project_id = :project_id

                UNION ALL

                SELECT child.id, child.project_id, child.name, child.status, child.parent_id,
                child.created_by_id, child.created_time
                FROM passports child
                INNER JOIN PassportTree parent ON child.parent_id = parent.id
            )
            SELECT pt.id AS passportId,
                   pt.project_id AS projectId,
                   pt.name AS passportName,
                   pt.status AS status,
                   pt.parent_id AS parentId,
                   pt.created_by_id AS passportCreatedById,
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
                   pd.created_by_id AS datasheetCreatedById,
                   pd.created_time AS datasheetCreatedTime,
                   dprop.id AS datasheetPropertyId,
                   pd.id AS datasheetPropertyDatasheetId,
                   dprop.code AS datasheetPropertyCode,
                   dprop.platform_id AS datasheetPropertyPlatformId,
                   dprop.group_tag AS datasheetPropertyGroupTag,
                   dprop.property_type AS datasheetPropertyType,
                   dprop.definition AS datasheetPropertyDefinition
            FROM PassportTree pt
            LEFT JOIN passport_datasheets pd ON pt.id = pd.passport_id
            LEFT JOIN datasheet_definitions def ON pd.definition_id = def.id
            LEFT JOIN datasheet_definition_properties dprop ON def.id = dprop.definition_id
            WHERE pt.status <> 'archived'
            """,
      nativeQuery = true)
  Optional<List<PassportDatasheetResultMapQueryResult>> findPassportWithDescendants(
      @Param("passport_id") String passportId, @Param("project_id") String projectId);

  /** Retrieves the non-archived immediate children of a passport within a project. */
  @Query(
      value =
          """
            SELECT DISTINCT p.id AS passportId,
                   p.project_id AS projectId,
                   p.name AS passportName,
                   p.status AS status,
                   p.parent_id AS parentId,
                   p.created_by_id AS passportCreatedById,
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
            WHERE p.parent_id = :passport_id
              AND p.project_id = :project_id
              AND p.status <> 'archived'
            """,
      nativeQuery = true)
  Optional<List<PassportDatasheetResultMapQueryResult>> findImmediateChildren(
      @Param("passport_id") String passportId, @Param("project_id") String projectId);

  /** Deactivates the passport. */
  @Modifying
  @Transactional
  @Query("UPDATE Passport p SET p.status = 'inactive' " + "WHERE p.id = :passport_id")
  int updateStatusToInactive(@Param("passport_id") String passportId);

  /** Retrieves the parentId. */
  @Transactional
  @Query("Select p.parentId from Passport p " + "WHERE p.id = :passport_id")
  String getParentId(@Param("passport_id") String passportId);

  /** Retrieves the non-archived root passports of a project. */
  @Query(
      "SELECT DISTINCT p FROM Passport p "
          + "LEFT JOIN FETCH p.datasheets d "
          + "LEFT JOIN FETCH d.definition def "
          + "LEFT JOIN FETCH def.properties "
          + "WHERE p.projectId = :projectId AND p.parentId IS NULL AND p.status <> 'archived'")
  List<Passport> getRootPassports(@Param("projectId") String projectId);

  /** Retrieves the non-archived passports of a project carrying a datasheet with the code. */
  @Query(
      value =
          """
                SELECT
    p.id AS passportId,
    p.project_id AS projectId,
    p.name AS passportName,
    p.status AS status,
    p.parent_id AS parentId,
    p.created_by_id AS passportCreatedById,
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
                AND p.project_id = :project_id
                AND p.status <> 'archived'
                """,
      nativeQuery = true)
  Optional<List<PassportDatasheetResultMapQueryResult>> findPassportsByCode(
      @Param("code") String code, @Param("project_id") String projectId);

  /** Soft-archives every non-archived passport of a project. Returns the number of rows. */
  @Modifying
  @Query(
      "UPDATE Passport p SET p.status = :archived, p.archivedTime = :time "
          + "WHERE p.projectId = :projectId AND p.status <> :archived")
  int archiveByProjectId(
      @Param("projectId") String projectId,
      @Param("time") OffsetDateTime time,
      @Param("archived") Passport.Status archived);

  /** Soft-archives a passport and its whole descendant subtree. Returns the number of rows. */
  @Modifying
  @Query(
      value =
          """
            WITH RECURSIVE subtree AS (
                SELECT id FROM passports WHERE id = :passport_id
                UNION ALL
                SELECT child.id FROM passports child
                INNER JOIN subtree parent ON child.parent_id = parent.id
            )
            UPDATE passports
            SET status = 'archived', archived_time = :time
            WHERE id IN (SELECT id FROM subtree) AND status <> 'archived'
            """,
      nativeQuery = true)
  int archiveSubtree(@Param("passport_id") String passportId, @Param("time") OffsetDateTime time);
}
