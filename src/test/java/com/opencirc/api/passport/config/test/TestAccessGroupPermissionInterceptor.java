package com.opencirc.api.passport.config.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.opencirc.api.passport.auth.principal.ApiKeyAuthenticationToken;
import com.opencirc.api.passport.auth.principal.ApiKeyPrincipal;
import com.opencirc.api.passport.config.AccessGroupPermissionInterceptor;
import com.opencirc.api.passport.enums.Permission;
import com.opencirc.api.passport.model.AccessGroupPermissions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

public class TestAccessGroupPermissionInterceptor {

  private final AccessGroupPermissionInterceptor interceptor =
      new AccessGroupPermissionInterceptor(new ObjectMapper());

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void mapsHttpMethodsToPermissions() {
    assertEquals(Permission.READ, AccessGroupPermissionInterceptor.requiredPermission("GET"));
    assertEquals(Permission.CREATE, AccessGroupPermissionInterceptor.requiredPermission("POST"));
    assertEquals(Permission.UPDATE, AccessGroupPermissionInterceptor.requiredPermission("put"));
    assertEquals(Permission.UPDATE, AccessGroupPermissionInterceptor.requiredPermission("PATCH"));
    assertEquals(Permission.DELETE, AccessGroupPermissionInterceptor.requiredPermission("DELETE"));
    assertNull(AccessGroupPermissionInterceptor.requiredPermission("OPTIONS"));
  }

  @Test
  void forbidsOperationsTheAccessGroupDoesNotGrant() throws Exception {
    authenticate(AccessGroupPermissions.readOnly());
    MockHttpServletResponse response = new MockHttpServletResponse();

    boolean proceed = interceptor.preHandle(request("POST"), response, new Object());

    assertFalse(proceed);
    assertEquals(403, response.getStatus());
    assertTrue(response.getContentAsString().contains("does not grant"));
  }

  @Test
  void allowsOperationsTheAccessGroupGrants() throws Exception {
    authenticate(new AccessGroupPermissions(false, true, true, false));

    assertTrue(interceptor.preHandle(request("GET"), new MockHttpServletResponse(), new Object()));
    assertTrue(interceptor.preHandle(request("PUT"), new MockHttpServletResponse(), new Object()));
    assertFalse(
        interceptor.preHandle(request("DELETE"), new MockHttpServletResponse(), new Object()));
  }

  @Test
  void ignoresRequestsWithoutMachinePrincipal() throws Exception {
    assertTrue(
        interceptor.preHandle(request("DELETE"), new MockHttpServletResponse(), new Object()));
  }

  private static void authenticate(AccessGroupPermissions permissions) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new ApiKeyAuthenticationToken(
                new ApiKeyPrincipal("key-1", "Key", "group-1", "project-1", permissions)));
  }

  private static MockHttpServletRequest request(String method) {
    return new MockHttpServletRequest(method, "/api/passport/root");
  }
}
