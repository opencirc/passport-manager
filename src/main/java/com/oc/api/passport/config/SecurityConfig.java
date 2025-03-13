package com.oc.api.passport.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@EnableWebSecurity
public class SecurityConfig {


	@Autowired
	private JwtFilter jwtFilter;

	@Autowired
	private UserDetailsService userDetailsService;

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();

	}

	@Bean
	public AuthenticationProvider authenticationProvider() throws Exception {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setPasswordEncoder(new BCryptPasswordEncoder(12));
		provider.setUserDetailsService(userDetailsService);
		return provider;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.requiresChannel(channel -> channel.anyRequest().requiresSecure()).csrf(customizer -> customizer.disable())
				.authorizeHttpRequests(request -> request.requestMatchers("/api/auth/register", "/api/auth/login")
						.permitAll().anyRequest().authenticated())
				// .formLogin(Customizer.withDefaults()) // remove comment this to test api in browser
				.httpBasic(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);


		return http.build();
	}
}
