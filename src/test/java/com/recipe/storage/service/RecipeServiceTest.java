package com.recipe.storage.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import com.recipe.storage.dto.CreateRecipeRequest;
import com.recipe.storage.dto.RecipeResponse;
import com.recipe.shared.model.Recipe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    private RecipeService recipeService;

    @BeforeEach
    void setUp() {
        recipeService = new RecipeService();
        ReflectionTestUtils.setField(recipeService, "firestore", firestore);
        ReflectionTestUtils.setField(recipeService, "recipesCollection", "recipes");
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
