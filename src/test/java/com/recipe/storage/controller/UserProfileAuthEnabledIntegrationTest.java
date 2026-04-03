package com.recipe.storage.controller;

import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the user profile endpoint with authentication enabled.
 * Verifies that the endpoint is accessible without an Authorization header.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "auth.enabled=true",
        "firestore.collection.recipes=test-recipes",
        "firestore.collection.users=test-users"
})
class UserProfileAuthEnabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FirebaseApp firebaseApp;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    private Firestore mockFirestore;

    @MockBean
    private FirebaseAuth firebaseAuth;

    @Test
    void getUserProfile_NoAuthHeader_IsAllowed_WhenAuthEnabled() throws Exception {
        // The profile endpoint is whitelisted in the auth filter; no Authorization header
        // should be required even when auth is globally enabled.
        // With a mocked FirebaseAuth (returning null for getUser), the service returns 404.
        // What matters is that it is NOT 401 or 403 (i.e., the whitelist is working).
        mockMvc.perform(get("/api/users/some-uid/profile"))
                .andExpect(status().isNotFound());
    }
}
