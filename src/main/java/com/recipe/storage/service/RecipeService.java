package com.recipe.storage.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.recipe.shared.model.NutritionalInfo;
import com.recipe.shared.model.Recipe;
import com.recipe.storage.dto.CreateRecipeRequest;
import com.recipe.storage.dto.RecipeResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
   * @param userId  The Firebase user ID of the recipe creator
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

      NutritionalInfo nutritionalInfo = mapToNutritionalInfo(request.getNutrition());
      com.recipe.shared.model.RecipeTips recipeTips = mapToRecipeTips(request.getTips());

      Recipe recipe = Recipe.builder()
          .id(recipeId)
          .userId(userId)
          .recipeName(request.getTitle())
          .description(request.getDescription())
          .ingredients(request.getIngredients())
          .instructions(request.getInstructions())
          .prepTimeMinutes(request.getPrepTime())
          .cookTimeMinutes(request.getCookTime())
          .servings(request.getServings())
          .nutritionalInfo(nutritionalInfo)
          .tips(recipeTips)
          .imageUrl(request.getImageUrl())
          .source(request.getSource())
          .tags(request.getTags())
          .dietaryRestrictions(request.getDietaryRestrictions())
          .isPublic(request.isPublic())
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
   * Update an existing recipe.
   *
   * @param recipeId The ID of the recipe to update
   * @param request  The update request
   * @param userId   The ID of the user attempting the update
   * @return The updated recipe response
   */
  public RecipeResponse updateRecipe(String recipeId, CreateRecipeRequest request, String userId) {
    if (firestore == null) {
      return createMockResponse(request, userId);
    }

    try {
      DocumentReference docRef = firestore.collection(recipesCollection).document(recipeId);
      ApiFuture<DocumentSnapshot> future = docRef.get();
      DocumentSnapshot document = future.get();

      if (!document.exists()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found");
      }

      Recipe existingRecipe = document.toObject(Recipe.class);
      if (existingRecipe == null) {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
            "Failed to load existing recipe");
      }

      // Verify ownership
      if (!userId.equals(existingRecipe.getUserId())) {
        log.warn("User {} attempted to update recipe {} owned by {}",
            userId, recipeId, existingRecipe.getUserId());
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
      }

      Instant now = Instant.now();

      NutritionalInfo nutritionalInfo = mapToNutritionalInfo(request.getNutrition());
      com.recipe.shared.model.RecipeTips recipeTips = mapToRecipeTips(request.getTips());

      // Update fields
      Recipe updatedRecipe = existingRecipe.toBuilder()
          .recipeName(request.getTitle())
          .description(request.getDescription())
          .ingredients(request.getIngredients())
          .instructions(request.getInstructions())
          .prepTimeMinutes(request.getPrepTime())
          .cookTimeMinutes(request.getCookTime())
          .servings(request.getServings())
          .nutritionalInfo(nutritionalInfo)
          .tips(recipeTips)
          .imageUrl(request.getImageUrl())
          .source(request.getSource())
          .tags(request.getTags())
          .dietaryRestrictions(request.getDietaryRestrictions())
          .isPublic(request.isPublic())
          .updatedAt(now)
          .build();

      ApiFuture<WriteResult> writeFuture = docRef.set(updatedRecipe);
      writeFuture.get();

      log.info("Recipe updated with ID: {} by user {}", recipeId, userId);
      return mapToResponse(updatedRecipe);
    } catch (InterruptedException | ExecutionException e) {
      log.error("Error updating recipe in Firestore", e);
      throw new RuntimeException("Failed to update recipe", e);
    }
  }

  /**
   * Create mock response for testing without Firestore.
   */
  private RecipeResponse createMockResponse(CreateRecipeRequest request, String userId) {
    String recipeId = UUID.randomUUID().toString();
    Instant now = Instant.now();

    NutritionalInfo nutritionalInfo = mapToNutritionalInfo(request.getNutrition());
    com.recipe.shared.model.RecipeTips recipeTips = mapToRecipeTips(request.getTips());

    Recipe recipe = Recipe.builder()
        .id(recipeId)
        .userId(userId)
        .recipeName(request.getTitle())
        .description(request.getDescription())
        .ingredients(request.getIngredients())
        .instructions(request.getInstructions())
        .prepTimeMinutes(request.getPrepTime())
        .cookTimeMinutes(request.getCookTime())
        .servings(request.getServings())
        .nutritionalInfo(nutritionalInfo)
        .tips(recipeTips)
        .imageUrl(request.getImageUrl())
        .source(request.getSource())
        .tags(request.getTags())
        .dietaryRestrictions(request.getDietaryRestrictions())
        .isPublic(request.isPublic())
        .createdAt(now)
        .updatedAt(now)
        .build();

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
   * Get all public recipes.
   *
   * @return List of public recipes
   */
  public List<RecipeResponse> getPublicRecipes() {
    if (firestore == null) {
      log.warn("Firestore not configured - returning empty list");
      return new ArrayList<>();
    }

    try {
      // TODO: Add composite index on isPublic + createdAt for better sorting
      Query query = firestore.collection(recipesCollection)
          .whereEqualTo("isPublic", true);

      ApiFuture<QuerySnapshot> future = query.get();
      QuerySnapshot querySnapshot = future.get();

      List<RecipeResponse> recipes = new ArrayList<>();
      querySnapshot.getDocuments().forEach(doc -> {
        Recipe recipe = doc.toObject(Recipe.class);
        recipes.add(mapToResponse(recipe));
      });

      // Sort in-memory by createdAt (newest first)
      recipes.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

      log.info("Found {} public recipes", recipes.size());
      return recipes;
    } catch (InterruptedException | ExecutionException e) {
      log.error("Error fetching public recipes from Firestore", e);
      throw new RuntimeException("Failed to fetch public recipes", e);
    }
  }

  /**
   * Get a specific recipe by ID.
   *
   * @param recipeId The recipe ID
   * @param userId   The Firebase user ID (for authorization)
   * @return The recipe if found and user has access
   * @throws ResponseStatusException if recipe not found or user doesn't have
   *                                 access
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

      // Verify user has access (owner or public)
      if (!userId.equals(recipe.getUserId()) && !recipe.isPublic()) {
        log.warn("User {} attempted to access private recipe {} owned by {}",
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
   * Delete a recipe by ID.
   *
   * @param recipeId The recipe ID
   * @param userId   The Firebase user ID (for authorization)
   * @throws ResponseStatusException if recipe not found or user doesn't have
   *                                 access
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

  /**
   * Map Recipe entity to RecipeResponse DTO.
   */
  private RecipeResponse mapToResponse(Recipe recipe) {
    Map<String, Object> nutritionMap = null;
    if (recipe.getNutritionalInfo() != null
        && recipe.getNutritionalInfo().getPerServing() != null) {
      // Simplified mapping: we return only perServing values as flat map to match API
      // contract
      nutritionMap = recipe.getNutritionalInfo().getPerServing().toMap();
    }

    Map<String, List<String>> tipsMap = null;
    if (recipe.getTips() != null) {
      // Simplified mapping for now, based on RecipeTips having simple conversion
      // available
      // Note: RecipeTips.toMap() currently returns null or specific map, need to
      // check implementation
      // The shared model toMap returns "substitutions" and "variations" only.
      try {
        tipsMap = recipe.getTips().toMap();
      } catch (Exception e) {
        log.warn("Failed to map tips", e);
        tipsMap = null;
      }
    }

    return RecipeResponse.builder()
        .id(recipe.getId())
        .userId(recipe.getUserId())
        .title(recipe.getRecipeName())
        .description(recipe.getDescription())
        .ingredients(recipe.getIngredients())
        .instructions(recipe.getInstructions())
        .prepTime(recipe.getPrepTimeMinutes())
        .cookTime(recipe.getCookTimeMinutes())
        .servings(recipe.getServings())
        .nutrition(nutritionMap)
        .tips(tipsMap)
        .imageUrl(recipe.getImageUrl())
        .source(recipe.getSource())
        .createdAt(recipe.getCreatedAt())
        .updatedAt(recipe.getUpdatedAt())
        .tags(recipe.getTags())
        .dietaryRestrictions(recipe.getDietaryRestrictions())
        .isPublic(recipe.isPublic())
        .build();
  }

  /**
   * Helper to map nutrition map to NutritionalInfo.
   */
  private NutritionalInfo mapToNutritionalInfo(Map<String, Object> nutritionMap) {
    if (nutritionMap == null) {
      return null;
    }
    return NutritionalInfo.builder()
        .perServing(com.recipe.shared.model.NutritionValues.fromMap(nutritionMap))
        .build();
  }

  /**
   * Helper to map tips map to RecipeTips.
   */
  private com.recipe.shared.model.RecipeTips mapToRecipeTips(Map<String, List<String>> tipsMap) {
    if (tipsMap == null) {
      return null;
    }
    return com.recipe.shared.model.RecipeTips.fromMap(tipsMap);
  }
}
