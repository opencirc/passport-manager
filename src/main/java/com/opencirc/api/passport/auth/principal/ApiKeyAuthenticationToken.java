package com.opencirc.api.passport.auth.principal;

import com.opencirc.api.passport.constants.AppConstants;
import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/** Spring Security authentication for a request authenticated with an API key. */
public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

  private static final long serialVersionUID = 1L;

  private final ApiKeyPrincipal principal;

  /** Creates an already authenticated token for the given principal. */
  public ApiKeyAuthenticationToken(ApiKeyPrincipal principal) {
    super(List.of(new SimpleGrantedAuthority(AppConstants.AUTHORITY_API_KEY)));
    this.principal = principal;
    setAuthenticated(true);
  }

  @Override
  public Object getCredentials() {
    return null;
  }

  @Override
  public ApiKeyPrincipal getPrincipal() {
    return principal;
  }
}
