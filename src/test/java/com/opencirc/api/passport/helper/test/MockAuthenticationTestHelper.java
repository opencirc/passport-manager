package com.opencirc.api.passport.helper.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.oc.api.passport.model.User;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.oc.api.passport.auth.principal.UserPrincipal;
import com.oc.api.passport.auth.service.AuthUserDetailsService;
import com.opencirc.api.passport.constants.test.TestConstants;

public class MockAuthenticationTestHelper {

    /**
     * Mocks User data.
     *
     * @param authUserDetailsService
     * @param authenticationManager
     */
    public void mockUserDetailsDB(AuthUserDetailsService authUserDetailsService,
            AuthenticationManager authenticationManager) {

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername(TestConstants.TEST_USERNAME_1);
        mockUser.setPassword("user1password");
        mockUser.setEmail("user1@example.com");
        mockUser.setActive(true);
        mockUser.setRole(User.Role.ADMIN);

        UserPrincipal mockUserPrincipal = new UserPrincipal(mockUser);

        UsernamePasswordAuthenticationToken mockAuthenticationToken =
                new UsernamePasswordAuthenticationToken(
                mockUserPrincipal, null, mockUserPrincipal.getAuthorities());

        when(authUserDetailsService.loadUserById(1L)).thenReturn(mockUserPrincipal);

        when(authUserDetailsService.loadUserById(999L))
                .thenThrow(new UsernameNotFoundException("User not found"));

        when(authenticationManager.authenticate(any())).thenAnswer(invocation -> {
            UsernamePasswordAuthenticationToken authRequest = invocation.getArgument(0);
            if (authRequest.getName().equals(TestConstants.TEST_USERNAME_1)
                    && authRequest.getCredentials().equals("user1password")) {
                return mockAuthenticationToken;
            } else {
                throw new BadCredentialsException("Invalid credentials");
            }
        });
    }

}
