package com.recipe.storage.filter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that validates Firebase ID tokens and extracts user ID.
 * Sets userId as a request attribute for downstream use.
 */
@Slf4j
@Component
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";
  private static final List<String> ALLOWED_ORIGINS = List.of(
      "http://localhost:5173",
      "http://localhost:5174",
      "https://recipe-mgmt-dev.web.app",
      "https://recipe-mgmt-dev.firebaseapp.com");

  @Value("${auth.enabled}")
  private boolean authEnabled;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain) throws ServletException, IOException {

    // Skip auth for OPTIONS requests (CORS preflight)
    if ("OPTIONS".equals(request.getMethod())) {
      // Set CORS headers for preflight response
      String origin = request.getHeader("Origin");
      if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
        response.setHeader("Access-Control-Allow-Origin", origin);
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "*");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Max-Age", "3600");
      }
      response.setStatus(HttpServletResponse.SC_OK);
      return;
    }

    // Skip auth for health check
    if (request.getRequestURI().contains("/actuator/health")) {
      filterChain.doFilter(request, response);
      return;
    }

    // Skip auth for public recipes
    if (request.getRequestURI().contains("/api/recipes/public")) {
      filterChain.doFilter(request, response);
      return;
    }

    // Skip auth for Swagger UI and API docs
    if (request.getRequestURI().contains("/v3/api-docs")
        || request.getRequestURI().contains("/swagger-ui")) {
      filterChain.doFilter(request, response);
      return;
    }

    if (!authEnabled) {
      log.warn("Authentication is disabled - using test user");
      request.setAttribute("userId", "test-user");
      filterChain.doFilter(request, response);
      return;
    }

    String authHeader = request.getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
      log.error("Missing or invalid Authorization header");
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
          "Missing or invalid Authorization header");
      return;
    }

    String idToken = authHeader.substring(BEARER_PREFIX.length());

    try {
      FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
      String uid = decodedToken.getUid();
      String email = decodedToken.getEmail();

      log.info("Authenticated user: uid={}, email={}", uid, email);

      // Set userId as request attribute for controller access
      request.setAttribute("userId", uid);

      filterChain.doFilter(request, response);
    } catch (FirebaseAuthException e) {
      log.error("Failed to verify Firebase token: {}", e.getMessage());
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
          "Invalid Firebase ID token");
    }
  }
}
