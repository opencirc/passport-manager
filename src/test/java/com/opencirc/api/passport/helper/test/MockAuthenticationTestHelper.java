package com.opencirc.api.passport.helper.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.opencirc.api.passport.auth.principal.UserPrincipal;
import com.opencirc.api.passport.auth.service.AuthUserDetailsService;
import com.opencirc.api.passport.constants.test.TestConstants;
import com.opencirc.api.passport.model.User;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class MockAuthenticationTestHelper {

  /**
   * Mocks User data.
   *
   * @param authUserDetailsService
   * @param authenticationManager
   */
  public void mockUserDetails(
      AuthUserDetailsService authUserDetailsService, AuthenticationManager authenticationManager) {
    User mockUser = new User();
    mockUser.setId("87510a3c-4357-47bc-80a1-9ed02285fbae");
    mockUser.setFirstName(TestConstants.TEST_FIRSTNAME);
    mockUser.setLastName(TestConstants.TEST_LASTNAME);
    mockUser.setPassword("Password123!");
    mockUser.setEmail("admin@test.com");
    mockUser.setActive(true);
    mockUser.setRole(User.Role.USER);

    UserPrincipal mockUserPrincipal = new UserPrincipal(mockUser);

    UsernamePasswordAuthenticationToken mockAuthenticationToken =
        new UsernamePasswordAuthenticationToken(
            mockUserPrincipal, null, mockUserPrincipal.getAuthorities());

    when(authUserDetailsService.loadUserById("87510a3c-4357-47bc-80a1-9ed02285fbae"))
        .thenReturn(mockUserPrincipal);

    when(authUserDetailsService.loadUserById("999"))
        .thenThrow(new UsernameNotFoundException("User not found"));

    when(authenticationManager.authenticate(any()))
        .thenAnswer(
            invocation -> {
              UsernamePasswordAuthenticationToken authRequest = invocation.getArgument(0);
              if (authRequest.getPrincipal().equals(TestConstants.TEST_EMAIL)
                  && authRequest.getCredentials().equals("Password123!")) {
                return mockAuthenticationToken;
              } else {
                throw new BadCredentialsException("Invalid credentials");
              }
            });
  }
}
