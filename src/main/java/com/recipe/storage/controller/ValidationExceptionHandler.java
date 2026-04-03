package com.recipe.storage.controller;

import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler that maps bean-validation constraint violations
 * on controller method parameters to HTTP 400 Bad Request.
 */
@RestControllerAdvice
public class ValidationExceptionHandler {

  /**
   * Handle {@code ConstraintViolationException} thrown by {@code @Validated} +
   * {@code @Min}/{@code @Max} on {@code @RequestParam} or {@code @PathVariable}
   * and map it to 400 Bad Request.
   *
   * @param ex the constraint violation exception
   * @return a structured error response with status and message
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Map<String, Object>> handleConstraintViolation(
      ConstraintViolationException ex) {
    Map<String, Object> body = Map.of(
        "status", HttpStatus.BAD_REQUEST.value(),
        "error", HttpStatus.BAD_REQUEST.getReasonPhrase(),
        "message", "Invalid request parameters");
    return ResponseEntity.badRequest().body(body);
  }
}
