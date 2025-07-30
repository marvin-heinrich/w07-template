package de.tum.aet.devops25.w07.service;

import de.tum.aet.devops25.w07.client.LLMRestClient;
import de.tum.aet.devops25.w07.client.LLMGrpcClient;
import de.tum.aet.devops25.w07.dto.Dish;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tum.mensa.MealRecommendation;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LLMRecommendationService {

    private final LLMRestClient llmRestClient;
    private final LLMGrpcClient llmGrpcClient;
    private final boolean useGrpc;

    public LLMRecommendationService(LLMRestClient llmRestClient, 
                                  LLMGrpcClient llmGrpcClient,
                                  @Value("${llm.use.grpc:true}") boolean useGrpc) {
        this.llmRestClient = llmRestClient;
        this.llmGrpcClient = llmGrpcClient;
        this.useGrpc = useGrpc;
    }

    /**
     * Get recommendation from LLM service using gRPC or REST API
     * @param favoriteMeals list of user's favorite meal names
     * @param todayMeals list of today's available dishes
     * @return recommendation as a string
     */
    public String getRecommendationFromLLM(List<String> favoriteMeals, List<Dish> todayMeals) {
<<<<<<< Updated upstream
        try {
            // Convert today's dishes to meal names
             List<String> todayMealNames = todayMeals.stream()
                    .map(Dish::name)
                    .collect(Collectors.toList());

            // TODO Call REST client
            return "";
=======
        return getRecommendationFromLLM("default-user", favoriteMeals, todayMeals);
    }
    
    /**
     * Get recommendation from LLM service using gRPC or REST API
     * @param userId user identifier
     * @param favoriteMeals list of user's favorite meal names
     * @param todayMeals list of today's available dishes
     * @return recommendation as a string
     */
    public String getRecommendationFromLLM(String userId, List<String> favoriteMeals, List<Dish> todayMeals) {
        try {
            // Convert today's dishes to meal names
            List<String> todayMealNames = todayMeals.stream()
                    .map(Dish::name)
                    .collect(Collectors.toList());

            if (useGrpc) {
                // Use gRPC client
                MealRecommendation.MealRecommendationResponse response = 
                    llmGrpcClient.getRecommendation(userId, favoriteMeals, todayMealNames);
                return response.getRecommendedMealName();
            } else {
                // Use REST client (fallback)
                return llmRestClient.generateRecommendations(favoriteMeals, todayMealNames);
            }
>>>>>>> Stashed changes

        } catch (Exception e) {
            System.err.println("Error fetching recommendation from LLM service: " + e.getMessage());
            return "";
        }
    }

}
