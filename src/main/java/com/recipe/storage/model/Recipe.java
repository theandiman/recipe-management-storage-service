package com.recipe.storage.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Recipe entity representing a stored recipe.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Recipe {

  private String id; // Firestore document ID
  private String userId; // Firebase user ID who created/saved the recipe
  private String title;
  private String description;
  private List<String> ingredients;
  private List<String> instructions;
  private Integer prepTime; // in minutes
  private Integer cookTime; // in minutes
  private Integer servings;
  
  // Nutrition information
  private Map<String, Object> nutrition;
  
  // Tips from AI generation
  private Map<String, List<String>> tips;
  
  // Image URL if generated
  private String imageUrl;
  
  // Metadata
  private String source; // e.g., "ai-generated", "manual"
  private Instant createdAt;
  private Instant updatedAt;
  
  // Tags for categorization
  private List<String> tags;
  private List<String> dietaryRestrictions;
}
