package com.opencirc.api.passport.dao;

import com.opencirc.api.passport.model.Admin;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Administrator repository. */
@Repository
public interface AdminRepository extends JpaRepository<Admin, String> {

  /** Fetches an administrator by email. */
  Admin findByEmail(String email);

  /** Checks whether an administrator with the given email exists. */
  boolean existsByEmail(String email);

  /** Retrieves the first administrator ordered by ID in ascending order. */
  Optional<Admin> findFirstByOrderByIdAsc();

  /** Updates the refresh token. */
  @Transactional
  @Modifying
  @Query("UPDATE Admin a SET a.refreshToken = :refreshToken WHERE a.id = :id")
  void updateRefreshTokenById(@Param("id") String id, @Param("refreshToken") String refreshToken);
}
