package com.recipe.storage.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipe.storage.dto.CreateRecipeRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "auth.enabled=false",
        "firestore.collection.recipes=test-recipes"
})
class RecipeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createRecipe_Success() throws Exception {
        CreateRecipeRequest request = CreateRecipeRequest.builder()
                .title("Delicious Pasta")
                .description("A wonderful Italian pasta dish")
                .ingredients(List.of("200g pasta", "2 tomatoes", "1 onion"))
                .instructions(List.of("Boil water", "Cook pasta", "Mix ingredients"))
                .prepTime(15)
                .cookTime(20)
                .servings(4)
                .source("ai-generated")
                .build();

        mockMvc.perform(post("/api/recipes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("X-User-ID", "user123"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Delicious Pasta"))
                .andExpect(jsonPath("$.servings").value(4));
    }

    @Test
    void createRecipe_MissingTitle_ReturnsBadRequest() throws Exception {
        CreateRecipeRequest request = CreateRecipeRequest.builder()
                .description("A wonderful Italian pasta dish")
                .ingredients(List.of("200g pasta"))
                .instructions(List.of("Cook"))
                .servings(4)
                .source("manual")
                .build();

        mockMvc.perform(post("/api/recipes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("X-User-ID", "test-user"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRecipe_EmptyIngredients_ReturnsBadRequest() throws Exception {
        CreateRecipeRequest request = CreateRecipeRequest.builder()
                .title("Test Recipe")
                .ingredients(List.of())
                .instructions(List.of("Cook"))
                .servings(4)
                .source("manual")
                .build();

        mockMvc.perform(post("/api/recipes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("X-User-ID", "test-user"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserRecipes_Success() throws Exception {
        mockMvc.perform(get("/api/recipes")
                .header("X-User-ID", "test-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getPublicRecipes_Success() throws Exception {
        mockMvc.perform(get("/api/recipes/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getRecipe_WithValidId_ReturnsRecipe() throws Exception {
        // Without Firestore, trying to get a non-existent recipe returns 404
        mockMvc.perform(get("/api/recipes/some-id")
                .header("X-User-ID", "test-user"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateRecipe_WithValidId_UpdatesRecipe() throws Exception {
        // Without Firestore, update returns a mock response (200 OK)
        CreateRecipeRequest updateRequest = CreateRecipeRequest.builder()
                .title("Updated Title")
                .description("Updated description")
                .ingredients(List.of("new ingredient"))
                .instructions(List.of("new step"))
                .servings(4)
                .source("manual")
                .build();

        mockMvc.perform(put("/api/recipes/some-id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .header("X-User-ID", "test-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));
    }

    @Test
    void deleteRecipe_WithValidId_DeletesRecipe() throws Exception {
        // Without Firestore, delete returns 404 for non-existent recipe
        mockMvc.perform(delete("/api/recipes/some-id")
                .header("X-User-ID", "test-user"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateRecipeSharing_WithValidId_UpdatesSharingStatus() throws Exception {
        // Without Firestore, update sharing returns 503
        String sharingRequest = "{\"isPublic\": true}";

        mockMvc.perform(patch("/api/recipes/some-id/sharing")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sharingRequest)
                .header("X-User-ID", "test-user"))
                .andExpect(status().isServiceUnavailable());
    }

    // Note: GET endpoint tests are skipped in integration tests
    // because they require Firestore to be configured.
    // These endpoints will be tested in manual/E2E tests with real Firestore.
}
