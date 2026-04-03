package com.recipe.storage.controller;

import com.recipe.storage.dto.UserProfileResponse;
import com.recipe.storage.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for public user profile operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Profiles", description = "APIs for retrieving public user profile information")
public class UserProfileController {

  private final UserProfileService userProfileService;

  /**
   * Get the public profile of a user by their Firebase UID.
   * This is an unauthenticated endpoint.
   *
   * @param uid The Firebase user ID
   * @return The user's public profile
   */
  @GetMapping("/{uid}/profile")
  @Operation(
      summary = "Get public user profile",
      description = "Returns the public profile of a user including display name, bio, "
          + "avatar URL and the number of their public recipes. "
          + "Does not require authentication.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Profile found",
          content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
      @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
  })
  public ResponseEntity<UserProfileResponse> getUserProfile(
      @Parameter(description = "Firebase user ID", required = true)
      @PathVariable String uid) {

    log.info("Fetching public profile for uid={}", uid);
    UserProfileResponse profile = userProfileService.getUserProfile(uid);
    return ResponseEntity.ok(profile);
  }
}
