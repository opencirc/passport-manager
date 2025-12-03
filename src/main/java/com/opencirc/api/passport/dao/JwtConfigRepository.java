package com.opencirc.api.passport.dao;

import com.opencirc.api.passport.model.JwtConfig;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface JwtConfigRepository extends JpaRepository<JwtConfig, String> {
  /** Fetches the stored secret key. */
  @Query(value = "SELECT secret_key FROM jwt_configs LIMIT 1", nativeQuery = true)
  Optional<JwtConfig> getSecretKey();

  /** Saves or updates the secret key and encryption key in the table. */
  @Modifying
  @Transactional
  @Query(
      value = "INSERT INTO jwt_configs (secret_key) " + "VALUES (:secretKey)",
      nativeQuery = true)
  void saveConfig(@Param("secretKey") String secretKey);
}
