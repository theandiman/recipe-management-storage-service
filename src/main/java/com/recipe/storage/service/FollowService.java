package com.recipe.storage.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.SetOptions;
import com.google.cloud.firestore.Transaction;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service for managing follow/unfollow operations between users.
 *
 * <p>Follow documents are stored at {@code follows/{followerId}/following/{followedId}}.
 * Follower and following counts on user profile documents are updated atomically via
 * Firestore transactions to ensure idempotency.
 */
@Slf4j
@Service
public class FollowService {

  private static final String FOLLOWING_SUBCOLLECTION = "following";

  @Autowired(required = false)
  private Firestore firestore;

  @Value("${firestore.collection.follows}")
  private String followsCollection;

  @Value("${firestore.collection.users}")
  private String usersCollection;

  /**
   * Follow a user.
   *
   * <p>Creates a follow document and atomically increments {@code followingCount} on the
   * follower's profile and {@code followerCount} on the followed user's profile.
   * If the follow relationship already exists the operation is a no-op (idempotent).
   *
   * @param followerId the Firebase UID of the user performing the follow
   * @param followedId the Firebase UID of the user being followed
   * @throws ResponseStatusException 400 if followerId equals followedId,
   *     503 if Firestore is not configured or unavailable
   */
  public void followUser(String followerId, String followedId) {
    if (followerId.equals(followedId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot follow yourself");
    }

    if (firestore == null) {
      log.warn("Firestore not configured - cannot follow user");
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "Follow service unavailable");
    }

    DocumentReference followDocRef = firestore.collection(followsCollection)
        .document(followerId)
        .collection(FOLLOWING_SUBCOLLECTION)
        .document(followedId);

    DocumentReference followerUserRef = firestore.collection(usersCollection).document(followerId);
    DocumentReference followedUserRef = firestore.collection(usersCollection).document(followedId);

    try {
      ApiFuture<Boolean> txFuture = firestore.runTransaction(transaction -> {
        DocumentSnapshot followDoc = transaction.get(followDocRef).get();
        if (!followDoc.exists()) {
          transaction.set(followDocRef,
              Map.of("followedAt", FieldValue.serverTimestamp()));
          transaction.set(followerUserRef,
              Map.of("followingCount", FieldValue.increment(1)), SetOptions.merge());
          transaction.set(followedUserRef,
              Map.of("followerCount", FieldValue.increment(1)), SetOptions.merge());
          return true;
        }
        return false;
      });
      if (Boolean.TRUE.equals(txFuture.get())) {
        log.info("User {} followed user {}", followerId, followedId);
      } else {
        log.info("User {} already follows user {} - no-op", followerId, followedId);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Interrupted while following user {} -> {}", followerId, followedId, e);
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "Follow service unavailable", e);
    } catch (ExecutionException e) {
      log.error("Error while following user {} -> {}", followerId, followedId, e);
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "Follow service unavailable", e);
    }
  }

  /**
   * Unfollow a user.
   *
   * <p>Deletes the follow document and atomically decrements {@code followingCount} on the
   * follower's profile and {@code followerCount} on the followed user's profile.
   * If the follow relationship does not exist the operation is a no-op (idempotent).
   *
   * @param followerId the Firebase UID of the user performing the unfollow
   * @param followedId the Firebase UID of the user being unfollowed
   * @throws ResponseStatusException 400 if followerId equals followedId,
   *     503 if Firestore is not configured or unavailable
   */
  public void unfollowUser(String followerId, String followedId) {
    if (followerId.equals(followedId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot unfollow yourself");
    }

    if (firestore == null) {
      log.warn("Firestore not configured - cannot unfollow user");
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "Follow service unavailable");
    }

    DocumentReference followDocRef = firestore.collection(followsCollection)
        .document(followerId)
        .collection(FOLLOWING_SUBCOLLECTION)
        .document(followedId);

    DocumentReference followerUserRef = firestore.collection(usersCollection).document(followerId);
    DocumentReference followedUserRef = firestore.collection(usersCollection).document(followedId);

    try {
      ApiFuture<Boolean> txFuture = firestore.runTransaction(transaction -> {
        DocumentSnapshot followDoc = transaction.get(followDocRef).get();
        if (followDoc.exists()) {
          transaction.delete(followDocRef);
          transaction.set(followerUserRef,
              Map.of("followingCount", FieldValue.increment(-1)), SetOptions.merge());
          transaction.set(followedUserRef,
              Map.of("followerCount", FieldValue.increment(-1)), SetOptions.merge());
          return true;
        }
        return false;
      });
      if (Boolean.TRUE.equals(txFuture.get())) {
        log.info("User {} unfollowed user {}", followerId, followedId);
      } else {
        log.info("User {} does not follow user {} - no-op", followerId, followedId);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Interrupted while unfollowing user {} -> {}", followerId, followedId, e);
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "Follow service unavailable", e);
    } catch (ExecutionException e) {
      log.error("Error while unfollowing user {} -> {}", followerId, followedId, e);
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "Follow service unavailable", e);
    }
  }
}
