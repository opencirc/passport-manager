package com.opencirc.api.passport.auth.principal;

import com.opencirc.api.passport.model.User;
import com.opencirc.api.passport.model.User.Role;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** UserPrincipal class. */
public class UserPrincipal implements UserDetails {

  /** serial version. */
  private static final long serialVersionUID = 1L;

  /** User Id. */
  private final String userId;

  /** User Email. */
  private final String email;

  /** User full name. */
  private final String fullName;

  /** User password. */
  private final String password;

  /** User role. */
  private final Role role;

  /** Indicates whether the user account is enabled (active) or disabled. */
  private final boolean enabled;

  /** Constructs a UserPrincipal from a User entity. */
  public UserPrincipal(User user) {
    Objects.requireNonNull(user, "User cannot be null");
    this.userId = (user.getId() != null) ? user.getId() : null;
    this.email = user.getEmail();
    this.fullName = user.getFullName();
    this.password = user.getPassword();
    this.enabled = user.isActive();
    this.role = user.getRole();
  }

  /** Get authorities. */
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    String authority = (role != null) ? role.getValue() : "USER";
    return Collections.singleton(new SimpleGrantedAuthority(authority));
  }

  /** Get the password. */
  @Override
  public String getPassword() {
    return password;
  }

  /** Get userId. */
  public String getUserId() {
    return userId;
  }

  /** Get email. */
  public String getEmail() {
    return email;
  }

  /** Gets fullName of the user. */
  public String getFullName() {
    return fullName;
  }

  /**
   * This method is required by UserDetails interface. Since this application identifies users by
   * their email address, the email is returned here instead of a username.
   */
  @Override
  public String getUsername() {
    return email;
  }

  /** Gets the status of the account is expired or not. */
  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  /** Gets the status of the account is locked or not. */
  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  /** Gets the status of the credentials is expired or not. */
  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  /** Gets the status of the account is enabled or not. */
  @Override
  public boolean isEnabled() {
    return enabled;
  }

  /** Gets the role of the user. */
  public Role getRole() {
    return role;
  }
}
