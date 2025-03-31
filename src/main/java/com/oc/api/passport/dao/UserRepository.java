package com.oc.api.passport.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.oc.api.passport.dto.UserEntity;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

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
}
