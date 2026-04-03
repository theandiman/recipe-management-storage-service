package com.recipe.storage.controller;

import com.recipe.storage.dto.UserProfileResponse;
import com.recipe.storage.service.FollowService;
import com.recipe.storage.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for public user profile operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Profiles", description = "APIs for accessing public user profile information")
public class UserProfileController {

  private final UserProfileService userProfileService;
  private final FollowService followService;

  /**
   * Get the public profile for a specific user.
   * Does NOT require authentication.
   *
   * @param uid The user's Firebase UID
   * @return The public user profile
   */
  @GetMapping("/{uid}/profile")
  @SecurityRequirements({})
  @Operation(
      summary = "Get a user's public profile",
      description = "Retrieves the public profile for a given user uid, including their display "
          + "name, bio, avatar URL, and count of public recipes. No authentication required.")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "User profile retrieved successfully",
          content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
      @ApiResponse(
          responseCode = "404",
          description = "User not found",
          content = @Content)
  })
  public ResponseEntity<UserProfileResponse> getUserProfile(
      @Parameter(description = "User Firebase UID", required = true) @PathVariable String uid) {

    MDC.put("user.profile.uid", uid);
    try {
      log.info("Fetching public profile for user {}", uid);
      UserProfileResponse profile = userProfileService.getUserProfile(uid);
      return ResponseEntity.ok(profile);
    } finally {
      MDC.remove("user.profile.uid");
    }
  }

  /**
   * Follow a user.
   * Requires Firebase authentication.
   *
   * @param uid    The Firebase UID of the user to follow
   * @param userId The authenticated caller's Firebase UID (injected by auth filter)
   * @return 204 No Content on success
   */
  @PostMapping("/{uid}/follow")
  @SecurityRequirement(name = "Firebase Auth")
  @Operation(
      summary = "Follow a user",
      description = "Creates a follow relationship from the authenticated caller to the specified "
          + "user. Increments followerCount on the followed user and followingCount on the "
          + "caller. Idempotent: calling multiple times is safe. Requires authentication.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Followed successfully",
          content = @Content),
      @ApiResponse(responseCode = "400", description = "Cannot follow yourself",
          content = @Content),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
  })
  public ResponseEntity<Void> followUser(
      @Parameter(description = "Firebase UID of the user to follow", required = true)
      @PathVariable String uid,
      @Parameter(hidden = true) @RequestAttribute("userId") String userId) {

    log.info("User {} following user {}", userId, uid);
    followService.followUser(userId, uid);
    return ResponseEntity.noContent().build();
  }

  /**
   * Unfollow a user.
   * Requires Firebase authentication.
   *
   * @param uid    The Firebase UID of the user to unfollow
   * @param userId The authenticated caller's Firebase UID (injected by auth filter)
   * @return 204 No Content on success
   */
  @DeleteMapping("/{uid}/follow")
  @SecurityRequirement(name = "Firebase Auth")
  @Operation(
      summary = "Unfollow a user",
      description = "Removes the follow relationship from the authenticated caller to the "
          + "specified user. Decrements followerCount on the unfollowed user and followingCount "
          + "on the caller. Idempotent: calling multiple times is safe. Requires authentication.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Unfollowed successfully",
          content = @Content),
      @ApiResponse(responseCode = "400", description = "Cannot unfollow yourself",
          content = @Content),
      @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
  })
  public ResponseEntity<Void> unfollowUser(
      @Parameter(description = "Firebase UID of the user to unfollow", required = true)
      @PathVariable String uid,
      @Parameter(hidden = true) @RequestAttribute("userId") String userId) {

    log.info("User {} unfollowing user {}", userId, uid);
    followService.unfollowUser(userId, uid);
    return ResponseEntity.noContent().build();
  }
}
