package com.recipe.storage.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
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
public class RecipeResponse {

  private String id;
  private String userId;
  private String title;
  private String description;
  private List<String> ingredients;
  private List<String> instructions;
  private Integer prepTime;
  private Integer cookTime;
  private Integer servings;
  private Map<String, Object> nutrition;
  private Map<String, List<String>> tips;
  private String imageUrl;
  private String source;
  private Instant createdAt;
  private Instant updatedAt;
  private List<String> tags;
  private List<String> dietaryRestrictions;
}
