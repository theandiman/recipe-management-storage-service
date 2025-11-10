package com.recipe.storage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new recipe.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRecipeRequest {

  @NotBlank(message = "Title is required")
  private String title;

  private String description;

  @NotEmpty(message = "At least one ingredient is required")
  private List<String> ingredients;

  @NotEmpty(message = "At least one instruction is required")
  private List<String> instructions;

  @Positive(message = "Prep time must be positive")
  private Integer prepTime;

  @Positive(message = "Cook time must be positive")
  private Integer cookTime;

  @NotNull(message = "Servings is required")
  @Positive(message = "Servings must be positive")
  private Integer servings;

  private Map<String, Object> nutrition;
  private Map<String, List<String>> tips;
  private String imageUrl;

  @NotBlank(message = "Source is required")
  private String source; // e.g., "ai-generated", "manual"

  private List<String> tags;
  private List<String> dietaryRestrictions;
}
