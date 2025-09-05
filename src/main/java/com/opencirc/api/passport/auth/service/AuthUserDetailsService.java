package com.opencirc.api.passport.auth.service;

import com.opencirc.api.passport.auth.principal.UserPrincipal;
import com.opencirc.api.passport.dao.UserRepository;
import com.opencirc.api.passport.model.User;
import com.opencirc.api.passport.util.StringUtil;
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
   * This method is required to be implemented by UserDetailsService interface. Loads a user by
   * their email. Although the interface defines the method as {@code loadUserByUsername}, our
   * application uses email as the unique identifier for users.
   *
   * @param email the user's email address
   * @return the details of the user
   * @throws UsernameNotFoundException if no user is found with the given email
   */
  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    if (email != null) {
      email = StringUtil.normalizeEmail(email);
    }
    User user = userRepository.findByEmail(email);
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
