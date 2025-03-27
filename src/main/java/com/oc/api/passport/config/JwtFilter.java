package com.oc.api.passport.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.oc.api.passport.constants.AppConstants;
import com.oc.api.passport.exception.AuthenticationException;
import com.oc.api.passport.service.AuthUserDetailsService;
import com.oc.api.passport.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
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
     * Injecting Properties class.
     */
    @Autowired
    private Properties properties;

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
        String accessToken = null;
        String refreshToken = null;
        String username = null;

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    accessToken = cookie.getValue();

                } else if ("refresh_token".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                }
            }
        }
        if (accessToken == null && refreshToken != null) {
            try {
                accessToken = jwtService
                        .generateAccessTokenUsingRefreshToken(refreshToken);
                ResponseCookie accessCookie = ResponseCookie
                        .from("access_token", accessToken)
                        .httpOnly(true)
                        .secure(false)
                        .path("/").maxAge(properties.getAccessTokenExpiryTime())
                        .sameSite("Lax").build();
                response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
            } catch (AuthenticationException e) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                        "Refresh token invalid: " + e.getMessage());
                return;
            }
        }

        if (accessToken == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            username = jwtService.extractUsername(accessToken);
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid or expired token");
            return;
        }

        if (username != null && SecurityContextHolder.getContext()
                .getAuthentication() == null) {
            try {

                UserDetails userDetails = context
                        .getBean(AuthUserDetailsService.class)
                        .loadUserByUsername(username);

                if (!jwtService.validateToken(accessToken, userDetails)) {
                    throw new AuthenticationException(
                            AppConstants.ERR_INVALID_TOKEN);
                }
                UsernamePasswordAuthenticationToken authToken = new
                        UsernamePasswordAuthenticationToken(userDetails, null,
                                userDetails.getAuthorities());
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

            } catch (UsernameNotFoundException ex) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "User not found");
                return;
            } catch (AuthenticationException ex) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                        "Invalid token: " + ex.getMessage());
                return;
            } catch (Exception ex) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Internal server error: " + ex.getMessage());
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
