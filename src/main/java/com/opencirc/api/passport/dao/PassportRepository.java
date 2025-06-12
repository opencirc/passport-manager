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
    @Query("SELECT p FROM Passport p " +
            "LEFT JOIN FETCH p.datasheetMappings dm " +
            "LEFT JOIN FETCH dm.datasheet " +
            "WHERE p.id = :id AND p.status = :status")
    Optional<Passport> findPassport(@Param("id") String id, @Param("status") Passport.Status status);

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
                FROM passports pe
                WHERE pe.id = :id

                UNION ALL

                SELECT child.id, child.name, child.status, child.parent_id
                FROM passports child
                INNER JOIN PassportTree parent ON child.parent_id = parent.id
            )
            SELECT pt.id AS passportId,
                   pt.name AS passportName,
                   ds.id AS datasheetId,
                   ds.data AS data,
                   ds.data_category AS dataCategory
            FROM PassportTree pt
            JOIN passport_datasheet_mappings pdm ON pt.id = pdm.passport_id
            JOIN datasheets ds ON pdm.datasheet_id = ds.id
            WHERE pt.status = 'active'
            """, nativeQuery = true)
    Optional<List<Passport>> findActivePassportWithDescendant(
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
