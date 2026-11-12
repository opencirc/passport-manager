package com.opencirc.api.passport.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI description of the two credential types and their endpoint families. */
@Configuration
public class OpenApiConfig {

  public static final String ADMIN_SESSION = "adminSession";
  public static final String API_KEY = "apiKey";

  /** The OpenAPI document served at /v3/api-docs. */
  @Bean
  public OpenAPI passportManagerOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("OpenCirc Passport Manager")
                .version("2")
                .description(
                    "Administrators (JWT session) govern owners, projects, access groups and API"
                        + " keys under /api/admin. Machine integrations (API key bearer token)"
                        + " operate on the passports, datasheets and audit logs of the key's"
                        + " project under /api/passport and /api/audit-logs, limited by the"
                        + " access group's CRUD permissions."))
        .components(
            new Components()
                .addSecuritySchemes(
                    API_KEY,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .description(
                            "Machine token opc_live_<key_id>_<secret> issued under an access"
                                + " group; also accepted in the X-Api-Key header."))
                .addSecuritySchemes(
                    ADMIN_SESSION,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name("access_token")
                        .description("Administrator session cookie set by POST /api/auth/login.")))
        .addSecurityItem(new SecurityRequirement().addList(API_KEY))
        .addSecurityItem(new SecurityRequirement().addList(ADMIN_SESSION));
  }
}
