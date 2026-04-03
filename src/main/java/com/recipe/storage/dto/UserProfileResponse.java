package com.recipe.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for a user's public profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Public user profile response")
public class UserProfileResponse {

  @Schema(description = "Firebase user ID", example = "firebase-uid-123")
  private String uid;

  @Schema(description = "User's display name", example = "Jane Smith")
  private String displayName;

  @Schema(description = "User's bio", example = "Passionate home cook and food lover",
      nullable = true)
  private String bio;

  @Schema(description = "URL to the user's avatar image",
      example = "https://example.com/avatars/jane.jpg", nullable = true)
  private String avatarUrl;

  @Schema(description = "Number of public recipes created by this user", example = "12")
  private long publicRecipeCount;
}
