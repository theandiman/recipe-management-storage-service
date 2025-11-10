# Recipe Storage Service

Spring Boot service for storing and managing recipes using Google Cloud Firestore.

## Features

- **Recipe Storage**: Save AI-generated or manually created recipes
- **Firestore Integration**: Serverless NoSQL database
- **Firebase Authentication**: Secure user-specific recipe storage
- **RESTful API**: Simple HTTP endpoints for recipe management

## Tech Stack

- **Spring Boot 3.4.0**
- **Java 17**
- **Google Cloud Firestore**
- **Firebase Admin SDK**
- **Maven**

## API Endpoints

### Save Recipe
```
POST /api/recipes
Authorization: Bearer <firebase-id-token>

Request Body:
{
  "title": "Chocolate Chip Cookies",
  "description": "Classic homemade cookies",
  "ingredients": ["flour", "sugar", "butter", "chocolate chips"],
  "instructions": ["Mix ingredients", "Bake at 350F"],
  "prepTime": 15,
  "cookTime": 12,
  "servings": 24,
  "nutrition": { "calories": 150, "protein": 2 },
  "tips": { "substitutions": ["Use dark chocolate"], "storage": ["Store in airtight container"] },
  "imageUrl": "https://example.com/image.jpg",
  "source": "ai-generated",
  "tags": ["dessert", "cookies"],
  "dietaryRestrictions": []
}

Response (201 Created):
{
  "id": "uuid-here",
  "userId": "firebase-user-id",
  "title": "Chocolate Chip Cookies",
  ...
  "createdAt": "2025-11-10T21:00:00Z",
  "updatedAt": "2025-11-10T21:00:00Z"
}
```

## Local Development

### Prerequisites
- Java 17
- Maven
- Firebase service account key

### Setup

1. Set environment variables:
```bash
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/firebase-sa.json
```

2. Run locally:
```bash
mvn spring-boot:run
```

The service will start on port 8081.

## Configuration

Key configuration in `application.properties`:

```properties
# Firebase
firebase.project.id=recipe-mgmt-dev

# Authentication
auth.enabled=true

# Firestore
firestore.collection.recipes=recipes
```

## Firestore Data Model

Recipes are stored in the `recipes` collection with the following structure:

```
recipes/{recipeId}
  - id: string (UUID)
  - userId: string (Firebase UID)
  - title: string
  - description: string
  - ingredients: array<string>
  - instructions: array<string>
  - prepTime: number (minutes)
  - cookTime: number (minutes)
  - servings: number
  - nutrition: map<string, any>
  - tips: map<string, array<string>>
  - imageUrl: string
  - source: string ("ai-generated" | "manual")
  - createdAt: timestamp
  - updatedAt: timestamp
  - tags: array<string>
  - dietaryRestrictions: array<string>
```

## Deployment

Deployed to Google Cloud Run via Cloud Build pipeline.

See [DEPLOYMENT.md](DEPLOYMENT.md) for details.

## Related Services

- **recipe-management-ai-service**: AI recipe generation with Gemini
- **recipe-management-frontend**: React frontend application
