package com.recipe.storage.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import java.io.FileInputStream;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Firebase and Firestore configuration.
 */
@Configuration
public class FirebaseConfig {

  @Value("${firebase.project.id}")
  private String projectId;

  /**
   * Initialize Firebase App with credentials.
   */
  @Bean
  public FirebaseApp firebaseApp() throws IOException {
    String credentialsPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
    
    if (credentialsPath == null || credentialsPath.isEmpty()) {
      throw new IllegalStateException(
          "GOOGLE_APPLICATION_CREDENTIALS environment variable not set");
    }

    FileInputStream serviceAccount = new FileInputStream(credentialsPath);

    FirebaseOptions options = FirebaseOptions.builder()
        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
        .setProjectId(projectId)
        .build();

    if (FirebaseApp.getApps().isEmpty()) {
      return FirebaseApp.initializeApp(options);
    }
    
    return FirebaseApp.getInstance();
  }

  /**
   * Get Firestore instance for database operations.
   */
  @Bean
  public Firestore firestore(FirebaseApp firebaseApp) {
    return FirestoreClient.getFirestore(firebaseApp);
  }
}
