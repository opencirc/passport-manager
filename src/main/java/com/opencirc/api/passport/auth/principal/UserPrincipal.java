package com.opencirc.api.passport.auth.principal;

import com.opencirc.api.passport.model.User;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UserPrincipal implements UserDetails {

  /** serial version. */
  private static final long serialVersionUID = 1L;

  /** User Id. */
  private final String userId;

  /** User Email. */
  private final String email;

  /** User password. */
  private final String password;

  /** Indicates whether the user account is enabled (active) or disabled. */
  private final boolean enabled;

  /**
   * Injecting UserPrincipal class.
   *
   * @param user
   */
  public UserPrincipal(User user) {
    Objects.requireNonNull(user, "User cannot be null");
    this.userId = String.valueOf(user.getId());
    this.email = user.getEmail();
    this.password = user.getPassword();
    this.enabled = user.isActive();
  }

  /**
   * method to get authorities.
   *
   * @return authorities
   */
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.singleton(new SimpleGrantedAuthority("USER"));
  }

  /**
   * method to get password.
   *
   * @return password
   */
  @Override
  public String getPassword() {
    return password;
  }

  /**
   * method to get userId.
   *
   * @return userId
   */
  public String getUserId() {
    return userId;
  }

  /**
   * Email getter.
   *
   * @return email
   */
  public String getEmail() {
    return email;
  }

  /**
   * This method is required by UserDetails interface. Since this application identifies users by
   * their email address, the email is returned here instead of a username.
   *
   * @return the user's email
   */
  @Override
  public String getUsername() {
    return email;
  }

  /**
   * Gets the status of the account is expired or not.
   *
   * @return status
   */
  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  /**
   * Gets the status of the account is locked or not.
   *
   * @return status
   */
  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  /**
   * Gets the status of the credentials is expired or not.
   *
   * @return status
   */
  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  /**
   * Gets the status of the account is enabled or not.
   *
   * @return status
   */
  @Override
  public boolean isEnabled() {
    return enabled;
  }
}
