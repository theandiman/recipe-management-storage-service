package com.recipe.storage.dto;

import com.recipe.shared.model.NutritionalInfo;
import com.recipe.shared.model.RecipeTips;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for recipe data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Recipe response with all recipe details including metadata")
public class RecipeResponse {

  @Schema(
      description = "Unique recipe identifier (Firestore document ID)",
      example = "abc123def456"
  )
  private String id;

  @Schema(
      description = "Firebase user ID of the recipe owner",
      example = "firebase-uid-123"
  )
  private String userId;

  @Schema(description = "Recipe name", example = "Spaghetti Carbonara")
  private String recipeName;

  @Schema(
      description = "Recipe description",
      example = "A classic Italian pasta dish with eggs, cheese, and pancetta"
  )
  private String description;

  @Schema(
      description = "List of ingredients with quantities",
      example = "[\"400g spaghetti\", \"200g pancetta\", \"4 large eggs\"]"
  )
  private List<String> ingredients;

  @Schema(
      description = "Step-by-step cooking instructions",
      example = "[\"Boil pasta in salted water\", \"Fry pancetta until crispy\"]"
  )
  private List<String> instructions;

  @Schema(description = "Preparation time in minutes", example = "15")
  private Integer prepTimeMinutes;

  @Schema(description = "Cooking time in minutes", example = "20")
  private Integer cookTimeMinutes;

  @Schema(description = "Total time in minutes", example = "35")
  private Integer totalTimeMinutes;

  @Schema(description = "Number of servings", example = "4")
  private Integer servings;

  @Schema(
      description = "Structured nutritional information",
      example = "{\"perServing\": {\"calories\": 450, \"protein\": 20}}"
  )
  private NutritionalInfo nutritionalInfo;

  @Schema(
      description = "Recipe tips and additional information",
      example = "{\"substitutions\": [\"Use turkey bacon instead of pancetta\"]}"
  )
  private RecipeTips tips;

  @Schema(
      description = "URL to recipe image",
      example = "https://example.com/images/carbonara.jpg"
  )
  private String imageUrl;

  @Schema(
      description = "Source of the recipe",
      example = "ai-generated",
      allowableValues = {"ai-generated", "manual"}
  )
  private String source;

  @Schema(
      description = "Recipe creation timestamp",
      example = "2024-01-15T10:30:00Z"
  )
  private Instant createdAt;

  @Schema(
      description = "Recipe last update timestamp",
      example = "2024-01-15T14:30:00Z"
  )
  private Instant updatedAt;

  @Schema(
      description = "Recipe tags for categorization",
      example = "[\"Italian\", \"Pasta\", \"Quick\"]"
  )
  private List<String> tags;

  @Schema(
      description = "Dietary restrictions/labels",
      example = "[\"Gluten-Free\", \"Vegetarian\"]"
  )
  private List<String> dietaryRestrictions;
}
