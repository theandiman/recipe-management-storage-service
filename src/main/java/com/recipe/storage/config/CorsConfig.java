package com.recipe.storage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration for allowing frontend access.
 */
@Configuration
public class CorsConfig {

  /**
   * Configure CORS to allow requests from frontend origins.
   */
  @Bean
  public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins(
                "http://localhost:5173", // Local development
                "http://localhost:5174", // Local development (alt port)
                "https://recipe-mgmt-dev.web.app", // Firebase Hosting (dev)
                "https://recipe-mgmt-dev.firebaseapp.com" // Firebase Hosting (dev alt)
            )
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .exposedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
      }
    };
  }
}
