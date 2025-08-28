package com.opencirc.api.passport.dao;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.opencirc.api.passport.model.ApiKey;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID>  {

}
