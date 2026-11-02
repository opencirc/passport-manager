package com.opencirc.api.passport.auth.principal;

import com.opencirc.api.passport.constants.AppConstants;
import com.opencirc.api.passport.model.Admin;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Authenticated administrator. Every administrator carries the same authority. */
public class AdminPrincipal implements UserDetails {

  /** serial version. */
  private static final long serialVersionUID = 1L;

  /** Admin Id. */
  private final String adminId;

  /** Admin Email. */
  private final String email;

  /** Admin full name. */
  private final String fullName;

  /** Admin password. */
  private final String password;

  /** Indicates whether the account is enabled (active) or disabled. */
  private final boolean enabled;

  /** Constructs an AdminPrincipal from an Admin entity. */
  public AdminPrincipal(Admin admin) {
    Objects.requireNonNull(admin, "Admin cannot be null");
    this.adminId = admin.getId();
    this.email = admin.getEmail();
    this.fullName = admin.getFullName();
    this.password = admin.getPassword();
    this.enabled = admin.isActive();
  }

  /** Get authorities. */
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.singleton(new SimpleGrantedAuthority(AppConstants.AUTHORITY_ADMIN));
  }

  /** Get the password. */
  @Override
  public String getPassword() {
    return password;
  }

  /** Get adminId. */
  public String getAdminId() {
    return adminId;
  }

  /** Get email. */
  public String getEmail() {
    return email;
  }

  /** Gets fullName of the administrator. */
  public String getFullName() {
    return fullName;
  }

  /**
   * This method is required by UserDetails interface. Since this application identifies
   * administrators by their email address, the email is returned here instead of a username.
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
}
