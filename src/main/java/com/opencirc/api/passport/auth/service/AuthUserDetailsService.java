package com.opencirc.api.passport.auth.service;

import com.opencirc.api.passport.auth.principal.UserPrincipal;
import com.opencirc.api.passport.dao.UserRepository;
import com.opencirc.api.passport.model.User;
import com.opencirc.api.passport.util.StringUtil;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthUserDetailsService implements UserDetailsService {

  @Autowired private UserRepository userRepository;

  /**
   * This method is required to be implemented by UserDetailsService interface. Loads a user by
   * their email. Although the interface defines the method as {@code loadUserByUsername}, our
   * application uses email as the unique identifier for users.
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

  /** Gets the user data by ID. */
  public UserDetails loadUserById(String userId) {
    Optional<User> userOptional = userRepository.findById(userId);
    User user =
        userOptional.orElseThrow(() -> new UsernameNotFoundException("User not found : " + userId));

    return new UserPrincipal(user);
  }
}
