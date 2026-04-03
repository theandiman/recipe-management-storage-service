package com.recipe.storage.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.recipe.storage.dto.CreateRecipeRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

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
                // Firestore is not mocked, so returns an empty paged envelope
                mockMvc.perform(get("/api/recipes/public"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.recipes").isArray())
                                .andExpect(jsonPath("$.totalCount").value(0));
        }

        @Test
        void getPublicRecipe_NoFirestore_ReturnsNotFound() throws Exception {
                mockMvc.perform(get("/api/recipes/some-recipe-id/public"))
                                .andExpect(status().isNotFound());
        }
}

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
                "auth.enabled=true",
                "firestore.collection.recipes=test-recipes"
})
class RecipeSharingAuthEnabledIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private FirebaseApp firebaseApp;

        @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
        private Firestore mockFirestore;

        @Test
        void getPublicRecipe_NoAuthHeader_IsAllowed_WhenAuthEnabled() throws Exception {
                // Endpoint should be reachable without Authorization header even when auth is enabled.
                // Without Firestore the service returns 404 (not 401/403).
                mockMvc.perform(get("/api/recipes/some-recipe-id/public"))
                                .andExpect(status().isNotFound());
        }
}
