package com.opencirc.api.passport.context;

import java.util.UUID;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
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

        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            Object principal = authentication.getPrincipal();

            if (principal instanceof UserPrincipal userPrincipal) {
                try {
                    UserDto userDto = new UserDto();
                    userDto.setId(UUID.fromString(userPrincipal.getUserId()));
                    userDto.setFullName(userPrincipal.getFullName());
                    userDto.setEmail(userPrincipal.getEmail());
                    userDto.setRole(userPrincipal.getRole());
                    userDto.setActive(true);
                    return userDto;
                } catch (IllegalArgumentException e) {
                    throw new AuthenticationCredentialsNotFoundException(
                            "Invalid user ID format in authenticated principal", e);
                }
            }
        }
        throw new AuthenticationCredentialsNotFoundException(
                "No " + "authenticated principal is available");
    }
}
