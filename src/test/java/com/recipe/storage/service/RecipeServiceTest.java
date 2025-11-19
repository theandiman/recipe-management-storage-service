package com.recipe.storage.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import com.recipe.storage.dto.CreateRecipeRequest;
import com.recipe.storage.dto.RecipeResponse;
import com.recipe.storage.model.Recipe;
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
        
        com.google.cloud.firestore.CollectionReference collectionRef = mock(com.google.cloud.firestore.CollectionReference.class);
        when(firestore.collection(anyString())).thenReturn(collectionRef);
        when(collectionRef.document(anyString())).thenReturn(documentReference);
        when(documentReference.set(any(Recipe.class))).thenReturn(writeResultFuture);
        when(writeResultFuture.get()).thenReturn(writeResult);

        // Act
        RecipeResponse response = recipeService.saveRecipe(request, userId);

        // Assert
        assertNotNull(response);
        assertEquals(request.getRecipeName(), response.getRecipeName());
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
        assertEquals(request.getRecipeName(), response.getRecipeName());
        assertEquals(userId, response.getUserId());
        // Source comes from request, not hardcoded to "test-mode"
        assertEquals(request.getSource(), response.getSource());
    }

    @Test
    void saveRecipe_ValidatesRequiredFields() throws ExecutionException, InterruptedException {
        // Arrange
        CreateRecipeRequest request = createValidRequest();
        String userId = "user123";

        com.google.cloud.firestore.CollectionReference collectionRef = mock(com.google.cloud.firestore.CollectionReference.class);
        when(firestore.collection(anyString())).thenReturn(collectionRef);
        when(collectionRef.document(anyString())).thenReturn(documentReference);
        when(documentReference.set(any(Recipe.class))).thenReturn(writeResultFuture);
        when(writeResultFuture.get()).thenReturn(writeResult);

        // Act
        RecipeResponse response = recipeService.saveRecipe(request, userId);

        // Assert
        assertNotNull(response.getRecipeName());
        assertNotNull(response.getIngredients());
        assertNotNull(response.getInstructions());
        assertNotNull(response.getServings());
    }

    private CreateRecipeRequest createValidRequest() {
        return CreateRecipeRequest.builder()
            .recipeName("Delicious Pasta")
            .description("A wonderful Italian pasta dish")
            .ingredients(List.of("200g pasta", "2 tomatoes", "1 onion"))
            .instructions(List.of("Boil water", "Cook pasta", "Mix ingredients"))
            .prepTimeMinutes(15)
            .cookTimeMinutes(20)
            .servings(4)
            .source("ai-generated")
            .build();
    }
}
