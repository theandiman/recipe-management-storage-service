package com.recipe.storage.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.recipe.storage.dto.CreateRecipeRequest;
import com.recipe.storage.dto.RecipeResponse;
import com.recipe.storage.model.Recipe;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

      com.recipe.shared.model.Recipe sharedRecipe = com.recipe.shared.model.Recipe.builder()
          .id(recipeId)
          .userId(userId)
          .recipeName(request.getRecipeName())
          .description(request.getDescription())
          .ingredients(request.getIngredients())
          .instructions(request.getInstructions())
          .prepTimeMinutes(request.getPrepTimeMinutes())
          .cookTimeMinutes(request.getCookTimeMinutes())
          .servings(request.getServings())
          .nutritionalInfo(request.getNutritionalInfo())
          .tips(request.getTips())
          .imageUrl(request.getImageUrl())
          .source(request.getSource())
          .tags(request.getTags())
          .dietaryRestrictions(request.getDietaryRestrictions())
          .createdAt(now)
          .updatedAt(now)
          .build();

      Recipe recipe = Recipe.fromShared(sharedRecipe);

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

  private RecipeResponse createMockResponse(CreateRecipeRequest request, String userId) {
    String recipeId = UUID.randomUUID().toString();
    Instant now = Instant.now();
    
    com.recipe.shared.model.Recipe sharedRecipe = com.recipe.shared.model.Recipe.builder()
        .id(recipeId)
        .userId(userId)
        .recipeName(request.getRecipeName())
        .description(request.getDescription())
        .ingredients(request.getIngredients())
        .instructions(request.getInstructions())
        .prepTimeMinutes(request.getPrepTimeMinutes())
        .cookTimeMinutes(request.getCookTimeMinutes())
        .servings(request.getServings())
        .nutritionalInfo(request.getNutritionalInfo())
        .tips(request.getTips())
        .imageUrl(request.getImageUrl())
        .source(request.getSource())
        .tags(request.getTags())
        .dietaryRestrictions(request.getDietaryRestrictions())
        .createdAt(now)
        .updatedAt(now)
        .build();
    
    Recipe recipe = Recipe.fromShared(sharedRecipe);
    
    log.info("Mock recipe created with ID: {}", recipeId);
    return mapToResponse(recipe);
  }

  /**
   * Get all recipes for a specific user.
   *
   * @param userId The Firebase user ID
   * @return List of recipes belonging to the user
   */
  public List<RecipeResponse> getUserRecipes(String userId) {
    if (firestore == null) {
      log.warn("Firestore not configured - returning empty list");
      return new ArrayList<>();
    }
    
    try {
      // Note: Removed orderBy to avoid needing composite index
      // Recipes are returned in document creation order
      // TODO: Add composite index and re-enable orderBy for better UX
      Query query = firestore.collection(recipesCollection)
          .whereEqualTo("userId", userId);
      
      ApiFuture<QuerySnapshot> future = query.get();
      QuerySnapshot querySnapshot = future.get();
      
      List<RecipeResponse> recipes = new ArrayList<>();
      querySnapshot.getDocuments().forEach(doc -> {
        Recipe recipe = doc.toObject(Recipe.class);
        recipes.add(mapToResponse(recipe));
      });
      
      // Sort in-memory by createdAt (newest first)
      recipes.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
      
      log.info("Found {} recipes for user {}", recipes.size(), userId);
      return recipes;
    } catch (InterruptedException | ExecutionException e) {
      log.error("Error fetching recipes from Firestore", e);
      throw new RuntimeException("Failed to fetch recipes", e);
    }
  }

  /**
   * Get a specific recipe by ID.
   *
   * @param recipeId The recipe ID
   * @param userId The Firebase user ID (for authorization)
   * @return The recipe if found and user has access
   * @throws ResponseStatusException if recipe not found or user doesn't have access
   */
  public RecipeResponse getRecipe(String recipeId, String userId) {
    if (firestore == null) {
      log.warn("Firestore not configured - cannot fetch recipe");
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found");
    }
    
    try {
      DocumentReference docRef = firestore.collection(recipesCollection).document(recipeId);
      ApiFuture<DocumentSnapshot> future = docRef.get();
      DocumentSnapshot document = future.get();
      
      if (!document.exists()) {
        log.warn("Recipe not found: {}", recipeId);
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found");
      }
      
      Recipe recipe = document.toObject(Recipe.class);
      if (recipe == null) {
        log.error("Failed to deserialize recipe: {}", recipeId);
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
            "Failed to load recipe");
      }
      
      // Verify user owns this recipe
      if (!userId.equals(recipe.getUserId())) {
        log.warn("User {} attempted to access recipe {} owned by {}", 
            userId, recipeId, recipe.getUserId());
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
      }
      
      log.info("Retrieved recipe {} for user {}", recipeId, userId);
      return mapToResponse(recipe);
    } catch (InterruptedException | ExecutionException e) {
      log.error("Error fetching recipe from Firestore", e);
      throw new RuntimeException("Failed to fetch recipe", e);
    }
  }

  /**
   * Update an existing recipe.
   *
   * @param recipeId The recipe ID
   * @param request The recipe update request
   * @param userId The Firebase user ID (for authorization)
   * @return The updated recipe
   * @throws ResponseStatusException if recipe not found or user doesn't have access
   */
  public RecipeResponse updateRecipe(String recipeId, CreateRecipeRequest request, String userId) {
    if (firestore == null) {
      log.warn("Firestore not configured - running in test mode");
      return createMockResponse(request, userId);
    }

    try {
      DocumentReference docRef = firestore.collection(recipesCollection).document(recipeId);
      ApiFuture<DocumentSnapshot> future = docRef.get();
      DocumentSnapshot document = future.get();

      if (!document.exists()) {
        log.warn("Recipe not found for update: {}", recipeId);
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found");
      }

      Recipe existingRecipe = document.toObject(Recipe.class);
      if (existingRecipe == null) {
        log.error("Failed to deserialize recipe for update: {}", recipeId);
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
            "Failed to load recipe");
      }

      // Verify user owns this recipe
      if (!userId.equals(existingRecipe.getUserId())) {
        log.warn("User {} attempted to update recipe {} owned by {}",
            userId, recipeId, existingRecipe.getUserId());
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
      }

      // Update fields
      Recipe updatedRecipe = existingRecipe.toBuilder()
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
          .updatedAt(Instant.now())
          .build();

      ApiFuture<WriteResult> writeFuture = docRef.set(updatedRecipe);
      WriteResult result = writeFuture.get();

      log.info("Updated recipe {} for user {} at {}", recipeId, userId, result.getUpdateTime());
      return mapToResponse(updatedRecipe);
    } catch (InterruptedException | ExecutionException e) {
      log.error("Error updating recipe in Firestore", e);
      throw new RuntimeException("Failed to update recipe", e);
    }
  }

  /**
   * Delete a recipe by ID.
   *
   * @param recipeId The recipe ID
   * @param userId The Firebase user ID (for authorization)
   * @throws ResponseStatusException if recipe not found or user doesn't have access
   */
  public void deleteRecipe(String recipeId, String userId) {
    if (firestore == null) {
      log.warn("Firestore not configured - cannot delete recipe");
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found");
    }
    
    try {
      DocumentReference docRef = firestore.collection(recipesCollection).document(recipeId);
      ApiFuture<DocumentSnapshot> future = docRef.get();
      DocumentSnapshot document = future.get();
      
      if (!document.exists()) {
        log.warn("Recipe not found for deletion: {}", recipeId);
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found");
      }
      
      Recipe recipe = document.toObject(Recipe.class);
      if (recipe == null) {
        log.error("Failed to deserialize recipe for deletion: {}", recipeId);
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
            "Failed to load recipe");
      }
      
      // Verify user owns this recipe
      if (!userId.equals(recipe.getUserId())) {
        log.warn("User {} attempted to delete recipe {} owned by {}", 
            userId, recipeId, recipe.getUserId());
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
      }
      
      // Delete the document
      ApiFuture<WriteResult> deleteFuture = docRef.delete();
      WriteResult result = deleteFuture.get();
      
      log.info("Deleted recipe {} for user {} at {}", recipeId, userId, result.getUpdateTime());
    } catch (InterruptedException | ExecutionException e) {
      log.error("Error deleting recipe from Firestore", e);
      throw new RuntimeException("Failed to delete recipe", e);
    }
  }

  private RecipeResponse mapToResponse(Recipe recipe) {
    return RecipeResponse.builder()
        .id(recipe.getId())
        .userId(recipe.getUserId())
        .recipeName(recipe.getRecipeName())
        .description(recipe.getDescription())
        .ingredients(recipe.getIngredients())
        .instructions(recipe.getInstructions())
        .prepTimeMinutes(recipe.getPrepTimeMinutes())
        .cookTimeMinutes(recipe.getCookTimeMinutes())
        .totalTimeMinutes(recipe.getTotalTimeMinutes())
        .servings(recipe.getServings())
        .nutritionalInfo(recipe.getNutritionalInfo())
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
