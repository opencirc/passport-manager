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

import com.oc.api.passport.constants.AppConstants;

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
    private Properties properties;

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
        provider.setPasswordEncoder(new BCryptPasswordEncoder(AppConstants.NUM_TWELVE));
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
                .authorizeHttpRequests(request -> request
                        .requestMatchers(properties.getRegisterUrl(),
                                properties.getLoginUrl(), "/swagger-ui/index.html")
                        .permitAll().anyRequest().authenticated())
                // .formLogin(Customizer.withDefaults()) // remove comment this
                // to test api in browser
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
