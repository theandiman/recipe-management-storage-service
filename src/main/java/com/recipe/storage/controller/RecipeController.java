package com.recipe.storage.controller;

import com.recipe.storage.dto.CreateRecipeRequest;
import com.recipe.storage.dto.RecipeResponse;
import com.recipe.storage.service.RecipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for recipe storage operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {

  private final RecipeService recipeService;

  /**
   * Save a new recipe.
   * Requires Firebase authentication.
   *
   * @param request The recipe creation request
   * @param userId The authenticated user's Firebase UID (injected by auth filter)
   * @return The saved recipe
   */
  @PostMapping
  public ResponseEntity<RecipeResponse> saveRecipe(
      @Valid @RequestBody CreateRecipeRequest request,
      @RequestAttribute("userId") String userId) {
    
    log.info("Saving recipe '{}' for user {}", request.getTitle(), userId);
    RecipeResponse response = recipeService.saveRecipe(request, userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
