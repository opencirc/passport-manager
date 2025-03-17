package com.oc.api.passport.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.oc.api.passport.dto.UserEntity;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
	UserEntity findByUsername(String username);

	UserEntity findByEmail(String email);
	
	boolean existsByUsername(String username);
}
