package com.opencirc.api.passport.helper.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.opencirc.api.passport.auth.principal.AdminPrincipal;
import com.opencirc.api.passport.auth.service.AdminDetailsService;
import com.opencirc.api.passport.model.Admin;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/** Helper class to mock administrator authentication. */
public class MockAuthenticationTestHelper {

  public static final String ADMIN_ID = "87510a3c-4357-47bc-80a1-9ed02285fbae";

  /** Mocks administrator details and the authentication manager. */
  public void mockUserDetails(
      AdminDetailsService adminDetailsService, AuthenticationManager authenticationManager) {
    Admin mockAdmin = new Admin();
    mockAdmin.setId(ADMIN_ID);
    mockAdmin.setFirstName("User");
    mockAdmin.setLastName("One");
    mockAdmin.setPassword("user1password");
    mockAdmin.setEmail("user1@test.com");
    mockAdmin.setActive(true);

    AdminPrincipal mockPrincipal = new AdminPrincipal(mockAdmin);

    UsernamePasswordAuthenticationToken mockAuthenticationToken =
        new UsernamePasswordAuthenticationToken(
            mockPrincipal, null, mockPrincipal.getAuthorities());

    when(adminDetailsService.loadUserById(ADMIN_ID)).thenReturn(mockPrincipal);

    when(adminDetailsService.loadUserById("999"))
        .thenThrow(new UsernameNotFoundException("Admin not found"));

    when(authenticationManager.authenticate(any()))
        .thenAnswer(
            invocation -> {
              UsernamePasswordAuthenticationToken authRequest = invocation.getArgument(0);
              if (authRequest.getName().equalsIgnoreCase("user")
                  && authRequest.getCredentials().equals("user1password")) {
                return mockAuthenticationToken;
              } else {
                throw new BadCredentialsException("Invalid credentials");
              }
            });
  }
}
