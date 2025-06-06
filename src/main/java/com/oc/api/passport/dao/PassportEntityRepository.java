package com.oc.api.passport.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.oc.api.passport.model.PassportEntity;

public interface PassportEntityRepository
        extends JpaRepository<PassportEntity, String> {

    /**
     * Retrieves passport entity.
     *
     * @param id
     * @param status
     *
     * @return passports
     */
    @Query(value = "SELECT pe.id as passportEntityId, pe.name as peName, "
            + "ds.id as datasheetId, ds.template_entry as templateEntry,"
            + "ds.data_category as dataCategory " + "FROM passport_entity pe "
            + "JOIN datasheet_mapping pdm ON pe.id = pdm.passport_entity_id "
            + "JOIN datasheet ds ON pdm.datasheet_id = ds.id "
            + "WHERE pe.id = :id "
            // + "AND AND (:peName IS NULL OR pe.passport_entity_name = :peName)
            // "
            + "AND pe.status = :status", nativeQuery = true)
    List<Object[]> findActivePassportEntity(@Param("id") String id,
            @Param("status") String status);

    /**
     * Retrieves passport entity with its children.
     *
     * @param id
     *
     * @return passports and its children
     */
    @Query(value = """
            WITH RECURSIVE PassportTree AS (
                SELECT pe.id, pe.name, pe.status, pe.parent_id
                FROM passport_entity pe
                WHERE pe.id = :id

                UNION ALL

                SELECT child.id, child.name, child.status, child.parent_id
                FROM passport_entity child
                INNER JOIN PassportTree parent ON child.parent_id = parent.id
            )
            SELECT pt.id AS passportEntityId,
                   pt.name AS passportEntityName,
                   ds.id AS datasheetId,
                   ds.template_entry AS templateEntry
                   ds.data_category AS dataCategory
            FROM PassportTree pt
            JOIN datasheet_mapping pdm ON pt.id = pdm.passport_entity_id
            JOIN datasheet ds ON pdm.datasheet_id = ds.id
            WHERE pt.status = 'active'
            """, nativeQuery = true)
    List<Object[]> findActivePassportEntityWithDescendant(
            @Param("id") String id);

    /**
     * Deactivates the passport.
     *
     * @param id
     *
     * @return status
     */
    @Modifying
    @Transactional
    @Query("UPDATE PassportEntity p SET p.status = 'inactive'"
            + "WHERE p.id = :id")
    int updateStatusToInactive(@Param("id") String id);

    /**
     * Retrieves the parentId.
     * @param id
     * @return status
     */
    @Transactional
    @Query("Select p.parentId from PassportEntity p "
            + "WHERE p.id = :id")
    String getParentId(@Param("id") String id);
}
