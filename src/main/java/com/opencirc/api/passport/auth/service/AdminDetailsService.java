package com.opencirc.api.passport.auth.service;

import com.opencirc.api.passport.auth.principal.AdminPrincipal;
import com.opencirc.api.passport.dao.AdminRepository;
import com.opencirc.api.passport.model.Admin;
import com.opencirc.api.passport.util.StringUtil;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Loads administrators for Spring Security. */
@Service
public class AdminDetailsService implements UserDetailsService {

  @Autowired private AdminRepository adminRepository;

  /**
   * Loads an administrator by email. Although the interface defines the method as {@code
   * loadUserByUsername}, this application uses email as the unique identifier.
   */
  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    if (email != null) {
      email = StringUtil.normalizeEmail(email);
    }
    Admin admin = adminRepository.findByEmail(email);
    if (admin == null) {
      throw new UsernameNotFoundException("Admin not found");
    }

    return new AdminPrincipal(admin);
  }

  /** Loads an administrator by ID. */
  public UserDetails loadUserById(String adminId) {
    Optional<Admin> adminOptional = adminRepository.findById(adminId);
    Admin admin =
        adminOptional.orElseThrow(
            () -> new UsernameNotFoundException("Admin not found : " + adminId));

    return new AdminPrincipal(admin);
  }
}
