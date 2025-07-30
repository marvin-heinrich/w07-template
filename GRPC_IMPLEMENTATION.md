# gRPC Implementation Documentation

## Overview

This project implements a gRPC-based meal recommendation system that allows communication between the Java Spring Boot server and the Python LLM service. The implementation consists of:

1. **Protocol Buffer Definition** - Defines the service contract and message types
2. **Java Server Implementation** - Spring Boot gRPC server and client
3. **Python Service Implementation** - Python gRPC server using FastAPI and LangChain
4. **Generated Code** - Auto-generated stubs and classes for both languages

## Architecture

```
┌─────────────────┐    gRPC Call    ┌─────────────────┐
│   Java Server   │ ────────────────▶ │  Python LLM     │
│                 │                  │  Service        │
│ Spring Boot +   │ ◀──────────────── │                 │
│ gRPC Client     │    gRPC Response │ FastAPI +       │
│                 │                  │ gRPC Server     │
└─────────────────┘                  └─────────────────┘
```

## Protocol Buffer Definition

**File:** `llm/proto/meal_recommendation.proto` and `server/src/main/proto/meal_recommendation.proto`

```protobuf
syntax = "proto3";
package tum.mensa;

service MealRecommendationService {
  rpc RecommendMeal (MealRecommendationRequest) returns (MealRecommendationResponse);
}

message MealRecommendationRequest {
  string user_id = 1;
  repeated string favorite_meals = 2;
  repeated MenuMeal today_menu = 3;
}

message MenuMeal {
  string name = 1;
  string description = 2;
  repeated string tags = 3;
}

message MealRecommendationResponse {
  string recommended_meal_name = 1;
  string reasoning = 2;
}
```

### Message Types

- **MealRecommendationRequest**: Contains user ID, favorite meals list, and today's menu
- **MenuMeal**: Represents a single meal with name, description, and tags
- **MealRecommendationResponse**: Contains the recommended meal name and reasoning

## Java Implementation (Server Side)

### Build Configuration

**File:** `server/build.gradle`

Key gRPC dependencies and protobuf configuration:

```gradle
dependencies {
    implementation 'net.devh:grpc-spring-boot-starter:3.1.0.RELEASE'
    implementation 'io.grpc:grpc-netty-shaded:1.63.0'
    implementation 'io.grpc:grpc-protobuf:1.63.0'
    implementation 'io.grpc:grpc-stub:1.63.0'
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.24.4"
    }
    plugins {
        grpc {
            artifact = 'io.grpc:protoc-gen-grpc-java:1.63.0'
        }
    }
    generateProtoTasks {
        all()*.plugins {
            grpc {}
        }
    }
}
```

### gRPC Client Implementation

**File:** `server/src/main/java/de/tum/aet/devops25/w07/client/LLMGrpcClient.java`

The client creates a managed channel and blocking stub for synchronous communication:

```java
@Component
public class LLMGrpcClient {
    private final ManagedChannel channel;
    private final MealRecommendationServiceGrpc.MealRecommendationServiceBlockingStub blockingStub;

    public LLMGrpcClient(@Value("${llm.grpc.host:localhost}") String host,
                         @Value("${llm.grpc.port:50051}") int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.blockingStub = MealRecommendationServiceGrpc.newBlockingStub(channel);
    }
}
```

Key features:
- **Configuration**: Uses Spring properties for host/port configuration
- **Connection Management**: Manages gRPC channel lifecycle
- **Error Handling**: Catches exceptions and returns error responses
- **Timeout**: 30-second deadline for requests
- **Resource Cleanup**: Proper channel shutdown on destruction

### gRPC Service Implementation

**File:** `server/src/main/java/de/tum/aet/devops25/w07/grpc/MealRecommendationGrpcService.java`

A simple implementation that provides basic meal recommendation logic:

```java
@GrpcService
public class MealRecommendationGrpcService extends MealRecommendationServiceGrpc.MealRecommendationServiceImplBase {
    
    @Override
    public void recommendMeal(MealRecommendation.MealRecommendationRequest request,
                             StreamObserver<MealRecommendation.MealRecommendationResponse> responseObserver) {
        // Implementation logic
    }
}
```

The service implements basic recommendation logic:
1. Checks for favorite meals in today's menu
2. Returns first matching favorite meal if found
3. Falls back to first available meal if no favorites match

## Python Implementation (LLM Service)

### gRPC Server Implementation

**File:** `llm/main.py`

The Python service implements both FastAPI HTTP endpoints and gRPC server:

```python
class MealRecommendationService(meal_recommendation_pb2_grpc.MealRecommendationServiceServicer):
    def RecommendMeal(self, request, context):
        # Extract data from request
        user_id = request.user_id
        favorite_meals = list(request.favorite_meals)
        today_menu = [meal.name for meal in request.today_menu]
        
        # Use LangChain to generate recommendation
        recommendation = recommendation_chain.invoke({
            "favorite_menu": favorite_meals_str, 
            "todays_menu": todays_meals_str
        })
        
        return meal_recommendation_pb2.MealRecommendationResponse(
            recommended_meal_name=recommendation.strip(),
            reasoning=f"Recommended based on your favorites: {favorite_meals_str}"
        )
```

### Generated Python Code

**Files:** 
- `llm/meal_recommendation_pb2.py` - Message classes
- `llm/meal_recommendation_pb2_grpc.py` - Service stubs and server classes

The generated code provides:
- **Stub Classes**: For client-side calls
- **Servicer Classes**: Base classes for server implementation
- **Message Classes**: For request/response serialization

### Server Startup

The Python service runs both HTTP and gRPC servers concurrently:

```python
if __name__ == "__main__":
    # Start gRPC server in a separate thread
    grpc_thread = threading.Thread(target=serve_grpc)
    grpc_thread.daemon = True
    grpc_thread.start()

    # Start FastAPI server
    uvicorn.run("main:app", host="0.0.0.0", port=port)
```

## Configuration

### Docker Compose Configuration

**File:** `compose.yml`

```yaml
services:
  server:
    environment:
      - LLM_GRPC_HOST=llm
      - LLM_GRPC_PORT=50051
    
  llm:
    ports:
      - "5001:5000"  # HTTP port
      - "50051:50051"  # gRPC port
```

### Application Properties

The Java server uses these properties for gRPC client configuration:
- `llm.grpc.host`: LLM service hostname (default: localhost)
- `llm.grpc.port`: LLM service gRPC port (default: 50051)

## Communication Flow

1. **Request Initiation**: Java server receives HTTP request for meal recommendation
2. **gRPC Client Call**: `LLMGrpcClient` creates gRPC request with user data
3. **Network Transport**: Request sent over gRPC to Python service on port 50051
4. **Python Processing**: Python service uses LangChain and LLM to generate recommendation
5. **Response Return**: Python service returns gRPC response with recommendation
6. **HTTP Response**: Java server converts gRPC response to HTTP response

## Error Handling

### Java Client
- Connection timeouts (30 seconds)
- Exception catching with error responses
- Proper channel shutdown

### Python Server
- Request validation
- Exception catching with error responses
- Graceful server shutdown handling

## Code Generation

### Java
Generated files are created in `server/build/generated/source/proto/main/`:
- Service stubs and classes
- Message builders and serializers

### Python
Generated files in `llm/` directory:
- `meal_recommendation_pb2.py` - Message classes
- `meal_recommendation_pb2_grpc.py` - Service classes

## Testing

The gRPC implementation can be tested by:
1. Starting both services via Docker Compose
2. Making HTTP requests to the Java server
3. Observing gRPC communication in service logs
4. Using gRPC testing tools like grpcurl for direct gRPC calls

## Step-by-Step Implementation Guide

Follow these steps to add gRPC to a similar project from scratch:

### Step 1: Define Protocol Buffers

1. **Create proto directory structure:**
   ```bash
   mkdir -p server/src/main/proto
   mkdir -p llm/proto
   ```

2. **Create the .proto file** in both directories:
   ```protobuf
   // meal_recommendation.proto
   syntax = "proto3";
   package tum.mensa;

   service MealRecommendationService {
     rpc RecommendMeal (MealRecommendationRequest) returns (MealRecommendationResponse);
   }

   message MealRecommendationRequest {
     string user_id = 1;
     repeated string favorite_meals = 2;
     repeated MenuMeal today_menu = 3;
   }

   message MenuMeal {
     string name = 1;
     string description = 2;
     repeated string tags = 3;
   }

   message MealRecommendationResponse {
     string recommended_meal_name = 1;
     string reasoning = 2;
   }
   ```

### Step 2: Configure Java/Spring Boot Service

1. **Add dependencies to `build.gradle`:**
   ```gradle
   plugins {
       id 'com.google.protobuf' version '0.9.4'
   }

   dependencies {
       // gRPC dependencies
       implementation 'net.devh:grpc-spring-boot-starter:3.1.0.RELEASE'
       implementation 'io.grpc:grpc-netty-shaded:1.63.0'
       implementation 'io.grpc:grpc-protobuf:1.63.0'
       implementation 'io.grpc:grpc-stub:1.63.0'
       compileOnly 'org.apache.tomcat:annotations-api:6.0.53'
   }
   ```

2. **Configure protobuf plugin:**
   ```gradle
   protobuf {
       protoc {
           artifact = "com.google.protobuf:protoc:3.24.4"
       }
       plugins {
           grpc {
               artifact = 'io.grpc:protoc-gen-grpc-java:1.63.0'
           }
       }
       generateProtoTasks {
           all()*.plugins {
               grpc {}
           }
       }
   }

   sourceSets {
       main {
           proto {
               srcDir 'src/main/proto'
           }
       }
   }
   ```

3. **Generate code by running:**
   ```bash
   ./gradlew generateProto
   ```

### Step 3: Implement Java gRPC Client

1. **Create gRPC client class:**
   ```java
   @Component
   public class LLMGrpcClient {
       private final ManagedChannel channel;
       private final MealRecommendationServiceGrpc.MealRecommendationServiceBlockingStub blockingStub;

       public LLMGrpcClient(@Value("${llm.grpc.host:localhost}") String host,
                            @Value("${llm.grpc.port:50051}") int port) {
           this.channel = ManagedChannelBuilder.forAddress(host, port)
                   .usePlaintext()
                   .build();
           this.blockingStub = MealRecommendationServiceGrpc.newBlockingStub(channel);
       }

       public MealRecommendation.MealRecommendationResponse getRecommendation(
               String userId, List<String> favoriteMeals, List<String> todayMeals) {
           // Build request and make gRPC call
       }

       @PreDestroy
       public void shutdown() {
           channel.shutdown();
       }
   }
   ```

2. **Add configuration properties:**
   ```properties
   # application.properties
   llm.grpc.host=localhost
   llm.grpc.port=50051
   ```

### Step 4: Implement Java gRPC Server (Optional)

1. **Create gRPC service implementation:**
   ```java
   @GrpcService
   public class MealRecommendationGrpcService extends MealRecommendationServiceGrpc.MealRecommendationServiceImplBase {
       
       @Override
       public void recommendMeal(MealRecommendation.MealRecommendationRequest request,
                                StreamObserver<MealRecommendation.MealRecommendationResponse> responseObserver) {
           // Implement your logic here
           MealRecommendation.MealRecommendationResponse response = 
               MealRecommendation.MealRecommendationResponse.newBuilder()
                   .setRecommendedMealName("Your recommendation")
                   .setReasoning("Your reasoning")
                   .build();
           
           responseObserver.onNext(response);
           responseObserver.onCompleted();
       }
   }
   ```

### Step 5: Configure Python Service

1. **Add gRPC dependencies to `requirements.txt`:**
   ```txt
   grpcio==1.74.0
   grpcio-tools==1.74.0
   protobuf==3.20.3
   ```

2. **Generate Python code:**
   ```bash
   cd llm
   python -m grpc_tools.protoc -I./proto --python_out=. --grpc_python_out=. proto/meal_recommendation.proto
   ```

### Step 6: Implement Python gRPC Server

1. **Create gRPC service implementation:**
   ```python
   import grpc
   from concurrent import futures
   import meal_recommendation_pb2
   import meal_recommendation_pb2_grpc

   class MealRecommendationService(meal_recommendation_pb2_grpc.MealRecommendationServiceServicer):
       def RecommendMeal(self, request, context):
           try:
               # Extract data from request
               user_id = request.user_id
               favorite_meals = list(request.favorite_meals)
               today_menu = [meal.name for meal in request.today_menu]
               
               # Your recommendation logic here
               recommendation = "Generated recommendation"
               
               return meal_recommendation_pb2.MealRecommendationResponse(
                   recommended_meal_name=recommendation,
                   reasoning="Your reasoning here"
               )
           except Exception as e:
               return meal_recommendation_pb2.MealRecommendationResponse(
                   recommended_meal_name="Error",
                   reasoning=f"Error: {str(e)}"
               )
   ```

2. **Create server startup function:**
   ```python
   def serve_grpc():
       server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
       meal_recommendation_pb2_grpc.add_MealRecommendationServiceServicer_to_server(
           MealRecommendationService(), server
       )
       
       grpc_port = int(os.getenv("GRPC_PORT", 50051))
       listen_addr = f'[::]:{grpc_port}'
       server.add_insecure_port(listen_addr)
       
       print(f"Starting gRPC server on port {grpc_port}")
       server.start()
       server.wait_for_termination()
   ```

### Step 7: Update Docker Configuration

1. **Expose gRPC port in `compose.yml`:**
   ```yaml
   services:
     llm:
       ports:
         - "5001:5000"    # HTTP port
         - "50051:50051"  # gRPC port
       environment:
         - GRPC_PORT=50051
     
     server:
       environment:
         - LLM_GRPC_HOST=llm
         - LLM_GRPC_PORT=50051
   ```

### Step 8: Integration and Testing

1. **Wire gRPC client into your service:**
   ```java
   @Service
   public class RecommendationService {
       private final LLMGrpcClient grpcClient;
       
       public RecommendationService(LLMGrpcClient grpcClient) {
           this.grpcClient = grpcClient;
       }
       
       public String getRecommendation(String userId, List<String> favorites, List<String> menu) {
           var response = grpcClient.getRecommendation(userId, favorites, menu);
           return response.getRecommendedMealName();
       }
   }
   ```

2. **Test the implementation:**
   ```bash
   # Start services
   docker-compose up
   
   # Test with HTTP request to Java server
   curl -X POST http://localhost:8080/api/recommend \
     -H "Content-Type: application/json" \
     -d '{"userId": "test", "favorites": ["pizza"], "menu": ["pasta", "pizza"]}'
   ```

### Step 9: Production Considerations

1. **Add security (TLS):**
   ```java
   // For production, use TLS
   this.channel = ManagedChannelBuilder.forAddress(host, port)
           .useTransportSecurity()
           .build();
   ```

2. **Add monitoring and logging:**
   ```java
   // Add interceptors for monitoring
   this.blockingStub = MealRecommendationServiceGrpc.newBlockingStub(channel)
           .withInterceptors(new LoggingInterceptor());
   ```

3. **Configure load balancing:**
   ```java
   // Configure load balancing for multiple servers
   this.channel = ManagedChannelBuilder.forTarget("dns:///your-service:50051")
           .defaultLoadBalancingPolicy("round_robin")
           .build();
   ```

## Benefits of gRPC Implementation

1. **Type Safety**: Protocol buffers provide strong typing
2. **Performance**: Binary serialization and HTTP/2 transport
3. **Language Agnostic**: Works across Java and Python services
4. **Code Generation**: Automatic stub generation reduces boilerplate
5. **Streaming Support**: Ready for future streaming implementations
6. **Service Discovery**: Clean service interface definitions