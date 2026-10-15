package com.mymindmirror.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the OpenAPI document that SpringDoc exposes at /v3/api-docs.
 *
 * <p>Declares:
 * <ul>
 *   <li>API metadata (title, version, contact)</li>
 *   <li>JWT bearer auth scheme (so Swagger UI shows an "Authorize" button)</li>
 *   <li>Global bearer auth applied to all endpoints by default</li>
 * </ul>
 *
 * <p>The server list is intentionally omitted: SpringDoc derives the server URL from
 * the incoming request, so the same build works against localhost, staging, and
 * production without any code change.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI myMindMirrorOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MyMindMirror API")
                        .description("""
                                AI-powered journaling, roadmap, and schedule backend.

                                **Authentication:** Use `POST /api/auth/login` to obtain a JWT,
                                then click the "Authorize" button above and paste the token
                                (without the `Bearer ` prefix). Registration is available at
                                `POST /api/auth/register` — no auth required.
                                """)
                        .version("v1")
                        .contact(new Contact()
                                .name("MyMindMirror")
                                .email("support@mymindmirror.local")))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste your JWT token below (without the 'Bearer ' prefix).")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}