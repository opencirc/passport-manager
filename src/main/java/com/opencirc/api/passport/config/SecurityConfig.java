package com.opencirc.api.passport.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencirc.api.passport.auth.service.AuthUserDetailsService;
import com.opencirc.api.passport.constants.AppConstants;
import com.opencirc.api.passport.service.ApiKeyService;
import com.opencirc.api.passport.service.JwtService;
import com.opencirc.api.passport.service.PasswordService;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

/** Spring security configuration. */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /** Injecting UserDetailsService class. */
  private UserDetailsService userDetailsService;

  /** Injecting Properties class. */
  private AppProperties properties;

  /** Constructor. */
  public SecurityConfig(UserDetailsService userDetailsService, AppProperties properties) {
    this.userDetailsService = userDetailsService;
    this.properties = properties;
  }

  /** JwtFilter bean. */
  @Bean
  public JwtFilter jwtFilter(
      JwtService jwtService,
      AppProperties properties,
      ApiKeyService apiKeyService,
      PasswordService passwordService,
      AuthUserDetailsService authUserDetailsService,
      ObjectMapper objectMapper) {
    return new JwtFilter(
        jwtService,
        properties,
        apiKeyService,
        passwordService,
        authUserDetailsService,
        objectMapper);
  }

  /** PasswordEncoder bean. */
  @Bean
  public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(AppConstants.PASSWORD_STRENGTH);
  }

  /** Bean to get authenticationManager. */
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }

  /** Bean of authenticationProvider. */
  @Bean
  public AuthenticationProvider authenticationProvider(
      BCryptPasswordEncoder bcryptPasswordEncoder) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setPasswordEncoder(bcryptPasswordEncoder);
    provider.setUserDetailsService(userDetailsService);
    return provider;
  }

  /** Configuring security filter chain. */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtFilter jwtFilter)
      throws Exception {
    http.csrf(customizer -> customizer.disable())
        .cors(corsCustomizer -> corsCustomizer.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(
            request ->
                request
                    .requestMatchers(
                        properties.getRegisterUrl(),
                        properties.getLoginUrl(),
                        "/swagger-ui/**",
                        "/v3/api-docs/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .httpBasic(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  private CorsConfigurationSource corsConfigurationSource() {
    return request -> {
      CorsConfiguration corsConfiguration = new CorsConfiguration();
      corsConfiguration.setAllowCredentials(true);
      corsConfiguration.setAllowedOrigins(
          Arrays.asList("http://localhost:3001", "http://localhost:3002", "http://localhost:3000"));
      corsConfiguration.setAllowedMethods(Collections.singletonList("*"));
      corsConfiguration.setAllowedHeaders(Collections.singletonList("*"));
      corsConfiguration.setMaxAge(Duration.ofMinutes(AppConstants.CORS_MAX_AGE));
      return corsConfiguration;
    };
  }
}
