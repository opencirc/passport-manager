package com.opencirc.api.passport.context;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.opencirc.api.passport.auth.principal.UserPrincipal;
import com.opencirc.api.passport.config.AppProperties;
import com.opencirc.api.passport.dto.CreatedByDto;

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
     * @return the instance of CreatedByDto
     */
    public CreatedByDto getCurrentUserInformation() {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserPrincipal) {
                CreatedByDto createdByDto = new CreatedByDto();
                if (((UserPrincipal) principal).getUserId() == null) {
                    createdByDto.setFullName(appProperties.getSystemAdminName());
                    createdByDto.setEmail(appProperties.getSystemAdminEmail());
                } else {
                    createdByDto.setFullName(((UserPrincipal) principal).getFullName());
                    createdByDto.setEmail(((UserPrincipal) principal).getEmail());
                }

                return createdByDto;
            }
        }
        throw new AuthenticationCredentialsNotFoundException(
                "No authenticated principal is available");
    }
}
