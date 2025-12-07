package com.recipe.storage.config;

import com.google.cloud.firestore.Firestore;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
@ConditionalOnProperty(name = "auth.enabled", havingValue = "false")
public class MockFirebaseConfig {

    @Bean
    public Firestore firestore() {
        return Mockito.mock(Firestore.class);
    }
}
