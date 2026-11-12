package com.opencirc.api.passport.auth.principal;

import com.opencirc.api.passport.model.AccessGroupPermissions;
import java.security.Principal;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Authenticated machine credential. Carries the project the key is scoped to and the permission
 * profile of its access group so authorization checks need no further lookups.
 */
@Getter
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

  /**
   * Secret component of the token the request was authenticated with, kept only for signing
   * validation tags during this request. Null when the actor was built without a token.
   */
  @ToString.Exclude @EqualsAndHashCode.Exclude private final String rawSecret;

  /** Creates a principal without the token secret. */
  public ApiKeyPrincipal(
      String keyId,
      String keyName,
      String accessGroupId,
      String projectId,
      AccessGroupPermissions permissions) {
    this(keyId, keyName, accessGroupId, projectId, permissions, null);
  }

  /** Creates a principal that can sign with the token secret. */
  public ApiKeyPrincipal(
      String keyId,
      String keyName,
      String accessGroupId,
      String projectId,
      AccessGroupPermissions permissions,
      String rawSecret) {
    this.keyId = keyId;
    this.keyName = keyName;
    this.accessGroupId = accessGroupId;
    this.projectId = projectId;
    this.permissions = permissions;
    this.rawSecret = rawSecret;
  }

  /** The principal name is the public key id. */
  @Override
  public String getName() {
    return keyId;
  }
}
