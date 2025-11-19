package com.recipe.storage.model;

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
    recipe.setId(sharedRecipe.getId());
    recipe.setUserId(sharedRecipe.getUserId());
    recipe.setRecipeName(sharedRecipe.getRecipeName());
    recipe.setDescription(sharedRecipe.getDescription());
    recipe.setIngredients(sharedRecipe.getIngredients());
    recipe.setInstructions(sharedRecipe.getInstructions());
    recipe.setPrepTimeMinutes(sharedRecipe.getPrepTimeMinutes());
    recipe.setCookTimeMinutes(sharedRecipe.getCookTimeMinutes());
    recipe.setTotalTimeMinutes(sharedRecipe.getTotalTimeMinutes());
    recipe.setPrepTime(sharedRecipe.getPrepTime());
    recipe.setCookTime(sharedRecipe.getCookTime());
    recipe.setTotalTime(sharedRecipe.getTotalTime());
    recipe.setServings(sharedRecipe.getServings());
    recipe.setNutritionalInfo(sharedRecipe.getNutritionalInfo());
    recipe.setTips(sharedRecipe.getTips());
    recipe.setImageUrl(sharedRecipe.getImageUrl());
    recipe.setSource(sharedRecipe.getSource());
    recipe.setCreatedAt(sharedRecipe.getCreatedAt());
    recipe.setUpdatedAt(sharedRecipe.getUpdatedAt());
    recipe.setTags(sharedRecipe.getTags());
    recipe.setDietaryRestrictions(sharedRecipe.getDietaryRestrictions());
    recipe.setImageGeneration(sharedRecipe.getImageGeneration());

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
