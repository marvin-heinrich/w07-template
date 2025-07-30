package de.tum.aet.devops25.w07.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tum.mensa.MealRecommendation;
import tum.mensa.MealRecommendationServiceGrpc;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class LLMGrpcClient {

    private final ManagedChannel channel;
    private final MealRecommendationServiceGrpc.MealRecommendationServiceBlockingStub blockingStub;

    public LLMGrpcClient(@Value("${llm.grpc.host:localhost}") String host,
                         @Value("${llm.grpc.port:50051}") int port) {
        // Create a gRPC channel
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        
        // Create a blocking stub for synchronous calls
        this.blockingStub = MealRecommendationServiceGrpc.newBlockingStub(channel);
    }

    /**
     * Get meal recommendation from LLM service via gRPC
     *
     * @param userId        The user ID requesting the recommendation
     * @param favoriteMeals List of user's favorite meal names
     * @param todayMeals    List of today's available meal names
     * @return Recommendation response containing meal name and reasoning
     */
    public MealRecommendation.MealRecommendationResponse getRecommendation(
            String userId, List<String> favoriteMeals, List<String> todayMeals) {
        
        // Build today's menu items
        var todayMenuBuilder = MealRecommendation.MealRecommendationRequest.newBuilder();
        for (String mealName : todayMeals) {
            MealRecommendation.MenuMeal menuMeal = MealRecommendation.MenuMeal.newBuilder()
                    .setName(mealName)
                    .setDescription("Available meal")
                    .build();
            todayMenuBuilder.addTodayMenu(menuMeal);
        }

        // Build the request
        MealRecommendation.MealRecommendationRequest request = 
                MealRecommendation.MealRecommendationRequest.newBuilder()
                        .setUserId(userId)
                        .addAllFavoriteMeals(favoriteMeals)
                        .addAllTodayMenu(todayMenuBuilder.getTodayMenuList())
                        .build();

        // Make the gRPC call
        try {
            return blockingStub.withDeadlineAfter(30, TimeUnit.SECONDS)
                    .recommendMeal(request);
        } catch (Exception e) {
            // Return error response
            return MealRecommendation.MealRecommendationResponse.newBuilder()
                    .setRecommendedMealName("Error")
                    .setReasoning("Failed to get recommendation: " + e.getMessage())
                    .build();
        }
    }

    @PreDestroy
    public void shutdown() {
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}