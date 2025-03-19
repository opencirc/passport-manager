package com.oc.api.passport.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
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
     * Gets the user details from email.
     *
     * @param email
     * @return User Entity
     */
    UserEntity findByEmail(String email);


    /**
     * Checks if the user is already present.
     *
     * @param username
     * @return status
     */
    boolean existsByUsername(String username);
}
