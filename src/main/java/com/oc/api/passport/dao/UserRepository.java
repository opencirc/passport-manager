package com.oc.api.passport.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.oc.api.passport.dto.UserDto;

@Repository
public interface UserRepository extends JpaRepository<UserDto, Long> {
	UserDto findByUsername(String username);

	UserDto findByEmail(String email);
	
	 boolean existsByUsername(String username);
}
