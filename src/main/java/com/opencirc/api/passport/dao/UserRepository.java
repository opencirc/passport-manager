package com.opencirc.api.passport.dao;

import com.opencirc.api.passport.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** User repository. */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

  /**
   * Fetches a user by email.
   */
  User findByEmail(String email);

  /**
   * Checks whether a user with the given email exists.
   */
  boolean existsByEmail(String email);

  /**
   * Retrieves the first user ordered by ID in ascending order.
   */
  Optional<User> findFirstByOrderByIdAsc();

  /**
   * Updates the refresh token.
   */
  @Transactional
  @Modifying
  @Query("UPDATE User u SET u.refreshToken = :refreshToken" + " WHERE u.id = :id")
  void updateRefreshTokenById(@Param("id") String id, @Param("refreshToken") String refreshToken);
}
