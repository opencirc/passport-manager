package com.opencirc.api.passport.config;

import com.opencirc.api.passport.auth.service.AdminDetailsService;
import com.opencirc.api.passport.constants.AppConstants;
import com.opencirc.api.passport.service.ApiKeyService;
import com.opencirc.api.passport.service.JwtService;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

/**
 * Spring security configuration. Administrative endpoints require an admin session; passport
 * endpoints require a machine API key; reference lookups accept either.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /** Injecting Properties class. */
  private final AppProperties properties;

  /** Constructor. */
  public SecurityConfig(AppProperties properties) {
    this.properties = properties;
  }

  /** JwtFilter bean. */
  @Bean
  public JwtFilter jwtFilter(
      JwtService jwtService,
      AppProperties properties,
      ApiKeyService apiKeyService,
      AdminDetailsService adminDetailsService,
      ObjectMapper objectMapper) {
    return new JwtFilter(jwtService, properties, apiKeyService, adminDetailsService, objectMapper);
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
                    .requestMatchers("/api/admin/**")
                    .hasAuthority(AppConstants.AUTHORITY_ADMIN)
                    .requestMatchers("/api/passport/**", "/api/audit-logs/**")
                    .hasAuthority(AppConstants.AUTHORITY_API_KEY)
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            handling ->
                handling.authenticationEntryPoint(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
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
