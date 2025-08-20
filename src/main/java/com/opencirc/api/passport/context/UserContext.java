package com.opencirc.api.passport.context;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.opencirc.api.passport.auth.principal.UserPrincipal;

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
}
