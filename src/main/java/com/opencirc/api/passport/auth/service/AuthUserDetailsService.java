package com.opencirc.api.passport.auth.service;

import com.opencirc.api.passport.auth.principal.UserPrincipal;
import com.opencirc.api.passport.dao.UserRepository;
import com.opencirc.api.passport.model.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthUserDetailsService implements UserDetailsService {

  /** Injecting UserRepository class. */
  @Autowired private UserRepository userRepository;

  /**
   * Gets the user data by name.
   *
   * @param username
   * @return the details of the user
   */
  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
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
  public UserDetails loadUserById(String userId) {
    Optional<User> userOptional = userRepository.findById(UUID.fromString(userId));
    User user =
        userOptional.orElseThrow(() -> new UsernameNotFoundException("User not found : " + userId));

    return new UserPrincipal(user);
  }
}
