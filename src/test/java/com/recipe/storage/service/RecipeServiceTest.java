package com.recipe.storage.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.recipe.storage.dto.CreateRecipeRequest;
import com.recipe.storage.dto.RecipeResponse;
import com.recipe.shared.model.Recipe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    private Firestore firestore;

    @Mock
    private DocumentReference documentReference;

    @Mock
    private ApiFuture<WriteResult> writeResultFuture;

    @Mock
    private WriteResult writeResult;

    @Mock
    private FirebaseAuth firebaseAuth;

    private RecipeService recipeService;

    @BeforeEach
    void setUp() {
        recipeService = new RecipeService();
        ReflectionTestUtils.setField(recipeService, "firestore", firestore);
        ReflectionTestUtils.setField(recipeService, "recipesCollection", "recipes");
        ReflectionTestUtils.setField(recipeService, "firebaseAuth", firebaseAuth);
    }

    @Test
    void saveRecipe_WithFirestore_Success() throws ExecutionException, InterruptedException {
        // Arrange
        CreateRecipeRequest request = createValidRequest();
        String userId = "user123";

        com.google.cloud.firestore.CollectionReference collectionRef = mock(
                com.google.cloud.firestore.CollectionReference.class);
        when(firestore.collection(anyString())).thenReturn(collectionRef);
        when(collectionRef.document(anyString())).thenReturn(documentReference);
        when(documentReference.set(any(Recipe.class))).thenReturn(writeResultFuture);
        when(writeResultFuture.get()).thenReturn(writeResult);

        // Act
        RecipeResponse response = recipeService.saveRecipe(request, userId);

        // Assert
        assertNotNull(response);
        assertEquals(request.getTitle(), response.getTitle());
        assertEquals(userId, response.getUserId());
        assertEquals(request.getSource(), response.getSource());
        verify(firestore).collection("recipes");
        verify(documentReference).set(any(Recipe.class));
    }

    @Test
    void saveRecipe_WithoutFirestore_ReturnsMockResponse() {
        // Arrange
        RecipeService serviceWithoutFirestore = new RecipeService();
        ReflectionTestUtils.setField(serviceWithoutFirestore, "recipesCollection", "recipes");
        CreateRecipeRequest request = createValidRequest();
        String userId = "user123";

        // Act
        RecipeResponse response = serviceWithoutFirestore.saveRecipe(request, userId);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(request.getTitle(), response.getTitle());
        assertEquals(userId, response.getUserId());
        // Source comes from request, not hardcoded to "test-mode"
        assertEquals(request.getSource(), response.getSource());
    }

    @Test
    void saveRecipe_ValidatesRequiredFields() throws ExecutionException, InterruptedException {
        // Arrange
        CreateRecipeRequest request = createValidRequest();
        String userId = "user123";

        com.google.cloud.firestore.CollectionReference collectionRef = mock(
                com.google.cloud.firestore.CollectionReference.class);
        when(firestore.collection(anyString())).thenReturn(collectionRef);
        when(collectionRef.document(anyString())).thenReturn(documentReference);
        when(documentReference.set(any(Recipe.class))).thenReturn(writeResultFuture);
        when(writeResultFuture.get()).thenReturn(writeResult);

        // Act
        RecipeResponse response = recipeService.saveRecipe(request, userId);

        // Assert
        assertNotNull(response.getTitle());
        assertNotNull(response.getIngredients());
        assertNotNull(response.getInstructions());
        assertNotNull(response.getServings());
    }

    @Test
    void updateRecipe_WithFirestore_Success() throws ExecutionException, InterruptedException {
        // Arrange
        String recipeId = "recipe123";
        String userId = "user123";
        CreateRecipeRequest request = createValidRequest();
        request.setTitle("Updated Title");

        com.google.cloud.firestore.CollectionReference collectionRef = mock(
                com.google.cloud.firestore.CollectionReference.class);
        when(firestore.collection(anyString())).thenReturn(collectionRef);
        when(collectionRef.document(recipeId)).thenReturn(documentReference);

        ApiFuture<com.google.cloud.firestore.DocumentSnapshot> futureSnapshot = mock(ApiFuture.class);
        com.google.cloud.firestore.DocumentSnapshot documentSnapshot = mock(
                com.google.cloud.firestore.DocumentSnapshot.class);
        when(documentReference.get()).thenReturn(futureSnapshot);
        when(futureSnapshot.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(true);

        com.recipe.shared.model.Recipe sharedRecipe = com.recipe.shared.model.Recipe.builder()
                .id(recipeId)
                .userId(userId)
                .recipeName("Old Title")
                .build();
        Recipe existingRecipe = sharedRecipe;
        when(documentSnapshot.toObject(Recipe.class)).thenReturn(existingRecipe);

        when(documentReference.set(any(Recipe.class))).thenReturn(writeResultFuture);
        when(writeResultFuture.get()).thenReturn(writeResult);

        // Act
        RecipeResponse response = recipeService.updateRecipe(recipeId, request, userId);

        // Assert
        assertNotNull(response);
        assertEquals("Updated Title", response.getTitle());
        verify(documentReference).set(any(Recipe.class));
    }

    @Test
    void updateRecipe_RecipeNotFound_ThrowsNotFoundException() throws ExecutionException, InterruptedException {
        // Arrange
        String recipeId = "nonexistent123";
        String userId = "user123";
        CreateRecipeRequest request = createValidRequest();

        com.google.cloud.firestore.CollectionReference collectionRef = mock(
                com.google.cloud.firestore.CollectionReference.class);
        when(firestore.collection(anyString())).thenReturn(collectionRef);
        when(collectionRef.document(recipeId)).thenReturn(documentReference);

        ApiFuture<com.google.cloud.firestore.DocumentSnapshot> futureSnapshot = mock(ApiFuture.class);
        com.google.cloud.firestore.DocumentSnapshot documentSnapshot = mock(
                com.google.cloud.firestore.DocumentSnapshot.class);
        when(documentReference.get()).thenReturn(futureSnapshot);
        when(futureSnapshot.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(false);

        // Act & Assert
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> recipeService.updateRecipe(recipeId, request, userId));
    }

    @Test
    void updateRecipe_UserDoesNotOwnRecipe_ThrowsForbiddenException() throws ExecutionException, InterruptedException {
        // Arrange
        String recipeId = "recipe123";
        String userId = "user123";
        String differentUserId = "user456";
        CreateRecipeRequest request = createValidRequest();

        com.google.cloud.firestore.CollectionReference collectionRef = mock(
                com.google.cloud.firestore.CollectionReference.class);
        when(firestore.collection(anyString())).thenReturn(collectionRef);
        when(collectionRef.document(recipeId)).thenReturn(documentReference);

        ApiFuture<com.google.cloud.firestore.DocumentSnapshot> futureSnapshot = mock(ApiFuture.class);
        com.google.cloud.firestore.DocumentSnapshot documentSnapshot = mock(
                com.google.cloud.firestore.DocumentSnapshot.class);
        when(documentReference.get()).thenReturn(futureSnapshot);
        when(futureSnapshot.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(true);

        com.recipe.shared.model.Recipe sharedRecipe = com.recipe.shared.model.Recipe.builder()
                .id(recipeId)
                .userId(differentUserId)
                .recipeName("Some Recipe")
                .build();
        Recipe existingRecipe = sharedRecipe;
        when(documentSnapshot.toObject(Recipe.class)).thenReturn(existingRecipe);

        // Act & Assert
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> recipeService.updateRecipe(recipeId, request, userId));
    }

    @Test
    void updateRecipe_DeserializationFailure_ThrowsInternalServerError()
            throws ExecutionException, InterruptedException {
        // Arrange
        String recipeId = "recipe123";
        String userId = "user123";
        CreateRecipeRequest request = createValidRequest();

        com.google.cloud.firestore.CollectionReference collectionRef = mock(
                com.google.cloud.firestore.CollectionReference.class);
        when(firestore.collection(anyString())).thenReturn(collectionRef);
        when(collectionRef.document(recipeId)).thenReturn(documentReference);

        ApiFuture<com.google.cloud.firestore.DocumentSnapshot> futureSnapshot = mock(ApiFuture.class);
        com.google.cloud.firestore.DocumentSnapshot documentSnapshot = mock(
                com.google.cloud.firestore.DocumentSnapshot.class);
        when(documentReference.get()).thenReturn(futureSnapshot);
        when(futureSnapshot.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(true);
        when(documentSnapshot.toObject(Recipe.class)).thenReturn(null);

        // Act & Assert
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> recipeService.updateRecipe(recipeId, request, userId));
    }

    @Test
    void updateRecipeSharing_Success() throws ExecutionException, InterruptedException {
        // Arrange
        String recipeId = "recipe123";
        String userId = "user123";
        boolean isPublic = true;

        com.google.cloud.firestore.CollectionReference collectionRef = mock(
                com.google.cloud.firestore.CollectionReference.class);
        when(firestore.collection(anyString())).thenReturn(collectionRef);
        when(collectionRef.document(recipeId)).thenReturn(documentReference);

        ApiFuture<com.google.cloud.firestore.DocumentSnapshot> futureSnapshot = mock(ApiFuture.class);
        com.google.cloud.firestore.DocumentSnapshot documentSnapshot = mock(
                com.google.cloud.firestore.DocumentSnapshot.class);
        when(documentReference.get()).thenReturn(futureSnapshot);
        when(futureSnapshot.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(true);

        Recipe existingRecipe = Recipe.builder()
                .id(recipeId)
                .userId(userId)
                .recipeName("Test Recipe")
                .publicRecipe(false)
                .build();
        when(documentSnapshot.toObject(Recipe.class)).thenReturn(existingRecipe);

        when(documentReference.update(eq("isPublic"), eq(isPublic), eq("updatedAt"), any())).thenReturn(writeResultFuture);
        when(writeResultFuture.get()).thenReturn(writeResult);

        // Act
        RecipeResponse response = recipeService.updateRecipeSharing(recipeId, isPublic, userId);

        // Assert
        assertNotNull(response);
        assertTrue(response.isPublic());
        verify(documentReference).update(eq("isPublic"), eq(isPublic), eq("updatedAt"), any());
    }

    @Test
    void updateRecipeSharing_UserDoesNotOwnRecipe_ThrowsForbidden() throws ExecutionException, InterruptedException {
        // Arrange
        String recipeId = "recipe123";
        String userId = "user123";
        String differentUserId = "user456";
        boolean isPublic = true;

        com.google.cloud.firestore.CollectionReference collectionRef = mock(
                com.google.cloud.firestore.CollectionReference.class);
        when(firestore.collection(anyString())).thenReturn(collectionRef);
        when(collectionRef.document(recipeId)).thenReturn(documentReference);

        ApiFuture<com.google.cloud.firestore.DocumentSnapshot> futureSnapshot = mock(ApiFuture.class);
        com.google.cloud.firestore.DocumentSnapshot documentSnapshot = mock(
                com.google.cloud.firestore.DocumentSnapshot.class);
        when(documentReference.get()).thenReturn(futureSnapshot);
        when(futureSnapshot.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(true);

        Recipe existingRecipe = Recipe.builder()
                .id(recipeId)
                .userId(differentUserId)
                .recipeName("Test Recipe")
                .publicRecipe(false)
                .build();
        when(documentSnapshot.toObject(Recipe.class)).thenReturn(existingRecipe);

        // Act & Assert
        org.springframework.web.server.ResponseStatusException exception = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> recipeService.updateRecipeSharing(recipeId, isPublic, userId));
        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void updateRecipeSharing_RecipeNotFound_ThrowsNotFoundException() throws ExecutionException, InterruptedException {
        // Arrange
        String recipeId = "nonexistent123";
        String userId = "user123";
        boolean isPublic = true;

        com.google.cloud.firestore.CollectionReference collectionRef = mock(
                com.google.cloud.firestore.CollectionReference.class);
        when(firestore.collection(anyString())).thenReturn(collectionRef);
        when(collectionRef.document(recipeId)).thenReturn(documentReference);

        ApiFuture<com.google.cloud.firestore.DocumentSnapshot> futureSnapshot = mock(ApiFuture.class);
        com.google.cloud.firestore.DocumentSnapshot documentSnapshot = mock(
                com.google.cloud.firestore.DocumentSnapshot.class);
        when(documentReference.get()).thenReturn(futureSnapshot);
        when(futureSnapshot.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(false);

        // Act & Assert
        org.springframework.web.server.ResponseStatusException exception = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> recipeService.updateRecipeSharing(recipeId, isPublic, userId));
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void updateRecipeSharing_FirestoreNotConfigured_ThrowsServiceUnavailable() {
        // Arrange
        RecipeService serviceWithoutFirestore = new RecipeService();
        ReflectionTestUtils.setField(serviceWithoutFirestore, "recipesCollection", "recipes");
        String recipeId = "recipe123";
        String userId = "user123";
        boolean isPublic = true;

        // Act & Assert
        org.springframework.web.server.ResponseStatusException exception = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> serviceWithoutFirestore.updateRecipeSharing(recipeId, isPublic, userId));
        assertEquals(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
    }

    @Test
    void getPublicRecipes_PopulatesAuthorDisplayName() throws Exception {
        // Arrange
        String userId = "user123";
        String displayName = "Jane Smith";

        CollectionReference collectionRef = mock(CollectionReference.class);
        Query query = mock(Query.class);
        ApiFuture<QuerySnapshot> queryFuture = mock(ApiFuture.class);
        QuerySnapshot querySnapshot = mock(QuerySnapshot.class);
        QueryDocumentSnapshot docSnapshot = mock(QueryDocumentSnapshot.class);
        UserRecord userRecord = mock(UserRecord.class);

        when(firestore.collection(anyString())).thenReturn(collectionRef);
        when(collectionRef.whereEqualTo("isPublic", true)).thenReturn(query);
        when(query.get()).thenReturn(queryFuture);
        when(queryFuture.get()).thenReturn(querySnapshot);

        Recipe recipe = Recipe.builder()
                .id("recipe1")
                .userId(userId)
                .recipeName("Public Recipe")
                .publicRecipe(true)
                .createdAt(Instant.now())
                .build();
        when(querySnapshot.getDocuments()).thenReturn(List.of(docSnapshot));
        when(docSnapshot.toObject(Recipe.class)).thenReturn(recipe);
        when(firebaseAuth.getUser(userId)).thenReturn(userRecord);
        when(userRecord.getDisplayName()).thenReturn(displayName);

        // Act
        List<RecipeResponse> responses = recipeService.getPublicRecipes();

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(displayName, responses.get(0).getAuthorDisplayName());
    }

    @Test
    void getPublicRecipes_FirebaseLookupFails_ReturnsNullDisplayName() throws Exception {
        // Arrange
        String userId = "user123";

        CollectionReference collectionRef = mock(CollectionReference.class);
        Query query = mock(Query.class);
        ApiFuture<QuerySnapshot> queryFuture = mock(ApiFuture.class);
        QuerySnapshot querySnapshot = mock(QuerySnapshot.class);
        QueryDocumentSnapshot docSnapshot = mock(QueryDocumentSnapshot.class);
        FirebaseAuthException authException = mock(FirebaseAuthException.class);

        when(firestore.collection(anyString())).thenReturn(collectionRef);
        when(collectionRef.whereEqualTo("isPublic", true)).thenReturn(query);
        when(query.get()).thenReturn(queryFuture);
        when(queryFuture.get()).thenReturn(querySnapshot);

        Recipe recipe = Recipe.builder()
                .id("recipe1")
                .userId(userId)
                .recipeName("Public Recipe")
                .publicRecipe(true)
                .createdAt(Instant.now())
                .build();
        when(querySnapshot.getDocuments()).thenReturn(List.of(docSnapshot));
        when(docSnapshot.toObject(Recipe.class)).thenReturn(recipe);
        when(firebaseAuth.getUser(userId)).thenThrow(authException);

        // Act
        List<RecipeResponse> responses = recipeService.getPublicRecipes();

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertNull(responses.get(0).getAuthorDisplayName());
    }

    @Test
    void getPublicRecipes_SameUserId_FirebaseLookupCalledOncePerUniqueUser() throws Exception {
        // Arrange
        String sharedUserId = "user123";
        String displayName = "Shared Author";

        CollectionReference collectionRef = mock(CollectionReference.class);
        Query query = mock(Query.class);
        ApiFuture<QuerySnapshot> queryFuture = mock(ApiFuture.class);
        QuerySnapshot querySnapshot = mock(QuerySnapshot.class);
        QueryDocumentSnapshot docSnapshot1 = mock(QueryDocumentSnapshot.class);
        QueryDocumentSnapshot docSnapshot2 = mock(QueryDocumentSnapshot.class);
        UserRecord userRecord = mock(UserRecord.class);

        when(firestore.collection(anyString())).thenReturn(collectionRef);
        when(collectionRef.whereEqualTo("isPublic", true)).thenReturn(query);
        when(query.get()).thenReturn(queryFuture);
        when(queryFuture.get()).thenReturn(querySnapshot);

        Recipe recipe1 = Recipe.builder()
                .id("recipe1")
                .userId(sharedUserId)
                .recipeName("Public Recipe 1")
                .publicRecipe(true)
                .createdAt(Instant.now())
                .build();
        Recipe recipe2 = Recipe.builder()
                .id("recipe2")
                .userId(sharedUserId)
                .recipeName("Public Recipe 2")
                .publicRecipe(true)
                .createdAt(Instant.now())
                .build();
        when(querySnapshot.getDocuments()).thenReturn(List.of(docSnapshot1, docSnapshot2));
        when(docSnapshot1.toObject(Recipe.class)).thenReturn(recipe1);
        when(docSnapshot2.toObject(Recipe.class)).thenReturn(recipe2);
        when(firebaseAuth.getUser(sharedUserId)).thenReturn(userRecord);
        when(userRecord.getDisplayName()).thenReturn(displayName);

        // Act
        List<RecipeResponse> responses = recipeService.getPublicRecipes();

        // Assert
        assertNotNull(responses);
        assertEquals(2, responses.size());
        responses.forEach(r -> assertEquals(displayName, r.getAuthorDisplayName()));
        // Firebase lookup must be called only once despite two recipes sharing the same userId
        verify(firebaseAuth, times(1)).getUser(sharedUserId);
    }

    @Test
    void getPublicRecipe_PopulatesAuthorDisplayName() throws Exception {
        // Arrange
        String recipeId = "recipe123";
        String userId = "user123";
        String displayName = "John Doe";

        com.google.cloud.firestore.CollectionReference collectionRef = mock(
                com.google.cloud.firestore.CollectionReference.class);
        when(firestore.collection(anyString())).thenReturn(collectionRef);
        when(collectionRef.document(recipeId)).thenReturn(documentReference);

        ApiFuture<com.google.cloud.firestore.DocumentSnapshot> futureSnapshot = mock(ApiFuture.class);
        com.google.cloud.firestore.DocumentSnapshot documentSnapshot = mock(
                com.google.cloud.firestore.DocumentSnapshot.class);
        when(documentReference.get()).thenReturn(futureSnapshot);
        when(futureSnapshot.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(true);

        Recipe recipe = Recipe.builder()
                .id(recipeId)
                .userId(userId)
                .recipeName("Public Recipe")
                .publicRecipe(true)
                .createdAt(Instant.now())
                .build();
        when(documentSnapshot.toObject(Recipe.class)).thenReturn(recipe);

        UserRecord userRecord = mock(UserRecord.class);
        when(firebaseAuth.getUser(userId)).thenReturn(userRecord);
        when(userRecord.getDisplayName()).thenReturn(displayName);

        // Act
        RecipeResponse response = recipeService.getPublicRecipe(recipeId);

        // Assert
        assertNotNull(response);
        assertEquals(recipeId, response.getId());
        assertEquals(displayName, response.getAuthorDisplayName());
        assertTrue(response.isPublic());
    }

    @Test
    void getPublicRecipe_FirebaseLookupFails_ReturnsNullDisplayName() throws Exception {
        // Arrange
        String recipeId = "recipe123";
        String userId = "user123";

        CollectionReference collectionRef = mock(CollectionReference.class);
        when(firestore.collection(anyString())).thenReturn(collectionRef);
        when(collectionRef.document(recipeId)).thenReturn(documentReference);

        ApiFuture<com.google.cloud.firestore.DocumentSnapshot> futureSnapshot = mock(ApiFuture.class);
        com.google.cloud.firestore.DocumentSnapshot documentSnapshot =
                mock(com.google.cloud.firestore.DocumentSnapshot.class);
        when(documentReference.get()).thenReturn(futureSnapshot);
        when(futureSnapshot.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(true);

        Recipe recipe = Recipe.builder()
                .id(recipeId)
                .userId(userId)
                .recipeName("Public Recipe")
                .publicRecipe(true)
                .createdAt(Instant.now())
                .build();
        when(documentSnapshot.toObject(Recipe.class)).thenReturn(recipe);

        FirebaseAuthException authException = mock(FirebaseAuthException.class);
        when(firebaseAuth.getUser(userId)).thenThrow(authException);

        // Act
        RecipeResponse response = recipeService.getPublicRecipe(recipeId);

        // Assert
        assertNotNull(response);
        assertNull(response.getAuthorDisplayName());
    }

    @Test
    void getPublicRecipe_PrivateRecipe_ThrowsNotFoundException() throws ExecutionException, InterruptedException {
        // Arrange
        String recipeId = "recipe123";

        com.google.cloud.firestore.CollectionReference collectionRef = mock(
                com.google.cloud.firestore.CollectionReference.class);
        when(firestore.collection(anyString())).thenReturn(collectionRef);
        when(collectionRef.document(recipeId)).thenReturn(documentReference);

        ApiFuture<com.google.cloud.firestore.DocumentSnapshot> futureSnapshot = mock(ApiFuture.class);
        com.google.cloud.firestore.DocumentSnapshot documentSnapshot = mock(
                com.google.cloud.firestore.DocumentSnapshot.class);
        when(documentReference.get()).thenReturn(futureSnapshot);
        when(futureSnapshot.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(true);

        Recipe recipe = Recipe.builder()
                .id(recipeId)
                .userId("owner123")
                .recipeName("Private Recipe")
                .publicRecipe(false)
                .build();
        when(documentSnapshot.toObject(Recipe.class)).thenReturn(recipe);

        // Act & Assert
        org.springframework.web.server.ResponseStatusException exception = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> recipeService.getPublicRecipe(recipeId));
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void getPublicRecipe_NonExistentRecipe_ThrowsNotFoundException() throws ExecutionException, InterruptedException {
        // Arrange
        String recipeId = "nonexistent123";

        com.google.cloud.firestore.CollectionReference collectionRef = mock(
                com.google.cloud.firestore.CollectionReference.class);
        when(firestore.collection(anyString())).thenReturn(collectionRef);
        when(collectionRef.document(recipeId)).thenReturn(documentReference);

        ApiFuture<com.google.cloud.firestore.DocumentSnapshot> futureSnapshot = mock(ApiFuture.class);
        com.google.cloud.firestore.DocumentSnapshot documentSnapshot = mock(
                com.google.cloud.firestore.DocumentSnapshot.class);
        when(documentReference.get()).thenReturn(futureSnapshot);
        when(futureSnapshot.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(false);

        // Act & Assert
        org.springframework.web.server.ResponseStatusException exception = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> recipeService.getPublicRecipe(recipeId));
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void getPublicRecipe_FirestoreNotConfigured_ThrowsServiceUnavailable() {
        // Arrange
        RecipeService serviceWithoutFirestore = new RecipeService();
        ReflectionTestUtils.setField(serviceWithoutFirestore, "recipesCollection", "recipes");
        String recipeId = "recipe123";

        // Act & Assert
        org.springframework.web.server.ResponseStatusException exception = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> serviceWithoutFirestore.getPublicRecipe(recipeId));
        assertEquals(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
    }

    private CreateRecipeRequest createValidRequest() {
        return CreateRecipeRequest.builder()
                .title("Delicious Pasta")
                .description("A wonderful Italian pasta dish")
                .ingredients(List.of("200g pasta", "2 tomatoes", "1 onion"))
                .instructions(List.of("Boil water", "Cook pasta", "Mix ingredients"))
                .prepTime(15)
                .cookTime(20)
                .servings(4)
                .source("ai-generated")
                .build();
    }
}
