package com.recipe.storage.model;

import org.springframework.beans.BeanUtils;

/**
 * Recipe entity representing a stored recipe.
 * This extends the shared Recipe model with storage-specific functionality.
 */
public class Recipe extends com.recipe.shared.model.Recipe {

  // Additional storage-specific methods can be added here if needed

  /**
   * Creates a Recipe from the shared Recipe model.
   */
  public static Recipe fromShared(com.recipe.shared.model.Recipe sharedRecipe) {
    if (sharedRecipe == null) {
      return null;
    }

    Recipe recipe = new Recipe();
    // Copy all fields from shared recipe
    BeanUtils.copyProperties(sharedRecipe, recipe);

    return recipe;
  }

  /**
   * Converts to shared Recipe model.
   */
  public com.recipe.shared.model.Recipe toShared() {
    return com.recipe.shared.model.Recipe.builder()
        .id(getId())
        .userId(getUserId())
        .recipeName(getRecipeName())
        .description(getDescription())
        .ingredients(getIngredients())
        .instructions(getInstructions())
        .prepTimeMinutes(getPrepTimeMinutes())
        .cookTimeMinutes(getCookTimeMinutes())
        .totalTimeMinutes(getTotalTimeMinutes())
        .prepTime(getPrepTime())
        .cookTime(getCookTime())
        .totalTime(getTotalTime())
        .servings(getServings())
        .nutritionalInfo(getNutritionalInfo())
        .tips(getTips())
        .imageUrl(getImageUrl())
        .source(getSource())
        .createdAt(getCreatedAt())
        .updatedAt(getUpdatedAt())
        .tags(getTags())
        .dietaryRestrictions(getDietaryRestrictions())
        .imageGeneration(getImageGeneration())
        .build();
  }
}
