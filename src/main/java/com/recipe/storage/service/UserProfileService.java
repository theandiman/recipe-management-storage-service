package com.recipe.storage.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.AggregateQuerySnapshot;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.recipe.storage.dto.UserProfileResponse;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service for fetching public user profile data.
 */
@Slf4j
@Service
public class UserProfileService {

  @Autowired(required = false)
  private Firestore firestore;

  @Autowired(required = false)
  private FollowService followService;

  @Value("${firestore.collection.users}")
  private String usersCollection;

  @Value("${firestore.collection.recipes}")
  private String recipesCollection;

  /**
   * Fetch the public profile for a given user uid.
   *
   * @param uid           The Firebase user ID of the profile to fetch
   * @param currentUserId The Firebase user ID of the authenticated caller, or {@code null} for
   *                      unauthenticated requests; used to populate {@code isFollowedByCurrentUser}
   * @return The public profile response
   * @throws ResponseStatusException 404 if the user does not exist,
   *     503 if Firestore is not configured or unavailable
   */
  public UserProfileResponse getUserProfile(String uid, String currentUserId) {
    if (firestore == null) {
      log.warn("Firestore not configured - cannot fetch user profile");
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "User profile service unavailable");
    }

    try {
      DocumentReference userDocRef = firestore.collection(usersCollection).document(uid);
      ApiFuture<DocumentSnapshot> userFuture = userDocRef.get();
      DocumentSnapshot userDocument = userFuture.get();

      if (userDocument == null || !userDocument.exists()) {
        log.warn("User profile not found: {}", uid);
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
      }

      String displayName = userDocument.getString("displayName");
      String bio = userDocument.getString("bio");
      String avatarUrl = userDocument.getString("avatarUrl");

      Long rawFollowerCount = userDocument.getLong("followerCount");
      long followerCount = rawFollowerCount != null ? rawFollowerCount : 0L;

      Long rawFollowingCount = userDocument.getLong("followingCount");
      long followingCount = rawFollowingCount != null ? rawFollowingCount : 0L;

      long publicRecipeCount = countPublicRecipes(uid);

      boolean isFollowedByCurrentUser = currentUserId != null
          && followService != null
          && followService.isFollowing(currentUserId, uid);

      log.info("Retrieved public profile for user {} (publicRecipeCount={}, followerCount={}, "
              + "followingCount={}, isFollowedByCurrentUser={})",
          uid, publicRecipeCount, followerCount, followingCount, isFollowedByCurrentUser);

      return UserProfileResponse.builder()
          .uid(uid)
          .displayName(displayName)
          .bio(bio)
          .avatarUrl(avatarUrl)
          .publicRecipeCount(publicRecipeCount)
          .followerCount(followerCount)
          .followingCount(followingCount)
          .isFollowedByCurrentUser(isFollowedByCurrentUser)
          .build();
    } catch (ResponseStatusException e) {
      throw e;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Interrupted while fetching user profile from Firestore", e);
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "User profile service unavailable", e);
    } catch (ExecutionException e) {
      log.error("Error fetching user profile from Firestore", e);
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "User profile service unavailable", e);
    }
  }

  private long countPublicRecipes(String uid) {
    try {
      ApiFuture<AggregateQuerySnapshot> countFuture = firestore.collection(recipesCollection)
          .whereEqualTo("userId", uid)
          .whereEqualTo("isPublic", true)
          .count()
          .get();
      AggregateQuerySnapshot snapshot = countFuture.get();
      return snapshot.getCount();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Interrupted while counting public recipes for user {}", uid, e);
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Failed to count public recipes",
          e);
    } catch (ExecutionException e) {
      log.error("Error counting public recipes for user {}", uid, e);
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Failed to count public recipes",
          e);
    }
  }
}
