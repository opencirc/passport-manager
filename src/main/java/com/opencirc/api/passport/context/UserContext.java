package com.opencirc.api.passport.context;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.opencirc.api.passport.auth.principal.UserPrincipal;
import com.opencirc.api.passport.dto.UserDto;

@Component
public class UserContext {


    /**
     * Gets the user information of the currently authenticated user.
     *
     * @return the instance of UserDto
     */
    public UserDto getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();

        if (authentication != null
                && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            String userId = userPrincipal.getUserId();
            if (userId == null || userId.isBlank()) {
                throw new AuthenticationCredentialsNotFoundException(
                        "Missing user ID in authenticated principal");
            }

            try {
                return UserDto.from(userPrincipal);
            } catch (IllegalArgumentException e) {
                throw new AuthenticationCredentialsNotFoundException(
                        "Invalid user ID format in authenticated principal", e);
            }
        }

        throw new AuthenticationCredentialsNotFoundException(
                "No authenticated principal is available");
    }
}
