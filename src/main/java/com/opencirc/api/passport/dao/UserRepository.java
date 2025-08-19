package com.opencirc.api.passport.dao;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.opencirc.api.passport.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {


    /**
     * Fetches a user by email.
     *
     * @param email
     * @return User Entity
     */
    User findByEmail(String email);

    /**
     * Checks whether a user with the given email exists.
     *
     * @param email the email to check
     * @return true if a user exists with the given email; false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Retrieves the first user ordered by ID in ascending order.
     *
     * @return an Optional containing the first User if present, or an empty
     *         Optional if no users exist
     */
    Optional<User> findFirstByOrderByIdAsc();

    /**
     * Updates the refresh token.
     *
     * @param id
     * @param refreshToken
     */
    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.refreshToken = :refreshToken"
            + " WHERE u.id = :id")
    void updateRefreshTokenById(@Param("id") UUID id,
                                @Param("refreshToken") String refreshToken);
}
