package com.opencirc.api.passport.config;

import com.opencirc.api.passport.auth.principal.ApiKeyPrincipal;
import com.opencirc.api.passport.constants.AppConstants;
import com.opencirc.api.passport.enums.Permission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

/**
 * Enforces the access group permission profile of a machine request against the HTTP method: reads
 * need {@code canRead}, creations {@code canCreate}, modifications {@code canUpdate} and removals
 * {@code canDelete}. The profile is already on the principal, so no lookup is needed.
 */
@Component
public class AccessGroupPermissionInterceptor implements HandlerInterceptor {

  private final ObjectMapper objectMapper;

  /** Constructor. */
  public AccessGroupPermissionInterceptor(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !(authentication.getPrincipal() instanceof ApiKeyPrincipal principal)) {
      return true;
    }

    Permission required = requiredPermission(request.getMethod());
    if (required == null
        || (principal.getPermissions() != null && principal.getPermissions().allows(required))) {
      return true;
    }

    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType("application/json");
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    objectMapper.writeValue(
        response.getWriter(),
        Map.of("error", "Forbidden", "message", AppConstants.ERR_PERMISSION_DENIED));
    return false;
  }

  /** Maps an HTTP method to the permission it needs; null for methods that need none. */
  public static Permission requiredPermission(String method) {
    if (method == null) {
      return null;
    }
    return switch (method.toUpperCase(Locale.ROOT)) {
      case "GET", "HEAD" -> Permission.READ;
      case "POST" -> Permission.CREATE;
      case "PUT", "PATCH" -> Permission.UPDATE;
      case "DELETE" -> Permission.DELETE;
      default -> null;
    };
  }
}
