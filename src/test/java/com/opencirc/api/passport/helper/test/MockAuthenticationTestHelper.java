package com.opencirc.api.passport.helper.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.oc.api.passport.dto.UserEntity;
import com.oc.api.passport.model.UserPrincipal;
import com.oc.api.passport.service.AuthUserDetailsService;
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

        UserEntity mockUserEntity = new UserEntity();
        mockUserEntity.setUserId(1L);
        mockUserEntity.setUsername(TestConstants.TEST_USERNAME_1);
        mockUserEntity.setPassword("user1password");
        mockUserEntity.setEmail("user1@example.com");
        mockUserEntity.setActive(true);
        mockUserEntity.setRole("ADMIN");

        UserPrincipal mockUserPrincipal = new UserPrincipal(mockUserEntity);

        UsernamePasswordAuthenticationToken mockAuthenticationToken = new UsernamePasswordAuthenticationToken(
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
