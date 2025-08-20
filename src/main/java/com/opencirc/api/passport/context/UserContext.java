package com.opencirc.api.passport.context;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.opencirc.api.passport.auth.principal.UserPrincipal;

@Component
public class UserContext {

    /**
     * Gets the userId of the currently authenticated user.
     *
     * @return the userId or null if not authenticated
     */
    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserPrincipal) {
                return ((UserPrincipal) principal).getUserId();
            } else if (principal instanceof String) {
                return (String) principal;
            }
        }
        return null;
    }
}
