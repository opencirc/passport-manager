package com.opencirc.api.passport.dao;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.opencirc.api.passport.model.ApiKey;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID>  {

    /**
     * Gets all API tokens for the given userId.
     * @param userId
     * @return list of ApiKey instances
     */
    List<ApiKey> findAllByUserId(UUID userId);
}
