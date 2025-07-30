package de.tum.aet.devops25.w07.grpc;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.stereotype.Service;
import tum.mensa.MealRecommendation;
import tum.mensa.MealRecommendationServiceGrpc;

@GrpcService
public class MealRecommendationGrpcService extends MealRecommendationServiceGrpc.MealRecommendationServiceImplBase {

    @Override
    public void recommendMeal(MealRecommendation.MealRecommendationRequest request,
                             StreamObserver<MealRecommendation.MealRecommendationResponse> responseObserver) {
        
        // Extract data from the request
        String userId = request.getUserId();
        var favoriteMeals = request.getFavoriteMealsList();
        var todayMenu = request.getTodayMenuList();
        
        // Simple recommendation logic for now
        String recommendedMeal = "Default Recommendation";
        String reasoning = "Based on your preferences";
        
        // Find first matching meal from today's menu that appears in favorites
        for (var menuMeal : todayMenu) {
            if (favoriteMeals.contains(menuMeal.getName())) {
                recommendedMeal = menuMeal.getName();
                reasoning = "Found your favorite meal '" + menuMeal.getName() + "' in today's menu!";
                break;
            }
        }
        
        // If no exact match, recommend the first meal from today's menu
        if (recommendedMeal.equals("Default Recommendation") && !todayMenu.isEmpty()) {
            var firstMeal = todayMenu.get(0);
            recommendedMeal = firstMeal.getName();
            reasoning = "Recommended based on today's available options";
        }
        
        // Build and send response
        MealRecommendation.MealRecommendationResponse response = 
            MealRecommendation.MealRecommendationResponse.newBuilder()
                .setRecommendedMealName(recommendedMeal)
                .setReasoning(reasoning)
                .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}