package com.oc.api.passport.dao;

import java.util.Optional;

import com.oc.api.passport.model.JwtConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface JwtConfigRepository extends JpaRepository<JwtConfig, String> {
    /**
     * Fetches the stored secret key.
     * @return the instance of JwtConfig
     */
    @Query(value = "SELECT secret_key FROM jwt_config LIMIT 1", nativeQuery = true)
    Optional<JwtConfig> getSecretKey();


    /**
     * Saves or updates the secret key and encryption key in the table.
     * @param secretKey
     */
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO jwt_config (secret_key) "
            + "VALUES (:secretKey)", nativeQuery = true)
    void saveConfig(@Param("secretKey") String secretKey);
}
