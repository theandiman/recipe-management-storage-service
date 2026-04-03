package com.recipe.storage.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.AggregateQuery;
import com.google.cloud.firestore.AggregateQuerySnapshot;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.recipe.storage.dto.UserProfileResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private Firestore firestore;

    private UserProfileService userProfileService;

    @BeforeEach
    void setUp() {
        userProfileService = new UserProfileService();
        ReflectionTestUtils.setField(userProfileService, "firestore", firestore);
        ReflectionTestUtils.setField(userProfileService, "usersCollection", "users");
        ReflectionTestUtils.setField(userProfileService, "recipesCollection", "recipes");
    }

    @Test
    void getUserProfile_WithFirestore_ReturnsProfile() throws ExecutionException, InterruptedException {
        // Arrange
        String uid = "user123";

        CollectionReference usersCollection = mock(CollectionReference.class);
        DocumentReference userDocRef = mock(DocumentReference.class);
        @SuppressWarnings("unchecked")
        ApiFuture<DocumentSnapshot> userFuture = mock(ApiFuture.class);
        DocumentSnapshot userSnapshot = mock(DocumentSnapshot.class);

        when(firestore.collection("users")).thenReturn(usersCollection);
        when(usersCollection.document(uid)).thenReturn(userDocRef);
        when(userDocRef.get()).thenReturn(userFuture);
        when(userFuture.get()).thenReturn(userSnapshot);
        when(userSnapshot.exists()).thenReturn(true);
        when(userSnapshot.getString("displayName")).thenReturn("Andy");
        when(userSnapshot.getString("bio")).thenReturn("I love pasta.");
        when(userSnapshot.getString("avatarUrl")).thenReturn("https://example.com/avatar.jpg");

        // Mock recipe count
        CollectionReference recipesCollection = mock(CollectionReference.class);
        Query uidQuery = mock(Query.class);
        Query publicQuery = mock(Query.class);
        AggregateQuery aggregateQuery = mock(AggregateQuery.class);
        @SuppressWarnings("unchecked")
        ApiFuture<AggregateQuerySnapshot> countFuture = mock(ApiFuture.class);
        AggregateQuerySnapshot countSnapshot = mock(AggregateQuerySnapshot.class);

        when(firestore.collection("recipes")).thenReturn(recipesCollection);
        when(recipesCollection.whereEqualTo("userId", uid)).thenReturn(uidQuery);
        when(uidQuery.whereEqualTo("isPublic", true)).thenReturn(publicQuery);
        when(publicQuery.count()).thenReturn(aggregateQuery);
        when(aggregateQuery.get()).thenReturn(countFuture);
        when(countFuture.get()).thenReturn(countSnapshot);
        when(countSnapshot.getCount()).thenReturn(12L);

        // Act
        UserProfileResponse response = userProfileService.getUserProfile(uid);

        // Assert
        assertNotNull(response);
        assertEquals(uid, response.getUid());
        assertEquals("Andy", response.getDisplayName());
        assertEquals("I love pasta.", response.getBio());
        assertEquals("https://example.com/avatar.jpg", response.getAvatarUrl());
        assertEquals(12L, response.getPublicRecipeCount());
    }

    @Test
    void getUserProfile_UserNotFound_Throws404() throws ExecutionException, InterruptedException {
        // Arrange
        String uid = "unknown-user";

        CollectionReference usersCollection = mock(CollectionReference.class);
        DocumentReference userDocRef = mock(DocumentReference.class);
        @SuppressWarnings("unchecked")
        ApiFuture<DocumentSnapshot> userFuture = mock(ApiFuture.class);
        DocumentSnapshot userSnapshot = mock(DocumentSnapshot.class);

        when(firestore.collection("users")).thenReturn(usersCollection);
        when(usersCollection.document(uid)).thenReturn(userDocRef);
        when(userDocRef.get()).thenReturn(userFuture);
        when(userFuture.get()).thenReturn(userSnapshot);
        when(userSnapshot.exists()).thenReturn(false);

        // Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userProfileService.getUserProfile(uid));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void getUserProfile_NullFirestore_Throws404() {
        // Arrange
        UserProfileService serviceWithoutFirestore = new UserProfileService();
        ReflectionTestUtils.setField(serviceWithoutFirestore, "usersCollection", "users");
        ReflectionTestUtils.setField(serviceWithoutFirestore, "recipesCollection", "recipes");

        // Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> serviceWithoutFirestore.getUserProfile("uid123"));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void getUserProfile_NullDocument_Throws404() throws ExecutionException, InterruptedException {
        // Arrange
        String uid = "user123";

        CollectionReference usersCollection = mock(CollectionReference.class);
        DocumentReference userDocRef = mock(DocumentReference.class);
        @SuppressWarnings("unchecked")
        ApiFuture<DocumentSnapshot> userFuture = mock(ApiFuture.class);

        when(firestore.collection("users")).thenReturn(usersCollection);
        when(usersCollection.document(uid)).thenReturn(userDocRef);
        when(userDocRef.get()).thenReturn(userFuture);
        when(userFuture.get()).thenReturn(null);

        // Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userProfileService.getUserProfile(uid));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void getUserProfile_ZeroPublicRecipes_ReturnsZeroCount()
            throws ExecutionException, InterruptedException {
        // Arrange
        String uid = "user456";

        CollectionReference usersCollection = mock(CollectionReference.class);
        DocumentReference userDocRef = mock(DocumentReference.class);
        @SuppressWarnings("unchecked")
        ApiFuture<DocumentSnapshot> userFuture = mock(ApiFuture.class);
        DocumentSnapshot userSnapshot = mock(DocumentSnapshot.class);

        when(firestore.collection("users")).thenReturn(usersCollection);
        when(usersCollection.document(uid)).thenReturn(userDocRef);
        when(userDocRef.get()).thenReturn(userFuture);
        when(userFuture.get()).thenReturn(userSnapshot);
        when(userSnapshot.exists()).thenReturn(true);
        when(userSnapshot.getString(anyString())).thenReturn(null);

        // Mock recipe count = 0
        CollectionReference recipesCollection = mock(CollectionReference.class);
        Query uidQuery = mock(Query.class);
        Query publicQuery = mock(Query.class);
        AggregateQuery aggregateQuery = mock(AggregateQuery.class);
        @SuppressWarnings("unchecked")
        ApiFuture<AggregateQuerySnapshot> countFuture = mock(ApiFuture.class);
        AggregateQuerySnapshot countSnapshot = mock(AggregateQuerySnapshot.class);

        when(firestore.collection("recipes")).thenReturn(recipesCollection);
        when(recipesCollection.whereEqualTo("userId", uid)).thenReturn(uidQuery);
        when(uidQuery.whereEqualTo("isPublic", true)).thenReturn(publicQuery);
        when(publicQuery.count()).thenReturn(aggregateQuery);
        when(aggregateQuery.get()).thenReturn(countFuture);
        when(countFuture.get()).thenReturn(countSnapshot);
        when(countSnapshot.getCount()).thenReturn(0L);

        // Act
        UserProfileResponse response = userProfileService.getUserProfile(uid);

        // Assert
        assertNotNull(response);
        assertEquals(uid, response.getUid());
        assertEquals(0L, response.getPublicRecipeCount());
    }
}
