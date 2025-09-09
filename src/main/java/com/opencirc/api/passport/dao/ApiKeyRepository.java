package com.opencirc.api.passport.dao;

import com.opencirc.api.passport.model.ApiKey;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

  /**
   * Finds an API key by its ID.
   *
   * @param id the API key UUID
   * @return an Optional containing the ApiKey if found
   */
  Optional<ApiKey> findById(UUID id);
}
