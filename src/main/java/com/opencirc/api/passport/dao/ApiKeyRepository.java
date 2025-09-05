package com.opencirc.api.passport.dao;

import com.opencirc.api.passport.model.ApiKey;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {}
