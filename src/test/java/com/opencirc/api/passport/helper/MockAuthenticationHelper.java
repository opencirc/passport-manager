package com.opencirc.api.passport.helper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.oc.api.passport.service.AuthUserDetailsService;

public class MockAuthenticationHelper {
	

	
	
	public void mockUserDetailsDB(AuthUserDetailsService authUserDetailsService, AuthenticationManager authenticationManager) {
		UserDetails mockUser = new User("user1", 
                "$2a$10$UyTGFBClronaghiklkmnxunwi6iK2st/LtInbJNq8hQSBVsXt/yRa",
                Collections.singletonList(new SimpleGrantedAuthority("ADMIN")));

		when(authUserDetailsService.loadUserByUsername("user1")).thenReturn(mockUser);
		when(authUserDetailsService.loadUserByUsername("user1d"))
		    .thenThrow(new UsernameNotFoundException("User not found"));
     

		when(authenticationManager.authenticate(any()))
	    .thenAnswer(invocation -> {
	        UsernamePasswordAuthenticationToken authRequest = invocation.getArgument(0);
	        if (authRequest.getName().equals("user1")) {
	            return new UsernamePasswordAuthenticationToken(mockUser, null, mockUser.getAuthorities());
	        } else {
	            throw new BadCredentialsException("Invalid credentials");
	        }
	    });
	}
	
}
