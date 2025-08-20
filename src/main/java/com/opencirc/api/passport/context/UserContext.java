package com.opencirc.api.passport.context;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.opencirc.api.passport.auth.principal.UserPrincipal;
import com.opencirc.api.passport.dto.CreatedByDto;

@Component
public class UserContext {

    /**
     * Gets the userId of the currently authenticated user.
     *
     * @return the userId
     * @throws AuthenticationCredentialsNotFoundException, when unauthenticated
     */
    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserPrincipal) {
                return ((UserPrincipal) principal).getUserId();
            }
        }
        throw new AuthenticationCredentialsNotFoundException(
                "No authenticated principal with a userId is available");
    }


    /**
     * Gets the user information (email and full name) of the currently
     * authenticated user.
     *
     * @return the instance of createdByDto
     */
    public CreatedByDto getCurrentUserInformation() {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();
        CreatedByDto createdByDto = null;
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            createdByDto = new CreatedByDto();
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserPrincipal) {
                createdByDto.setFullName(((UserPrincipal) principal).getFullName());
                createdByDto.setEmail(((UserPrincipal) principal).getEmail());
                return createdByDto;
            }
        }
        throw new AuthenticationCredentialsNotFoundException(
                "No authenticated principle is available");
    }
}
