package com.oc.api.passport.auth.service;

import java.util.Optional;

import com.oc.api.passport.auth.principal.UserPrincipal;
import com.oc.api.passport.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.oc.api.passport.dao.UserRepository;

@Service
public class AuthUserDetailsService implements UserDetailsService {

    /**
     * Injecting UserRepository class.
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * Gets the user data by name.
     *
     * @param username
     * @return the details of the user
     */
    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        return new UserPrincipal(user);
        }

    /**
     * Gets the user data by ID.
     *
     * @param userId User's unique identifier
     * @return User details
     */
    public UserDetails loadUserById(Long userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        User user = userOptional.orElseThrow(
                () -> new UsernameNotFoundException("User not found : " + userId));

        return new UserPrincipal(user);
    }
}
