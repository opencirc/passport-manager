package com.oc.api.passport.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.oc.api.passport.dto.UserEntity;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {


    /**
     * Gets the user details from name.
     *
     * @param username
     * @return User Entity
     */
    UserEntity findByUsername(String username);

    /**
     * Gets the user details from name.
     *
     * @param userId
     * @return User Entity
     */
    UserEntity findByUserId(Long userId);


    /**
     * Checks if the user is already present.
     *
     * @param username
     * @return status
     */
    boolean existsByUsername(String username);

    /**
     * Updates the refresh token.
     *
     * @param userId
     * @param refreshToken
     */
    @Transactional
    @Modifying
    @Query("UPDATE UserEntity u SET u.refreshToken = :refreshToken"
            + " WHERE u.userId = :userId")
    void updateRefreshTokenByUserId(@Param("userId") Long userId,
            @Param("refreshToken") String refreshToken);
}
