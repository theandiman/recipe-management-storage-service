package com.recipe.storage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for Recipe Storage Service.
 * Handles recipe persistence using Firestore.
 */
@SpringBootApplication
public class RecipeStorageApplication {

  public static void main(String[] args) {
    SpringApplication.run(RecipeStorageApplication.class, args);
  }
}
