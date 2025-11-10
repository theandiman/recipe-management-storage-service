package com.recipe.storage.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import com.recipe.storage.dto.CreateRecipeRequest;
import com.recipe.storage.dto.RecipeResponse;
import com.recipe.storage.model.Recipe;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for managing recipes in Firestore.
 */
@Slf4j
@Service
public class RecipeService {

  @Autowired(required = false)
  private Firestore firestore;

  @Value("${firestore.collection.recipes}")
  private String recipesCollection;

  /**
   * Save a new recipe to Firestore.
   *
   * @param request The recipe creation request
   * @param userId The Firebase user ID of the recipe creator
   * @return The saved recipe with generated ID
   */
  public RecipeResponse saveRecipe(CreateRecipeRequest request, String userId) {
    if (firestore == null) {
      log.warn("Firestore not configured - running in test mode");
      return createMockResponse(request, userId);
    }
    
    try {
      String recipeId = UUID.randomUUID().toString();
      Instant now = Instant.now();

      Recipe recipe = Recipe.builder()
          .id(recipeId)
          .userId(userId)
          .title(request.getTitle())
          .description(request.getDescription())
          .ingredients(request.getIngredients())
          .instructions(request.getInstructions())
          .prepTime(request.getPrepTime())
          .cookTime(request.getCookTime())
          .servings(request.getServings())
          .nutrition(request.getNutrition())
          .tips(request.getTips())
          .imageUrl(request.getImageUrl())
          .source(request.getSource())
          .tags(request.getTags())
          .dietaryRestrictions(request.getDietaryRestrictions())
          .createdAt(now)
          .updatedAt(now)
          .build();

      DocumentReference docRef = firestore.collection(recipesCollection).document(recipeId);
      ApiFuture<WriteResult> future = docRef.set(recipe);
      
      // Wait for the write to complete
      WriteResult result = future.get();
      log.info("Recipe saved with ID: {} at {}", recipeId, result.getUpdateTime());

      return mapToResponse(recipe);
    } catch (InterruptedException | ExecutionException e) {
      log.error("Error saving recipe to Firestore", e);
      throw new RuntimeException("Failed to save recipe", e);
    }
  }

  /**
   * Create mock response for testing without Firestore.
   */
  private RecipeResponse createMockResponse(CreateRecipeRequest request, String userId) {
    String recipeId = UUID.randomUUID().toString();
    Instant now = Instant.now();
    
    Recipe recipe = Recipe.builder()
        .id(recipeId)
        .userId(userId)
        .title(request.getTitle())
        .description(request.getDescription())
        .ingredients(request.getIngredients())
        .instructions(request.getInstructions())
        .prepTime(request.getPrepTime())
        .cookTime(request.getCookTime())
        .servings(request.getServings())
        .nutrition(request.getNutrition())
        .tips(request.getTips())
        .imageUrl(request.getImageUrl())
        .source(request.getSource())
        .tags(request.getTags())
        .dietaryRestrictions(request.getDietaryRestrictions())
        .createdAt(now)
        .updatedAt(now)
        .build();
    
    log.info("Mock recipe created with ID: {}", recipeId);
    return mapToResponse(recipe);
  }

  /**
   * Map Recipe entity to RecipeResponse DTO.
   */
  private RecipeResponse mapToResponse(Recipe recipe) {
    return RecipeResponse.builder()
        .id(recipe.getId())
        .userId(recipe.getUserId())
        .title(recipe.getTitle())
        .description(recipe.getDescription())
        .ingredients(recipe.getIngredients())
        .instructions(recipe.getInstructions())
        .prepTime(recipe.getPrepTime())
        .cookTime(recipe.getCookTime())
        .servings(recipe.getServings())
        .nutrition(recipe.getNutrition())
        .tips(recipe.getTips())
        .imageUrl(recipe.getImageUrl())
        .source(recipe.getSource())
        .createdAt(recipe.getCreatedAt())
        .updatedAt(recipe.getUpdatedAt())
        .tags(recipe.getTags())
        .dietaryRestrictions(recipe.getDietaryRestrictions())
        .build();
  }
}
