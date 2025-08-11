package com.opencirc.api.passport.config;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
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

  /** Injecting JwtFilter class. */
  @Autowired private JwtFilter jwtFilter;

  /** Injecting UserDetailsService class. */
  @Autowired private UserDetailsService userDetailsService;

  /** Injecting Properties class. */
  @Autowired private AppProperties properties;

  /**
   * Bean to get authenticationManager.
   *
   * @param config the authentication configuration
   * @return the instance of authentication manager
   */
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }

  /**
   * Bean of authenticationProvider.
   */
  @Bean
  public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setPasswordEncoder(new BCryptPasswordEncoder(12));
    provider.setUserDetailsService(userDetailsService);
    return provider;
  }

  /**
   * Configures the security filter chain for the given http security object.
   *
   * @param http The http security object
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
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
          Arrays.asList("http://localhost:3001", "http://localhost:3002"));
      corsConfiguration.setAllowedMethods(Collections.singletonList("*"));
      corsConfiguration.setAllowedHeaders(Collections.singletonList("*"));
      corsConfiguration.setMaxAge(Duration.ofMinutes(25));
      return corsConfiguration;
    };
  }
}
