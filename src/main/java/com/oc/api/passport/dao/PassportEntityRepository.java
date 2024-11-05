package com.oc.api.passport.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.oc.api.passport.dto.PassportEntityDto;

public interface PassportEntityRepository extends JpaRepository<PassportEntityDto, String> {

	@Query(value = "SELECT pe.passport_entity_id as passportEntityId, pe.pe_name as peName, "
			+ "ds.datasheet_id as datasheetId, ds.template_entry as templateEntry " + "FROM passport_entity pe "
			+ "JOIN pe_datasheet_mapping pdm ON pe.passport_entity_id = pdm.passport_entity_id "
			+ "JOIN pe_datasheet ds ON pdm.datasheet_id = ds.datasheet_id "
			+ "WHERE pe.passport_entity_id = :passportEntityId "
			// + "AND AND (:peName IS NULL OR pe.pe_name = :peName) "
			+ "AND pe.status = :status", nativeQuery = true)
	List<Object[]> findActivePassportEntity(@Param("passportEntityId") String peId,
			@Param("status") String status);

	@Query(value = """
	        WITH RECURSIVE PassportTree AS (
	            SELECT pe.passport_entity_id, pe.pe_name, pe.status, pe.parent_pe_id
	            FROM passport_entity pe
	            WHERE pe.passport_entity_id = :passportEntityId

	            UNION ALL

	            SELECT child.passport_entity_id, child.pe_name, child.status, child.parent_pe_id
	            FROM passport_entity child
	            INNER JOIN PassportTree parent ON child.parent_pe_id = parent.passport_entity_id
	        )
	        SELECT pt.passport_entity_id AS passportEntityId, 
	               pt.pe_name AS peName, 
	               ds.datasheet_id AS datasheetId, 
	               ds.template_entry AS templateEntry 
	        FROM PassportTree pt
	        JOIN pe_datasheet_mapping pdm ON pt.passport_entity_id = pdm.passport_entity_id
	        JOIN pe_datasheet ds ON pdm.datasheet_id = ds.datasheet_id
	        WHERE pt.status = 'active'
	        """, nativeQuery = true)
	List<Object[]> findActivePassportEntityWithDescendant(@Param("passportEntityId") String peId);

	@Modifying
	@Transactional
	@Query("UPDATE PassportEntityDto p SET p.status = 'inactive' WHERE p.passportEntityId = :passportEntityId")
	int updateStatusToInactive(@Param("passportEntityId") String passportEntityId);

	@Transactional
	@Query("Select p.parentPe from PassportEntityDto p WHERE p.passportEntityId = :passportEntityId")
	String getParentId(@Param("passportEntityId") String passportEntityId);
}