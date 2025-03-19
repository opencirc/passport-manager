package com.oc.api.passport.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.oc.api.passport.constants.AppConstants;
import com.oc.api.passport.exception.AuthenticationException;
import com.oc.api.passport.service.AuthUserDetailsService;
import com.oc.api.passport.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter to validate the JWT token.
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    /**
     * Injecting JwtService class.
     */
    @Autowired
    private JwtService jwtService;

    /**
     * Injecting ApplicationContext class.
     */
    @Autowired
    private ApplicationContext context;

    /**
     * Filters the http request and validates the jwt token.
     *
     * @param request
     * @param response
     * @param filterChain
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(AppConstants.JWT_AUTH);
        String token = null;
        String username = null;

        if (authHeader != null
                && authHeader.startsWith(AppConstants.JWT_BEARER)) {
            token = authHeader.substring(AppConstants.JWT_BEARER_LENGTH);
            username = jwtService.extractUsername(token);
        }

        if (username != null && SecurityContextHolder.getContext()
                .getAuthentication() == null) {
            UserDetails userDetails = context
                    .getBean(AuthUserDetailsService.class)
                    .loadUserByUsername(username);
            if (!jwtService.validateToken(token, userDetails)) {
                throw new AuthenticationException(
                        AppConstants.ERR_INVALID_TOKEN);
            }
            UsernamePasswordAuthenticationToken authToken = new
                    UsernamePasswordAuthenticationToken(userDetails, null,
                            userDetails.getAuthorities());
            authToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }
}
