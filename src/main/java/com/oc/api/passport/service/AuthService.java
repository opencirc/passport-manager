package com.oc.api.passport.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.oc.api.passport.config.Properties;
import com.oc.api.passport.dao.UserRepository;
import com.oc.api.passport.dto.UserDto;
import com.oc.api.passport.exception.AuthenticationException;

@Service
public class AuthService {

	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private Properties properties;

	private BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder(12);

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	JwtService jwtService;

	@Autowired
	AuthUserDetailsService authUserDetailsService;

	public void register(UserDto user) throws AuthenticationException {
		if (userRepository.existsByUsername(user.getUsername())) {
			throw new AuthenticationException("Username already exists");
		}

		String encodedPassword = bCryptPasswordEncoder.encode(user.getPassword());
		user.setPassword(encodedPassword);

		userRepository.save(user);
	}

	public Map<String, String> verify(UserDto user) {

		Authentication authentication = authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));
		if (authentication.isAuthenticated()) {
			String accessToken = jwtService.generateToken(user.getUsername(),  properties.getAccessTokenExpiryTime());
			String refreshToken = jwtService.generateToken(user.getUsername(), properties.getRefreshTokenExpiryTime());
			Map<String, String> response = new HashMap<>();
			response.put("accessToken", accessToken);
			response.put("refreshToken", refreshToken);
			return response;
		}
		return null;

	}

	public Map<String, String> refreshToken(String token) throws AuthenticationException {
		String username = jwtService.extractUserName(token);
		UserDetails userDetails = authUserDetailsService.loadUserByUsername(username);
		if (!jwtService.validateToken(token, userDetails)) {
			throw new AuthenticationException("Invalid token");
		}

		String newAccessToken = jwtService.generateToken(username, properties.getAccessTokenExpiryTime());
		Map<String, String> response = new HashMap<>();
		response.put("accessToken", newAccessToken);

		return response;

	}

}
