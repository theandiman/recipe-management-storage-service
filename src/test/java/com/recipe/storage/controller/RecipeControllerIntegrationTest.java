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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
                .content(objectMapper.writeValueAsString(request)))
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
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getUserRecipes_Success() throws Exception {
        // First create a recipe
        CreateRecipeRequest request = CreateRecipeRequest.builder()
            .title("Test Recipe for List")
            .description("Test description")
            .ingredients(List.of("ingredient1", "ingredient2"))
            .instructions(List.of("step1", "step2"))
            .servings(2)
            .source("test")
            .build();

        mockMvc.perform(post("/api/recipes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("X-User-ID", "user-list-test"))
            .andExpect(status().isCreated());

        // Now fetch all recipes for the user
        mockMvc.perform(get("/api/recipes")
                .header("X-User-ID", "user-list-test"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getRecipe_Success() throws Exception {
        // First create a recipe
        CreateRecipeRequest request = CreateRecipeRequest.builder()
            .title("Specific Recipe")
            .description("Get this one")
            .ingredients(List.of("ingredient1"))
            .instructions(List.of("step1"))
            .servings(1)
            .source("test")
            .build();

        String response = mockMvc.perform(post("/api/recipes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("X-User-ID", "user456"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String recipeId = objectMapper.readTree(response).get("id").asText();

        // Now fetch the specific recipe
        mockMvc.perform(get("/api/recipes/" + recipeId)
                .header("X-User-ID", "user456"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(recipeId))
            .andExpect(jsonPath("$.title").value("Specific Recipe"));
    }

    @Test
    void getRecipe_NotFound() throws Exception {
        mockMvc.perform(get("/api/recipes/nonexistent-id")
                .header("X-User-ID", "user123"))
            .andExpect(status().isNotFound());
    }
}
