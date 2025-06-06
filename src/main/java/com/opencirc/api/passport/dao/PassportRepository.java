package com.opencirc.api.passport.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.opencirc.api.passport.model.Passport;

public interface PassportRepository
        extends JpaRepository<Passport, String> {

    /**
     * Retrieves passport.
     *
     * @param id
     *
     * @return passports
     */
    @Query(value = "SELECT pe.id as passportId, pe.name as peName, "
            + "ds.id as datasheetId, ds.template_entry as templateEntry,"
            + "ds.data_category as dataCategory FROM Passport pe "
            + "JOIN datasheet_mapping pdm ON pe.id = pdm.passport_id "
            + "JOIN datasheet ds ON pdm.datasheet_id = ds.id "
            + "WHERE pe.id = :id "
            + "AND pe.status = :status", nativeQuery = true)
    Optional<Passport> findPassport(@Param("id") String id);

    /**
     * Retrieves passport with its children.
     *
     * @param id
     *
     * @return passports and its children
     */
    @Query(value = """
            WITH RECURSIVE PassportTree AS (
                SELECT pe.id, pe.name, pe.status, pe.parent_id
                FROM Passport pe
                WHERE pe.id = :id

                UNION ALL

                SELECT child.id, child.name, child.status, child.parent_id
                FROM Passport child
                INNER JOIN PassportTree parent ON child.parent_id = parent.id
            )
            SELECT pt.id AS passportId,
                   pt.name AS passportName,
                   ds.id AS datasheetId,
                   ds.template_entry AS templateEntry
                   ds.data_category AS dataCategory
            FROM PassportTree pt
            JOIN datasheet_mapping pdm ON pt.id = pdm.passport_id
            JOIN datasheet ds ON pdm.datasheet_id = ds.id
            WHERE pt.status = 'active'
            """, nativeQuery = true)
    List<Object[]> findActivePassportWithDescendant(
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
    @Query("UPDATE Passport p SET p.status = 'inactive'"
            + "WHERE p.id = :id")
    int updateStatusToInactive(@Param("id") String id);

    /**
     * Retrieves the parentId.
     * @param id
     * @return status
     */
    @Transactional
    @Query("Select p.parentId from Passport p "
            + "WHERE p.id = :id")
    String getParentId(@Param("id") String id);
}
