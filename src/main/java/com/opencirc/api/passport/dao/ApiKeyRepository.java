package com.opencirc.api.passport.dao;

import com.opencirc.api.passport.model.ApiKey;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, String> {

  /**
   * Gets all API tokens for the given userId.
   *
   * @param userId
   * @return list of ApiKey instances
   */
  List<ApiKey> findAllByUserId(String userId);

  /**
   * Finds an API key by its ID.
   *
   * @param id the API key UUID
   * @return an Optional containing the ApiKey if found
   */
  Optional<ApiKey> findById(String id);
}
