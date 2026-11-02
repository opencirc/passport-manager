package com.opencirc.api.passport.context;

import com.opencirc.api.passport.auth.principal.AdminPrincipal;
import com.opencirc.api.passport.auth.principal.ApiKeyPrincipal;
import com.opencirc.api.passport.dto.AdminDto;
import java.util.Optional;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Resolves the actor of the current request: an administrator, a machine credential, or none. */
@Component
public class ActorContext {

  /** The authenticated administrator, if the request carries an admin session. */
  public Optional<AdminPrincipal> currentAdmin() {
    Object principal = principal();
    if (principal instanceof AdminPrincipal admin) {
      return Optional.of(admin);
    }
    return Optional.empty();
  }

  /** The authenticated machine credential, if the request carries an API key. */
  public Optional<ApiKeyPrincipal> currentApiKey() {
    Object principal = principal();
    if (principal instanceof ApiKeyPrincipal apiKey) {
      return Optional.of(apiKey);
    }
    return Optional.empty();
  }

  /** The authenticated administrator; fails when the request is not an admin session. */
  public AdminDto requireAdmin() {
    AdminPrincipal admin =
        currentAdmin()
            .orElseThrow(
                () ->
                    new AuthenticationCredentialsNotFoundException(
                        "No authenticated administrator is available"));
    if (admin.getAdminId() == null || admin.getAdminId().isBlank()) {
      throw new AuthenticationCredentialsNotFoundException(
          "Missing admin ID in authenticated principal");
    }
    return AdminDto.from(admin);
  }

  /** The authenticated machine credential; fails when the request carries no API key. */
  public ApiKeyPrincipal requireApiKey() {
    return currentApiKey()
        .orElseThrow(
            () ->
                new AuthenticationCredentialsNotFoundException(
                    "No authenticated API key is available"));
  }

  private Object principal() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null ? authentication.getPrincipal() : null;
  }
}
