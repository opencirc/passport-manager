package com.opencirc.api.passport.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.opencirc.api.passport.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {


    /**
     * Fetches a user by name.
     *
     * @param username
     * @return User Entity
     */
    User findByUsername(String username);


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
     * @param id
     * @param refreshToken
     */
    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.refreshToken = :refreshToken"
            + " WHERE u.id = :id")
    void updateRefreshTokenById(@Param("id") Long id,
                                @Param("refreshToken") String refreshToken);
}
