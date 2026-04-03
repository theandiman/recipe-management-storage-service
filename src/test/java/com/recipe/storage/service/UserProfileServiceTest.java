package com.recipe.storage.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.AggregateQuery;
import com.google.cloud.firestore.AggregateQuerySnapshot;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private FirebaseAuth firebaseAuth;

    @Mock
    private Firestore firestore;

    @Mock
    private UserRecord userRecord;

    private UserProfileService userProfileService;

    @BeforeEach
    void setUp() {
        userProfileService = new UserProfileService();
        ReflectionTestUtils.setField(userProfileService, "firebaseAuth", firebaseAuth);
        ReflectionTestUtils.setField(userProfileService, "firestore", firestore);
        ReflectionTestUtils.setField(userProfileService, "recipesCollection", "recipes");
        ReflectionTestUtils.setField(userProfileService, "usersCollection", "users");
    }

    @Test
    void getUserProfile_WithFirebaseAuth_ReturnsProfile()
            throws FirebaseAuthException, ExecutionException, InterruptedException {
        // Arrange
        String uid = "user123";
        when(firebaseAuth.getUser(uid)).thenReturn(userRecord);
        when(userRecord.getUid()).thenReturn(uid);
        when(userRecord.getDisplayName()).thenReturn("Jane Smith");
        when(userRecord.getPhotoUrl()).thenReturn("https://example.com/photo.jpg");

        mockBioLookup(uid, null);
        mockPublicRecipeCount(uid, 5L);

        // Act
        UserProfileResponse response = userProfileService.getUserProfile(uid);

        // Assert
        assertNotNull(response);
        assertEquals(uid, response.getUid());
        assertEquals("Jane Smith", response.getDisplayName());
        assertNull(response.getBio());
        assertEquals("https://example.com/photo.jpg", response.getAvatarUrl());
        assertEquals(5L, response.getPublicRecipeCount());
    }

    @Test
    void getUserProfile_WithBioInFirestore_ReturnsBio()
            throws FirebaseAuthException, ExecutionException, InterruptedException {
        // Arrange
        String uid = "user456";
        when(firebaseAuth.getUser(uid)).thenReturn(userRecord);
        when(userRecord.getUid()).thenReturn(uid);
        when(userRecord.getDisplayName()).thenReturn("Bob Chef");
        when(userRecord.getPhotoUrl()).thenReturn(null);

        mockBioLookup(uid, "Passionate home cook");
        mockPublicRecipeCount(uid, 3L);

        // Act
        UserProfileResponse response = userProfileService.getUserProfile(uid);

        // Assert
        assertEquals("Passionate home cook", response.getBio());
        assertEquals(3L, response.getPublicRecipeCount());
        assertNull(response.getAvatarUrl());
    }

    @Test
    void getUserProfile_UnknownUid_ThrowsNotFoundException() throws FirebaseAuthException {
        // Arrange
        String uid = "unknown-uid";
        FirebaseAuthException notFound = mock(FirebaseAuthException.class);
        when(notFound.getAuthErrorCode()).thenReturn(AuthErrorCode.USER_NOT_FOUND);
        when(firebaseAuth.getUser(uid)).thenThrow(notFound);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userProfileService.getUserProfile(uid));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void getUserProfile_FirebaseAuthError_ThrowsInternalServerError() throws FirebaseAuthException {
        // Arrange
        String uid = "user789";
        FirebaseAuthException authError = mock(FirebaseAuthException.class);
        when(authError.getAuthErrorCode()).thenReturn(AuthErrorCode.INVALID_ID_TOKEN);
        when(firebaseAuth.getUser(uid)).thenThrow(authError);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userProfileService.getUserProfile(uid));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
    }

    @Test
    void getUserProfile_WithoutFirebaseAuth_ReturnsMockProfile() {
        // Arrange
        UserProfileService serviceWithoutAuth = new UserProfileService();
        ReflectionTestUtils.setField(serviceWithoutAuth, "recipesCollection", "recipes");
        ReflectionTestUtils.setField(serviceWithoutAuth, "usersCollection", "users");

        // Act
        UserProfileResponse response = serviceWithoutAuth.getUserProfile("test-uid");

        // Assert
        assertNotNull(response);
        assertEquals("test-uid", response.getUid());
        assertEquals("Test User", response.getDisplayName());
        assertNull(response.getBio());
        assertNull(response.getAvatarUrl());
        assertEquals(0L, response.getPublicRecipeCount());
    }

    // ---- helpers ----

    @SuppressWarnings("unchecked")
    private void mockBioLookup(String uid, String bio)
            throws ExecutionException, InterruptedException {
        CollectionReference usersCollection = mock(CollectionReference.class);
        when(firestore.collection("users")).thenReturn(usersCollection);
        DocumentReference docRef = mock(DocumentReference.class);
        when(usersCollection.document(uid)).thenReturn(docRef);
        ApiFuture<DocumentSnapshot> docFuture = mock(ApiFuture.class);
        when(docRef.get()).thenReturn(docFuture);
        DocumentSnapshot docSnapshot = mock(DocumentSnapshot.class);
        when(docFuture.get()).thenReturn(docSnapshot);
        if (bio != null) {
            when(docSnapshot.exists()).thenReturn(true);
            when(docSnapshot.getString("bio")).thenReturn(bio);
        } else {
            when(docSnapshot.exists()).thenReturn(false);
        }
    }

    @SuppressWarnings("unchecked")
    private void mockPublicRecipeCount(String uid, long count)
            throws ExecutionException, InterruptedException {
        CollectionReference recipesCollection = mock(CollectionReference.class);
        when(firestore.collection("recipes")).thenReturn(recipesCollection);
        Query userQuery = mock(Query.class);
        when(recipesCollection.whereEqualTo(eq("userId"), eq(uid))).thenReturn(userQuery);
        Query publicQuery = mock(Query.class);
        when(userQuery.whereEqualTo(eq("isPublic"), eq(true))).thenReturn(publicQuery);
        AggregateQuery aggregateQuery = mock(AggregateQuery.class);
        when(publicQuery.count()).thenReturn(aggregateQuery);
        ApiFuture<AggregateQuerySnapshot> countFuture = mock(ApiFuture.class);
        when(aggregateQuery.get()).thenReturn(countFuture);
        AggregateQuerySnapshot countSnapshot = mock(AggregateQuerySnapshot.class);
        when(countFuture.get()).thenReturn(countSnapshot);
        when(countSnapshot.getCount()).thenReturn(count);
    }
}
