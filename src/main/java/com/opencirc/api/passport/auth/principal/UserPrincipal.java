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

  /** User name. */
  private final String username;

  /** Indicator for the User account is active or not. */
  private final boolean isActive;

  /** User Authorities. */
  private final Collection<? extends GrantedAuthority> authorities;

  /** User Email. */
  private final String email;

  /** User password. */
  private final String password;

  /**
   * Injecting UserPrincipal class.
   *
   * @param user
   */
  public UserPrincipal(User user) {
    Objects.requireNonNull(user, "User cannot be null");
    this.userId = String.valueOf(user.getId());
    this.username = user.getUsername();
    this.email = user.getEmail();
    this.password = user.getPassword();
    this.isActive = user.isActive();
    this.authorities = Collections.singleton(new SimpleGrantedAuthority(user.getRole().getValue()));
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
   * @return password
   */
  public String getEmail() {
    return email;
  }

  /**
   * method to get Username.
   *
   * @return Username
   */
  @Override
  public String getUsername() {
    return username;
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
    return true;
  }
}
