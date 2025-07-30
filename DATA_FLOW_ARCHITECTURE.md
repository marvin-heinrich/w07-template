# Data Flow Architecture

This document explains how data flows between the PostgreSQL database, Spring Boot server, client application, and LLM service in the mensa recommendation system.

## Overview

The application consists of four main components:
- **Client**: Frontend application for user interactions
- **Server**: Spring Boot API handling business logic and database operations
- **Database**: PostgreSQL storing user preferences
- **LLM Service**: AI service providing meal recommendations

## Database Schema

### Table: `user_preferences`
```sql
CREATE TABLE user_preferences (
    name VARCHAR(255) PRIMARY KEY,
    -- favorite_meals stored as @ElementCollection
);
```

**Entity**: `UserPreferences` (`server/src/main/java/de/tum/aet/devops25/w07/entity/UserPreferences.java:10`)
- `name` (String, Primary Key) - User identifier
- `favoriteMeals` (List<String>) - Collection of favorite meal names

## Data Flow Patterns

### 1. User Preference Management

#### Client → Server → Database
```
GET /api/preferences/{name}
├── UserPreferenceController:19
├── UserPreferenceService:18
└── UserPreferenceRepository (JPA)
```

#### Adding Preferences
```
POST /api/preferences/{name}?meal=...
├── UserPreferenceController:24
├── UserPreferenceService:25 (prevents duplicates)
└── Database UPDATE
```

#### Removing Preferences
```
DELETE /api/preferences/{name}?meal=...
├── UserPreferenceController:29
├── UserPreferenceService:48
└── Database UPDATE
```

### 2. Recommendation Flow

#### Complete Recommendation Process
```
GET /api/recommend/{name}
├── RecommendationController:30
├── Fetch user preferences from database
├── Get today's canteen menu
├── Send to LLM service for recommendation
└── Return recommendation to client
```

#### Detailed Steps:
1. **Fetch User Data**: `RecommendationController:32`
   ```java
   UserPreferences userPreferences = userPreferenceService.getPreferences(name);
   ```

2. **Get Menu Data**: `RecommendationController:39`
   ```java
   List<Dish> todaysMeals = canteenService.getTodayMeals("mensa-garching");
   ```

3. **LLM Service Call**: `LLMRecommendationService:25` (Currently TODO)
   - Input: User's `favoriteMeals` + today's `Dish` objects
   - Process: Convert dishes to meal names
   - Output: Recommendation string

### 3. LLM Service Integration

#### Request/Response DTOs

**RecommendRequest** (`server/src/main/java/de/tum/aet/devops25/w07/dto/RecommendRequest.java:7`)
```json
{
  "favorite_menu": ["Schnitzel", "Pizza"],
  "todays_menu": ["Pasta", "Salad", "Schnitzel"]
}
```

**RecommendResponse** (`server/src/main/java/de/tum/aet/devops25/w07/dto/RecommendResponse.java:5`)
```json
{
  "recommendation": "Based on your preferences, I recommend the Schnitzel today!"
}
```

#### REST Client Configuration
- **Service URL**: `${LLM_SERVICE_URL:http://localhost:5000}` (from `application.properties:22`)
- **Docker Service**: `http://llm:5000` (from `compose.yml:30`)
- **Client**: `LLMRestClient:17` configured with Spring's RestClient

## Data Transformation Pipeline

### Database → API Response
```
UserPreferences (JPA Entity)
├── name: String
└── favoriteMeals: List<String>
    ↓
JSON Response to Client
{
  "name": "john_doe",
  "favoriteMeals": ["Schnitzel", "Pizza"]
}
```

### API → LLM Service
```
UserPreferences.favoriteMeals + List<Dish>
    ↓ LLMRecommendationService:28
List<String> todayMealNames = todayMeals.stream()
    .map(Dish::name)
    .collect(Collectors.toList());
    ↓
RecommendRequest DTO → LLM Service
```

## Configuration

### Database Connection
```properties
# From application.properties:13-17
spring.datasource.url=${DATABASE_URL:jdbc:postgresql://localhost:5432/mensa}
spring.datasource.username=${POSTGRES_USER:postgres}
spring.datasource.password=${POSTGRES_PASSWORD:supersecret}
spring.jpa.hibernate.ddl-auto=update
```

### Docker Services
```yaml
# From compose.yml:47-54
database:
  image: postgres:17
  environment:
    - POSTGRES_DB=${POSTGRES_DB:-mensa}
    - POSTGRES_USER=${POSTGRES_USER:-postgres}
    - POSTGRES_PASSWORD=${POSTGRES_PASSWORD:-supersecret}
```

## Current Implementation Status

### ✅ Completed
- User preference CRUD operations
- Database schema and JPA entities
- REST API endpoints for preferences
- DTO definitions for LLM communication
- LLM service configuration

### ⚠️ TODO Items
- **RecommendationController:42**: LLM service call implementation
- **LLMRestClient:31-35**: REST request/response handling
- Complete recommendation flow integration

## Security Considerations

- User preferences are stored with simple string identifiers
- No authentication/authorization implemented
- LLM service communication over internal Docker network
- Database credentials managed via environment variables

## Error Handling

- **UserPreferenceService**: Validates non-null/non-empty inputs
- **LLMRecommendationService:35**: Catches exceptions from LLM service calls
- **RecommendationController:34**: Returns 204 No Content for missing preferences