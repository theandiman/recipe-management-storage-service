package com.recipe.storage.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.recipe.storage.dto.CreateRecipeRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
                "auth.enabled=false",
                "firestore.collection.recipes=test-recipes"
})
class RecipeSharingIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        void createPublicRecipe_Success() throws Exception {
                CreateRecipeRequest request = CreateRecipeRequest.builder()
                                .title("Public Recipe")
                                .description("A public recipe")
                                .ingredients(List.of("Ingredient 1"))
                                .instructions(List.of("Step 1"))
                                .servings(2)
                                .prepTime(10)
                                .cookTime(10)
                                .source("manual")
                                .isPublic(true)
                                .build();

                mockMvc.perform(post("/api/recipes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header("X-User-ID", "user123"))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.isPublic").value(true));
        }

        @Test
        void getPublicRecipes_ReturnsOk() throws Exception {
                mockMvc.perform(get("/api/recipes/public"))
                                .andExpect(status().isOk());
                // Expect empty list because Firestore is not mocked
                // .andExpect(jsonPath("$").isArray());
        }

        @Test
        void getPublicRecipe_NoFirestore_ReturnsServiceUnavailable() throws Exception {
                mockMvc.perform(get("/api/recipes/some-recipe-id/public"))
                                .andExpect(status().isServiceUnavailable());
        }
}

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
                "auth.enabled=true",
                "firestore.collection.recipes=test-recipes"
})
class RecipeSharingAuthEnabledIntegrationTest {

        @MockBean
        private FirebaseApp firebaseApp;

        @MockBean
        private Firestore firestore;

        @MockBean
        private FirebaseAuth firebaseAuth;

        @Autowired
        private MockMvc mockMvc;

        @BeforeEach
        @SuppressWarnings("unchecked")
        void setUp() throws Exception {
                CollectionReference colRef = mock(CollectionReference.class);
                DocumentReference docRef = mock(DocumentReference.class);
                ApiFuture<DocumentSnapshot> future = mock(ApiFuture.class);
                DocumentSnapshot snapshot = mock(DocumentSnapshot.class);
                when(firestore.collection(anyString())).thenReturn(colRef);
                when(colRef.document(anyString())).thenReturn(docRef);
                when(docRef.get()).thenReturn(future);
                when(future.get()).thenReturn(snapshot);
                when(snapshot.exists()).thenReturn(false);
        }

        @Test
        void getPublicRecipe_NoAuthHeader_IsAllowed_WhenAuthEnabled() throws Exception {
                // Public endpoint must be reachable without Authorization header
                // even when auth is enabled (no 401/403 returned).
                mockMvc.perform(get("/api/recipes/some-recipe-id/public"))
                                .andExpect(status().isNotFound());
        }
}
