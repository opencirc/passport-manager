package com.opencirc.api.passport.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.opencirc.api.passport.constants.AppConstants;
import com.opencirc.api.passport.exception.AuthenticationException;
import com.opencirc.api.passport.auth.service.AuthUserDetailsService;
import com.opencirc.api.passport.service.JwtService;

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
    private AppProperties properties;

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

        try {
            String accessToken = extractTokenFromCookies(request, "access_token");
            String refreshToken = extractTokenFromCookies(request, "refresh_token");

            if (accessToken == null && refreshToken != null) {
                accessToken = handleRefreshToken(response, refreshToken);
                if (accessToken == null) {
                    sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                            "Invalid refresh token");
                    return;
                }
            }

            if (accessToken == null) {
                filterChain.doFilter(request, response);
                return;
            }

            String userId = extractUserIdFromToken(accessToken, response);
            if (userId == null) {
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                authenticateUser(request, response, accessToken, userId);
            }

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            System.out.println("Exception caused " +e.getMessage());
        }
    }

    /**
     * Extracts the token from cookies.
     * @param request
     * @param tokenName
     * @return token
     */
    private String extractTokenFromCookies(HttpServletRequest request,
            String tokenName) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (tokenName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Handles refresh token logic and generates a new access token.
     * @param response
     * @param refreshToken
     * @return new access token
     */
    private String handleRefreshToken(HttpServletResponse response,
            String refreshToken) {
        try {
            String newAccessToken = jwtService
                    .generateAccessTokenUsingRefreshToken(refreshToken);
            jwtService.generateTokenCookie(response, newAccessToken,
                    AppConstants.COOKIE_ACCESS_TOKEN,
                    properties.getAccessTokenExpiryTime());
            return newAccessToken;
        } catch (AuthenticationException e) {
            return null;
        }
    }

    /**
     * Extracts the user ID from the token.
     * @param token
     * @param response
     * @return userId
     */
    private String extractUserIdFromToken(String token, HttpServletResponse response)
            throws IOException {
        try {
            return jwtService.extractUserId(token);
        } catch (Exception e) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid or expired token");
            return null;
        }
    }

    /**
     * Authenticates the user using extracted user ID.
     * @param request
     * @param response
     * @param accessToken
     * @param userId
     */
    private void authenticateUser(HttpServletRequest request,
            HttpServletResponse response, String accessToken, String userId)
            throws IOException {
        try {
            AuthUserDetailsService authUserDetailsService = context
                    .getBean(AuthUserDetailsService.class);
            UserDetails userDetails = authUserDetailsService.loadUserById(userId);

            if (!jwtService.validateToken(accessToken, userDetails)) {
                throw new AuthenticationException(AppConstants.ERR_INVALID_TOKEN);
            }

            UsernamePasswordAuthenticationToken authToken = new
                    UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authToken);
        } catch (UsernameNotFoundException e) {
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND,
                    "User not found");
        } catch (AuthenticationException e) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid token: " + e.getMessage());
        } catch (Exception e) {
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Internal server error: " + e.getMessage());
        }
    }

    /**
     * Sends an error response.
     * @param response
     * @param status
     * @param message
     */
    private void sendErrorResponse(HttpServletResponse response, int status,
            String message) throws IOException {
        response.sendError(status, message);
    }

}
