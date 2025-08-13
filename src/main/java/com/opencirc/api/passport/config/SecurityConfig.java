package com.opencirc.api.passport.config;

import com.opencirc.api.passport.constants.AppConstants;
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

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

/**
 * Spring security configuration.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Injecting JwtFilter class.
     */
    @Autowired
    private JwtFilter jwtFilter;

    /**
     * Injecting UserDetailsService class.
     */
    @Autowired
    private UserDetailsService userDetailsService;

    /**
     * Injecting Properties class.
     */
    @Autowired
    private AppProperties properties;

    /**
     * Bean to get authenticationManager.
     * @param config
     * @return the instance of authentication manager
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();

    }

    /**
     * Bean of authenticationProvider.
     * @return the instance of authentication provider
     */
    @Bean
    public AuthenticationProvider authenticationProvider() throws Exception {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(new BCryptPasswordEncoder(AppConstants.TWELVE));
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }

    /**
     * Configuring security filter chain.
     * @param http
     * @return instance of security filter chain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        http.csrf(customizer -> customizer.disable())
        .cors(corsCustomizer -> corsCustomizer
                .configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(request -> request
                        .requestMatchers(properties.getRegisterUrl(),
                                properties.getLoginUrl(), "/swagger-ui/**",
                                "/v3/api-docs/**")
                        .permitAll().anyRequest().authenticated())
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    private CorsConfigurationSource corsConfigurationSource() {
        return request -> {
            CorsConfiguration corsConfiguration = new CorsConfiguration();
            corsConfiguration.setAllowCredentials(true);
            corsConfiguration.setAllowedOrigins(Arrays.asList("http://localhost:3001",
                    "http://localhost:3002"));
            corsConfiguration.setAllowedMethods(Collections.singletonList("*"));
            corsConfiguration.setAllowedHeaders(Collections.singletonList("*"));
            corsConfiguration.setMaxAge(Duration.ofMinutes(AppConstants.TWENTY_FIVE));
            return corsConfiguration;
        };
    }
}
