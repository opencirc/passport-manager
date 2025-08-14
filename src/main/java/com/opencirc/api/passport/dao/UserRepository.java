package com.opencirc.api.passport.dao;

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
