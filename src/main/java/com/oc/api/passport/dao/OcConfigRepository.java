package com.oc.api.passport.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.oc.api.passport.dto.OcConfig;

@Repository
public interface OcConfigRepository extends JpaRepository<OcConfig, String> {
	
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO oc_config (hashvalue) VALUES (:hashValue)", nativeQuery = true)
    void saveConfig(@Param("hashValue") String hashValue);
}
