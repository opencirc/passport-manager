package com.opencirc.api.passport.auth.principal;

import com.opencirc.api.passport.model.AccessGroupPermissions;
import java.security.Principal;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Authenticated machine credential. Carries the project the key is scoped to and the permission
 * profile of its access group so authorization checks need no further lookups.
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class ApiKeyPrincipal implements Principal {

  /** Public key id (the primary key of the api_keys row). */
  private final String keyId;

  /** Display name of the key. */
  private final String keyName;

  /** Access group that issued the key. */
  private final String accessGroupId;

  /** Project the key operates in. */
  private final String projectId;

  /** Permission profile of the access group. */
  private final AccessGroupPermissions permissions;

  /** The principal name is the public key id. */
  @Override
  public String getName() {
    return keyId;
  }
}
