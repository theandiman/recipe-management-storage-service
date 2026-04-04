package com.recipe.storage.dto;

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
public class UserProfileResponse {

  private String uid;
  private String displayName;
  private String bio;
  private String avatarUrl;
  private long publicRecipeCount;
  private long followerCount;
  private long followingCount;
  private boolean isFollowedByCurrentUser;
}
