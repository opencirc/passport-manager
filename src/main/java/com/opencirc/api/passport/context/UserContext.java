package com.opencirc.api.passport.context;

import java.util.UUID;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.opencirc.api.passport.auth.principal.UserPrincipal;
import com.opencirc.api.passport.config.AppProperties;
import com.opencirc.api.passport.dto.UserDto;
import com.opencirc.api.passport.model.User.Role;

@Component
public class UserContext {

    /**
     * Injected application properties.
     */
    private final AppProperties appProperties;


    /**
     * Constructor.
     * @param appProperties application properties
     */
    public UserContext(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    /**
     * Gets the user information of the currently authenticated user.
     *
     * @return the instance of UserDto
     */
    public UserDto getCurrentUser() {
        Authentication authentication = SecurityContextHolder
                .getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            Object principal = authentication.getPrincipal();

            if (principal instanceof UserPrincipal userPrincipal) {
                UserDto userDto = new UserDto();
                if (userPrincipal.getUserId() == null) {
                    userDto.setId(null);
                    userDto.setFullName(appProperties.getSystemAdminName());
                    userDto.setEmail(appProperties.getSystemAdminEmail());
                    userDto.setRole(Role.ADMIN);
                } else {
                    userDto.setId(UUID.fromString(userPrincipal.getUserId()));
                    userDto.setFullName(userPrincipal.getFullName());
                    userDto.setEmail(userPrincipal.getEmail());
                    userDto.setRole(userPrincipal.getRole());
                }
                userDto.setActive(true);
                return userDto;
            }
        }
        throw new AuthenticationCredentialsNotFoundException("No "
                + "authenticated principal is available");
    }
}
