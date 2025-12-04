package com.recipe.storage.controller;

import com.recipe.storage.dto.CreateRecipeRequest;
import com.recipe.storage.dto.RecipeResponse;
import com.recipe.storage.service.RecipeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
@Tag(name = "Recipe Storage", description = "APIs for storing and retrieving user recipes")
@SecurityRequirement(name = "Firebase Auth")
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
  @Operation(
      summary = "Save a new recipe",
      description = "Creates and stores a new recipe for the authenticated user. "
          + "Requires Firebase authentication token in Authorization header."
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "201",
          description = "Recipe created successfully",
          content = @Content(schema = @Schema(implementation = RecipeResponse.class))
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Invalid request body (missing required fields or validation errors)",
          content = @Content
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Unauthorized - invalid or missing Firebase token",
          content = @Content
      )
  })
  public ResponseEntity<RecipeResponse> saveRecipe(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "Recipe details to save",
          required = true,
          content = @Content(schema = @Schema(implementation = CreateRecipeRequest.class))
      )
      @Valid @RequestBody CreateRecipeRequest request,
      @Parameter(hidden = true) @RequestAttribute("userId") String userId) {
    
    log.info("Saving recipe '{}' for user {}", request.getRecipeName(), userId);
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
  @Operation(
      summary = "Get all user recipes",
      description = "Retrieves all recipes owned by the authenticated user, "
          + "ordered by creation date (newest first). "
          + "Requires Firebase authentication token in Authorization header."
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Recipes retrieved successfully (returns empty array if none found)",
          content = @Content(schema = @Schema(implementation = RecipeResponse.class))
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Unauthorized - invalid or missing Firebase token",
          content = @Content
      )
  })
  public ResponseEntity<List<RecipeResponse>> getUserRecipes(
      @Parameter(hidden = true) @RequestAttribute("userId") String userId) {
    
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
  @Operation(
      summary = "Get a specific recipe by ID",
      description = "Retrieves a recipe by its ID. User must own the recipe to access it. "
          + "Requires Firebase authentication token in Authorization header."
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Recipe retrieved successfully",
          content = @Content(schema = @Schema(implementation = RecipeResponse.class))
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Unauthorized - invalid or missing Firebase token",
          content = @Content
      ),
      @ApiResponse(
          responseCode = "403",
          description = "Forbidden - user does not own this recipe",
          content = @Content
      ),
      @ApiResponse(
          responseCode = "404",
          description = "Recipe not found",
          content = @Content
      )
  })
  public ResponseEntity<RecipeResponse> getRecipe(
      @Parameter(description = "Recipe ID", required = true)
      @PathVariable String recipeId,
      @Parameter(hidden = true) @RequestAttribute("userId") String userId) {
    
    log.info("Fetching recipe {} for user {}", recipeId, userId);
    RecipeResponse recipe = recipeService.getRecipe(recipeId, userId);
    return ResponseEntity.ok(recipe);
  }

  /**
   * Delete a recipe by ID.
   * Requires Firebase authentication and user must own the recipe.
   *
   * @param recipeId The recipe ID to delete
   * @param userId The authenticated user's Firebase UID (injected by auth filter)
   * @return 204 No Content on successful deletion
   */
  @DeleteMapping("/{recipeId}")
  @Operation(
      summary = "Delete a recipe by ID",
      description = "Deletes a recipe by its ID. User must own the recipe to delete it. "
          + "Requires Firebase authentication token in Authorization header."
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "204",
          description = "Recipe deleted successfully",
          content = @Content
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Unauthorized - invalid or missing Firebase token",
          content = @Content
      ),
      @ApiResponse(
          responseCode = "403",
          description = "Forbidden - user does not own this recipe",
          content = @Content
      ),
      @ApiResponse(
          responseCode = "404",
          description = "Recipe not found",
          content = @Content
      )
  })
  public ResponseEntity<Void> deleteRecipe(
      @Parameter(description = "Recipe ID to delete", required = true)
      @PathVariable String recipeId,
      @Parameter(hidden = true) @RequestAttribute("userId") String userId) {
    
    log.info("Deleting recipe {} for user {}", recipeId, userId);
    recipeService.deleteRecipe(recipeId, userId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Update a recipe by ID.
   * Requires Firebase authentication and user must own the recipe.
   *
   * @param recipeId The recipe ID to update
   * @param request The recipe update request
   * @param userId The authenticated user's Firebase UID (injected by auth filter)
   * @return The updated recipe
   */
  @PutMapping("/{recipeId}")
  @Operation(
      summary = "Update a recipe by ID",
      description = "Updates a recipe by its ID. User must own the recipe to update it. "
          + "Requires Firebase authentication token in Authorization header."
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Recipe updated successfully",
          content = @Content(schema = @Schema(implementation = RecipeResponse.class))
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Invalid request body (missing required fields or validation errors)",
          content = @Content
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Unauthorized - invalid or missing Firebase token",
          content = @Content
      ),
      @ApiResponse(
          responseCode = "403",
          description = "Forbidden - user does not own this recipe",
          content = @Content
      ),
      @ApiResponse(
          responseCode = "404",
          description = "Recipe not found",
          content = @Content
      )
  })
  public ResponseEntity<RecipeResponse> updateRecipe(
      @Parameter(description = "Recipe ID to update", required = true)
      @PathVariable String recipeId,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "Recipe details to update",
          required = true,
          content = @Content(schema = @Schema(implementation = CreateRecipeRequest.class))
      )
      @Valid @RequestBody CreateRecipeRequest request,
      @Parameter(hidden = true) @RequestAttribute("userId") String userId) {

    log.info("Updating recipe {} for user {}", recipeId, userId);
    RecipeResponse response = recipeService.updateRecipe(recipeId, request, userId);
    return ResponseEntity.ok(response);
  }
}
