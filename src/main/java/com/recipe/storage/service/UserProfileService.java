package com.recipe.storage.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.AggregateQuerySnapshot;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.recipe.storage.dto.UserProfileResponse;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service for fetching public user profile information.
 */
@Slf4j
@Service
public class UserProfileService {

  @Autowired(required = false)
  private FirebaseAuth firebaseAuth;

  @Autowired(required = false)
  private Firestore firestore;

  @Value("${firestore.collection.recipes}")
  private String recipesCollection;

  @Value("${firestore.collection.users}")
  private String usersCollection;

  /**
   * Retrieve the public profile for a given user ID.
   *
   * @param uid The Firebase user ID
   * @return The user's public profile
   * @throws ResponseStatusException with 404 if the uid is not found
   */
  public UserProfileResponse getUserProfile(String uid) {
    if (firebaseAuth == null) {
      log.warn("FirebaseAuth not configured - running in test mode");
      return buildMockProfile(uid);
    }

    UserRecord userRecord;
    try {
      userRecord = firebaseAuth.getUser(uid);
    } catch (FirebaseAuthException e) {
      if (AuthErrorCode.USER_NOT_FOUND.equals(e.getAuthErrorCode())) {
        log.info("User not found: {}", uid);
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + uid);
      }
      log.error("Failed to fetch user record for uid={}", uid, e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to fetch user profile", e);
    }

    if (userRecord == null) {
      log.info("User not found: {}", uid);
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + uid);
    }

    String bio = fetchBio(uid);
    long publicRecipeCount = countPublicRecipes(uid);

    return UserProfileResponse.builder()
        .uid(userRecord.getUid())
        .displayName(userRecord.getDisplayName())
        .bio(bio)
        .avatarUrl(userRecord.getPhotoUrl())
        .publicRecipeCount(publicRecipeCount)
        .build();
  }

  /**
   * Fetch bio from the Firestore users collection. Returns null if not available.
   */
  private String fetchBio(String uid) {
    if (firestore == null) {
      return null;
    }
    try {
      ApiFuture<DocumentSnapshot> future =
          firestore.collection(usersCollection).document(uid).get();
      DocumentSnapshot doc = future.get();
      if (doc != null && doc.exists()) {
        return doc.getString("bio");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("Interrupted while fetching bio for uid={}", uid);
    } catch (ExecutionException e) {
      log.warn("Failed to fetch bio for uid={}: {}", uid, e.getMessage());
    }
    return null;
  }

  /**
   * Count the number of public recipes for the given uid.
   */
  private long countPublicRecipes(String uid) {
    if (firestore == null) {
      return 0L;
    }
    try {
      Query query = firestore.collection(recipesCollection)
          .whereEqualTo("userId", uid)
          .whereEqualTo("isPublic", true);
      ApiFuture<AggregateQuerySnapshot> future = query.count().get();
      AggregateQuerySnapshot snapshot = future.get();
      return snapshot != null ? snapshot.getCount() : 0L;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("Interrupted while counting public recipes for uid={}", uid);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to fetch user profile");
    } catch (ExecutionException e) {
      log.warn("Failed to count public recipes for uid={}: {}", uid, e.getMessage());
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to fetch user profile");
    }
  }

  private UserProfileResponse buildMockProfile(String uid) {
    return UserProfileResponse.builder()
        .uid(uid)
        .displayName("Test User")
        .bio(null)
        .avatarUrl(null)
        .publicRecipeCount(0L)
        .build();
  }
}
