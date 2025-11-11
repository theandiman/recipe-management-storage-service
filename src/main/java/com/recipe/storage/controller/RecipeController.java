package com.recipe.storage.controller;

import com.recipe.storage.dto.CreateRecipeRequest;
import com.recipe.storage.dto.RecipeResponse;
import com.recipe.storage.service.RecipeService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

  /**
   * Get all recipes for the authenticated user.
   * Requires Firebase authentication.
   *
   * @param userId The authenticated user's Firebase UID (injected by auth filter)
   * @return List of recipes belonging to the user
   */
  @GetMapping
  public ResponseEntity<List<RecipeResponse>> getUserRecipes(
      @RequestAttribute("userId") String userId) {
    
    log.info("Fetching recipes for user {}", userId);
    List<RecipeResponse> recipes = recipeService.getUserRecipes(userId);
    return ResponseEntity.ok(recipes);
  }

  /**
   * Get a specific recipe by ID.
   * Requires Firebase authentication and user must own the recipe.
   *
   * @param recipeId The recipe ID
   * @param userId The authenticated user's Firebase UID (injected by auth filter)
   * @return The recipe if found and user has access
   */
  @GetMapping("/{recipeId}")
  public ResponseEntity<RecipeResponse> getRecipe(
      @PathVariable String recipeId,
      @RequestAttribute("userId") String userId) {
    
    log.info("Fetching recipe {} for user {}", recipeId, userId);
    RecipeResponse recipe = recipeService.getRecipe(recipeId, userId);
    return ResponseEntity.ok(recipe);
  }
}
