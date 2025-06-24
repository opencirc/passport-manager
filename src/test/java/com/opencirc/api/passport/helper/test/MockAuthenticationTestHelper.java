package com.opencirc.api.passport.helper.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.UUID;

import com.opencirc.api.passport.model.User;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.opencirc.api.passport.auth.principal.UserPrincipal;
import com.opencirc.api.passport.auth.service.AuthUserDetailsService;
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
        UUID existingUserId = UUID.fromString("87510a3c-4357-47bc-80a1-9ed02285fbae");
        User mockUser = new User();
        mockUser.setId(existingUserId);
        mockUser.setUsername(TestConstants.TEST_USERNAME_1);
        mockUser.setPassword("user1password");
        mockUser.setEmail("user1@example.com");
        mockUser.setActive(true);
        mockUser.setRole(User.Role.USER);

        UserPrincipal mockUserPrincipal = new UserPrincipal(mockUser);

        UsernamePasswordAuthenticationToken mockAuthenticationToken =
                new UsernamePasswordAuthenticationToken(
                mockUserPrincipal, null, mockUserPrincipal.getAuthorities());

        when(authUserDetailsService.loadUserById("87510a3c-4357-47bc-80a1-9ed02285fbae")).thenReturn(mockUserPrincipal);

        when(authUserDetailsService.loadUserById("999"))
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
