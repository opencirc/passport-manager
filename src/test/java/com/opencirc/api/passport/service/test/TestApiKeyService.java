package com.opencirc.api.passport.service.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.opencirc.api.passport.auth.principal.ApiKeyPrincipal;
import com.opencirc.api.passport.constants.AppConstants;
import com.opencirc.api.passport.dao.AccessGroupRepository;
import com.opencirc.api.passport.dao.ApiKeyRepository;
import com.opencirc.api.passport.dao.ProjectRepository;
import com.opencirc.api.passport.dto.GeneratedApiKeyDto;
import com.opencirc.api.passport.enums.ApiKeyStatus;
import com.opencirc.api.passport.enums.ProjectStatus;
import com.opencirc.api.passport.exception.AuthenticationException;
import com.opencirc.api.passport.model.AccessGroup;
import com.opencirc.api.passport.model.AccessGroupPermissions;
import com.opencirc.api.passport.model.ApiKey;
import com.opencirc.api.passport.model.Project;
import com.opencirc.api.passport.service.ApiKeyService;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
public class TestApiKeyService {

  @Mock private ApiKeyRepository apiKeyRepository;
  @Mock private AccessGroupRepository accessGroupRepository;
  @Mock private ProjectRepository projectRepository;

  private ApiKeyService apiKeyService;

  @BeforeEach
  void setUp() {
    apiKeyService = new ApiKeyService(apiKeyRepository, accessGroupRepository, projectRepository);
  }

  private ApiKey storedKey(String secret) {
    ApiKey key = new ApiKey();
    key.setId("abcDEF123456");
    key.setAccessGroupId("group-1");
    key.setSecretHash(ApiKeyService.hashSecret(secret));
    key.setName("Pipeline");
    key.setStatus(ApiKeyStatus.ACTIVE);
    return key;
  }

  private void stubActiveProject() {
    AccessGroup group = new AccessGroup();
    group.setId("group-1");
    group.setProjectId("project-1");
    group.setPermissions(AccessGroupPermissions.readOnly());
    when(accessGroupRepository.findById("group-1")).thenReturn(Optional.of(group));
    Project project = new Project();
    project.setId("project-1");
    project.setStatus(ProjectStatus.ACTIVE);
    when(projectRepository.findById("project-1")).thenReturn(Optional.of(project));
  }

  @Test
  void issuesStructuredTokenAndStoresOnlyTheHash() {
    when(accessGroupRepository.existsById("group-1")).thenReturn(true);
    when(apiKeyRepository.existsById(anyString())).thenReturn(false);
    when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(i -> i.getArgument(0));

    GeneratedApiKeyDto generated = apiKeyService.createApiKey("group-1", "Pipeline", null);

    assertTrue(generated.getToken().startsWith(AppConstants.API_KEY_PREFIX));
    var parsed = ApiKeyService.parseToken(generated.getToken()).orElseThrow();
    assertEquals(generated.getId(), parsed.keyId());
    assertEquals(AppConstants.API_KEY_ID_LENGTH, parsed.keyId().length());
    assertEquals(AppConstants.API_KEY_SECRET_LENGTH, parsed.secret().length());

    ArgumentCaptor<ApiKey> saved = ArgumentCaptor.forClass(ApiKey.class);
    org.mockito.Mockito.verify(apiKeyRepository).save(saved.capture());
    assertEquals(ApiKeyService.hashSecret(parsed.secret()), saved.getValue().getSecretHash());
    assertNotEquals(parsed.secret(), saved.getValue().getSecretHash());
    assertEquals(ApiKeyStatus.ACTIVE, saved.getValue().getStatus());
    assertEquals("active", generated.getStatus());
  }

  @Test
  void authenticatesValidTokenWithProjectScopeAndPermissions() {
    when(apiKeyRepository.findById("abcDEF123456")).thenReturn(Optional.of(storedKey("s3cret")));
    stubActiveProject();

    ApiKeyPrincipal principal =
        apiKeyService.authenticate(ApiKeyService.buildToken("abcDEF123456", "s3cret"));

    assertEquals("abcDEF123456", principal.getKeyId());
    assertEquals("Pipeline", principal.getKeyName());
    assertEquals("group-1", principal.getAccessGroupId());
    assertEquals("project-1", principal.getProjectId());
    assertFalse(principal.getPermissions().isCanCreate());
    assertTrue(principal.getPermissions().isCanRead());
  }

  @Test
  void rejectsWrongSecret() {
    when(apiKeyRepository.findById("abcDEF123456")).thenReturn(Optional.of(storedKey("s3cret")));

    assertThrows(
        AuthenticationException.class,
        () -> apiKeyService.authenticate(ApiKeyService.buildToken("abcDEF123456", "wrong")));
  }

  @Test
  void rejectsUnknownKeyId() {
    when(apiKeyRepository.findById("unknownKey01")).thenReturn(Optional.empty());

    assertThrows(
        AuthenticationException.class,
        () -> apiKeyService.authenticate(ApiKeyService.buildToken("unknownKey01", "s3cret")));
  }

  @Test
  void rejectsMalformedTokens() {
    assertThrows(AuthenticationException.class, () -> apiKeyService.authenticate("opc_live_"));
    assertThrows(AuthenticationException.class, () -> apiKeyService.authenticate("sk_abc_def"));
    assertThrows(AuthenticationException.class, () -> apiKeyService.authenticate("opc_live_abc_"));
    assertThrows(AuthenticationException.class, () -> apiKeyService.authenticate(null));
  }

  @Test
  void rejectsRevokedKey() {
    ApiKey key = storedKey("s3cret");
    key.setStatus(ApiKeyStatus.REVOKED);
    when(apiKeyRepository.findById("abcDEF123456")).thenReturn(Optional.of(key));

    AuthenticationException exception =
        assertThrows(
            AuthenticationException.class,
            () -> apiKeyService.authenticate(ApiKeyService.buildToken("abcDEF123456", "s3cret")));
    assertEquals("API key revoked", exception.getMessage());
  }

  @Test
  void rejectsExpiredKey() {
    ApiKey key = storedKey("s3cret");
    key.setExpirationTime(ZonedDateTime.now().minusMinutes(1));
    when(apiKeyRepository.findById("abcDEF123456")).thenReturn(Optional.of(key));

    AuthenticationException exception =
        assertThrows(
            AuthenticationException.class,
            () -> apiKeyService.authenticate(ApiKeyService.buildToken("abcDEF123456", "s3cret")));
    assertEquals("API key expired", exception.getMessage());
  }

  @Test
  void rejectsKeyOfArchivedProject() {
    when(apiKeyRepository.findById("abcDEF123456")).thenReturn(Optional.of(storedKey("s3cret")));
    AccessGroup group = new AccessGroup();
    group.setId("group-1");
    group.setProjectId("project-1");
    when(accessGroupRepository.findById("group-1")).thenReturn(Optional.of(group));
    Project project = new Project();
    project.setId("project-1");
    project.setStatus(ProjectStatus.ARCHIVED);
    when(projectRepository.findById("project-1")).thenReturn(Optional.of(project));

    AuthenticationException exception =
        assertThrows(
            AuthenticationException.class,
            () -> apiKeyService.authenticate(ApiKeyService.buildToken("abcDEF123456", "s3cret")));
    assertEquals("Project is archived", exception.getMessage());
  }

  @Test
  void revokeKeepsTheRowButChangesStatus() {
    ApiKey key = storedKey("s3cret");
    when(apiKeyRepository.findById("abcDEF123456")).thenReturn(Optional.of(key));
    when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(i -> i.getArgument(0));

    ApiKey revoked = apiKeyService.revokeApiKey("abcDEF123456");

    assertEquals(ApiKeyStatus.REVOKED, revoked.getStatus());
    org.mockito.Mockito.verify(apiKeyRepository, org.mockito.Mockito.never()).delete(any());
  }

  @Test
  void resolvesTokenFromBearerOrApiKeyHeader() {
    MockHttpServletRequest bearer = new MockHttpServletRequest();
    bearer.addHeader("Authorization", "Bearer opc_live_abc_def");
    assertEquals("opc_live_abc_def", ApiKeyService.resolveToken(bearer));

    MockHttpServletRequest header = new MockHttpServletRequest();
    header.addHeader("X-Api-Key", " opc_live_abc_def ");
    assertEquals("opc_live_abc_def", ApiKeyService.resolveToken(header));

    MockHttpServletRequest jwtBearer = new MockHttpServletRequest();
    jwtBearer.addHeader("Authorization", "Bearer eyJhbGciOi.jwt.token");
    assertNull(ApiKeyService.resolveToken(jwtBearer));

    assertNull(ApiKeyService.resolveToken(new MockHttpServletRequest()));
  }
}
