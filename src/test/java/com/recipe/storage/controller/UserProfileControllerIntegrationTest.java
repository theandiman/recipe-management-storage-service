package com.recipe.storage.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the user profile endpoint with authentication disabled.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "auth.enabled=false",
        "firestore.collection.recipes=test-recipes",
        "firestore.collection.users=test-users"
})
class UserProfileControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getUserProfile_NoFirestore_ReturnsMockProfile() throws Exception {
        // Without FirebaseAuth configured (auth.enabled=false), the service returns a mock profile
        mockMvc.perform(get("/api/users/test-user/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid").value("test-user"))
                .andExpect(jsonPath("$.displayName").value("Test User"))
                .andExpect(jsonPath("$.publicRecipeCount").value(0));
    }

    @Test
    void getUserProfile_NoAuthHeaderRequired() throws Exception {
        // The endpoint must be accessible without any Authorization header
        mockMvc.perform(get("/api/users/any-uid/profile"))
                .andExpect(status().isOk());
    }
}
