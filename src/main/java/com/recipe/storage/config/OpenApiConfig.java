package com.recipe.storage.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for API documentation.
 */
@Configuration
public class OpenApiConfig {

  /**
   * Configure OpenAPI documentation.
   *
   * @return OpenAPI configuration
   */
  @Bean
  public OpenAPI recipeStorageOpenApi() {
    final String securitySchemeName = "Firebase Auth";
    
    return new OpenAPI()
        .info(new Info()
            .title("Recipe Storage Service API")
            .description("API for storing and managing user recipes with Firebase authentication")
            .version("v1.0")
            .license(new License()
                .name("Apache 2.0")
                .url("https://www.apache.org/licenses/LICENSE-2.0")))
        .addSecurityItem(new SecurityRequirement()
            .addList(securitySchemeName))
        .components(new Components()
            .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                .name(securitySchemeName)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Firebase ID Token (JWT). "
                    + "Obtain from Firebase Authentication and include in Authorization header.")));
  }
}
