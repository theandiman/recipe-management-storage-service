# Recipe Storage Service

A Spring Boot microservice for persisting and managing recipe data using Google Cloud Firestore.

## Overview

The Recipe Storage Service provides a REST API for storing, retrieving, and managing recipes. It integrates with Firebase Authentication for security and uses Firestore as the NoSQL database backend.

## Technology Stack

- **Java 17**
- **Spring Boot 3.4.0**
- **Firebase Admin SDK 9.3.0** - Authentication & Firestore
- **Google Cloud Firestore** - NoSQL database
- **Lombok** - Reduce boilerplate code
- **OpenAPI/Swagger** - API documentation
- **Maven** - Build & dependency management

## Features

- ✅ Create and read operations for recipes
- ✅ Firebase Authentication integration
- ✅ Firestore database persistence
- ✅ OpenAPI/Swagger documentation
- ✅ CORS configuration for frontend integration
- ✅ Cloud Run deployment ready
- ✅ Comprehensive error handling

## Architecture

```
┌─────────────┐
│   Frontend  │
│  (React)    │
└──────┬──────┘
       │ HTTP + Firebase JWT
       ▼
┌─────────────────────────────┐
│  Recipe Storage Service     │
│  ┌───────────────────────┐  │
│  │ FirebaseAuthFilter    │  │ ← Validates JWT tokens
│  └───────────────────────┘  │
│  ┌───────────────────────┐  │
│  │ RecipeController      │  │ ← REST endpoints
│  └───────────────────────┘  │
│  ┌───────────────────────┐  │
│  │ RecipeService         │  │ ← Business logic
│  └───────────────────────┘  │
└──────────┬──────────────────┘
           │
           ▼
    ┌─────────────┐
    │  Firestore  │
    │  Database   │
    └─────────────┘
```

## API Endpoints

### Save Recipe
```http
POST /api/recipes
Authorization: Bearer <firebase-id-token>
Content-Type: application/json

{
  "title": "Spaghetti Carbonara",
  "description": "Classic Italian pasta",
  "ingredients": ["400g spaghetti", "200g pancetta", ...],
  "instructions": ["Boil pasta", "Fry pancetta", ...],
  "prepTime": 15,
  "cookTime": 20,
  "servings": 4,
  "nutrition": { "calories": 450, ... },
  "tips": {
    "substitutions": ["Use bacon instead of pancetta"],
    "storage": ["Store in airtight container for 3 days"]
  },
  "source": "ai-generated"
}
```

### Get Recipe by ID
```http
GET /api/recipes/{id}
Authorization: Bearer <firebase-id-token>
```

### Get User's Recipes
```http
GET /api/recipes/user/{userId}
Authorization: Bearer <firebase-id-token>
```

### Update Recipe
```http
PUT /api/recipes/{id}
Authorization: Bearer <firebase-id-token>
Content-Type: application/json
```

### Delete Recipe
```http
DELETE /api/recipes/{id}
Authorization: Bearer <firebase-id-token>
```

## Local Development

### Prerequisites

- Java 17+
- Maven 3.9+
- Firebase service account JSON file
- GCP project with Firestore enabled

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/theandiman/recipe-management-storage-service.git
   cd recipe-management-storage-service
   ```

2. **Set up Firebase credentials**
   ```bash
   export GOOGLE_APPLICATION_CREDENTIALS=/path/to/firebase-service-account.json
   ```

3. **Configure application properties**
   
   Create `src/main/resources/application-local.properties`:
   ```properties
   # Firebase
   firebase.project-id=your-firebase-project-id
   
   # Firestore
   firestore.collection.recipes=recipes
   
   # Authentication
   auth.enabled=true
   ```

4. **Build the project**
   ```bash
   mvn clean install
   ```

5. **Run the service**
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

   The service will be available at `http://localhost:8081`

6. **Access API documentation**
   
   Open http://localhost:8081/swagger-ui.html

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `production` |
| `FIREBASE_PROJECT_ID` | Firebase project ID | - |
| `GOOGLE_APPLICATION_CREDENTIALS` | Path to Firebase service account JSON | `/secrets/firebase-sa.json` |
| `AUTH_ENABLED` | Enable/disable Firebase auth | `true` |
| `FIRESTORE_COLLECTION_RECIPES` | Firestore collection name | `recipes` |

## Testing

```bash
# Run all tests
mvn test

# Run with coverage
mvn verify

# Run specific test
mvn test -Dtest=RecipeServiceTest
```

## Deployment

### Cloud Run

The service is automatically deployed to Google Cloud Run via Cloud Build when changes are pushed to `main`.

**Deployment configuration:**
- Region: `europe-west2`
- Min instances: 0
- Max instances: 10
- CPU: 1
- Memory: 512Mi
- Port: 8081

**Cloud Build triggers on:**
- Push to `main` branch
- Pull request creation

See `cloudbuild.yaml` for full CI/CD configuration.

### Manual Deployment

```bash
# Build Docker image
docker build -t recipe-storage-service .

# Tag for GCP Artifact Registry
docker tag recipe-storage-service \
  europe-west2-docker.pkg.dev/PROJECT_ID/recipe-storage/recipe-storage-service:latest

# Push to registry
docker push europe-west2-docker.pkg.dev/PROJECT_ID/recipe-storage/recipe-storage-service:latest

# Deploy to Cloud Run
gcloud run deploy recipe-storage-service \
  --image europe-west2-docker.pkg.dev/PROJECT_ID/recipe-storage/recipe-storage-service:latest \
  --region europe-west2 \
  --platform managed
```

## Authentication

The service uses Firebase Authentication with JWT tokens:

1. **Client obtains Firebase ID token** from Firebase Auth
2. **Client includes token** in `Authorization: Bearer <token>` header
3. **FirebaseAuthenticationFilter validates token** using Firebase Admin SDK
4. **User ID extracted** from token and added to request attributes
5. **Controller uses user ID** to scope recipe operations

**CORS Configuration:**
- Allowed origins: `http://localhost:5173`, Firebase Hosting URLs
- Allowed methods: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`
- Credentials: Enabled

## Project Structure

```
recipe-storage-service/
├── src/main/java/com/recipe/storage/
│   ├── config/
│   │   ├── CorsConfig.java           # CORS configuration
│   │   ├── FirebaseConfig.java       # Firebase initialization
│   │   └── OpenApiConfig.java        # Swagger documentation
│   ├── controller/
│   │   └── RecipeController.java     # REST endpoints
│   ├── dto/
│   │   ├── CreateRecipeRequest.java  # Request DTO
│   │   └── RecipeResponse.java       # Response DTO
│   ├── filter/
│   │   └── FirebaseAuthenticationFilter.java  # JWT validation
│   ├── model/
│   │   └── Recipe.java               # Firestore entity
│   ├── service/
│   │   └── RecipeService.java        # Business logic
│   └── RecipeStorageApplication.java # Main application
├── src/main/resources/
│   ├── application.properties         # Default config
│   └── application-local.properties   # Local dev config
├── cloudbuild.yaml                    # CI/CD pipeline
├── Dockerfile                         # Container image
└── pom.xml                           # Maven dependencies
```

## Troubleshooting

### Service returns 403 Forbidden

**Issue:** Cloud Run IAM policy blocking requests

**Solution:** Ensure Cloud Run service allows unauthenticated access (Firebase Auth handles authentication at application level):
```bash
gcloud run services add-iam-policy-binding recipe-storage-service \
  --region=europe-west2 \
  --member="allUsers" \
  --role="roles/run.invoker"
```

### Service returns 500 - Firestore database not found

**Issue:** Firestore database doesn't exist in GCP project

**Solution:** Create Firestore database:
```bash
gcloud firestore databases create \
  --location=europe-west2 \
  --project=YOUR_PROJECT_ID
```

### CORS errors in browser

**Issue:** OPTIONS preflight requests failing

**Solution:** Verify `CorsConfig` includes your frontend origin and `FirebaseAuthenticationFilter` allows OPTIONS requests without authentication.

### Local development - GOOGLE_APPLICATION_CREDENTIALS not set

**Issue:** Missing Firebase service account credentials

**Solution:** 
1. Download service account JSON from Firebase Console
2. Set environment variable:
   ```bash
   export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json
   ```

## Related Repositories

- [recipe-management-frontend](https://github.com/theandiman/recipe-management-frontend) - React TypeScript frontend
- [recipe-management-infrastructure](https://github.com/theandiman/recipe-management-infrastructure) - Terraform IaC
- [recipe-management-ai-service](https://github.com/theandiman/recipe-management-ai-service) - AI recipe generation

## Contributing

1. Create a feature branch
2. Make changes with tests
3. Ensure `mvn verify` passes
4. Create a pull request

## License

MIT
