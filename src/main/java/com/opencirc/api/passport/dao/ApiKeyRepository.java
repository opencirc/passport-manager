package com.opencirc.api.passport.dao;

import com.opencirc.api.passport.model.ApiKey;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, String> {

  /** Gets all API tokens for the given userId. */
  List<ApiKey> findAllByUserId(String userId);

  /** Finds an API key by its ID. */
  Optional<ApiKey> findById(String id);
}
