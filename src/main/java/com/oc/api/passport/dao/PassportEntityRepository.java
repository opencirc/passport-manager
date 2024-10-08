package com.oc.api.passport.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.oc.api.passport.dto.PassportEntity;

public interface PassportEntityRepository extends JpaRepository<PassportEntity, String> {
	
}