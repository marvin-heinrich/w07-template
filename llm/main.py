import os
import json
import requests
from typing import Dict, Any, List, Optional
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from langchain.llms.base import LLM
from langchain_core.prompts import PromptTemplate
from langchain.callbacks.manager import CallbackManagerForLLMRun
import grpc
from concurrent import futures
import meal_recommendation_pb2
import meal_recommendation_pb2_grpc

# Environment configuration
CHAIR_API_KEY = os.getenv("CHAIR_API_KEY")
API_URL = "https://gpu.aet.cit.tum.de/api/chat/completions"

# Create FastAPI application instance
app = FastAPI(
    title="LLM Recommendation Service",
    description="Service that generates personalized food recommendations using an LLM",
    version="1.0.0"
)


class RecommendRequest(BaseModel):
    """
    Request schema for recommendation endpoint.

    Attributes:
        favorite_menu (List[str]): User's favorite meal names
        todays_menu (List[str]): Today's available meal names
    """
    favorite_menu: List[str] = Field(..., description="User's favorite meal names")
    todays_menu: List[str] = Field(..., description="Today's available meal names")


class RecommendResponse(BaseModel):
    """
    Response schema for recommendation endpoint.

    Attributes:
        recommendation (str): The personalized recommendation string.
    """
    recommendation: str = Field(..., description="Personalized food recommendation")


class OpenWebUILLM(LLM):
    """
    Custom LangChain LLM wrapper for Open WebUI API.
    
    This class integrates the Open WebUI API with LangChain's LLM interface,
    allowing us to use the API in LangChain chains and pipelines.
    """
    
    api_url: str = API_URL
    api_key: str = CHAIR_API_KEY
    model_name: str = "llama3:latest"
    
    @property
    def _llm_type(self) -> str:
        return "open_webui"
    
    def _call(
        self,
        prompt: str,
        stop: Optional[List[str]] = None,
        run_manager: Optional[CallbackManagerForLLMRun] = None,
        **kwargs: Any,
    ) -> str:
        """
        Call the Open WebUI API to generate a response.
        
        Args:
            prompt: The input prompt to send to the model
            stop: Optional list of stop sequences
            run_manager: Optional callback manager for LangChain
            **kwargs: Additional keyword arguments
            
        Returns:
            The generated response text
            
        Raises:
            Exception: If API call fails
        """
        if not self.api_key:
            raise ValueError("CHAIR_API_KEY environment variable is required")
        
        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json",
        }
        
        # Build messages for chat completion
        messages = [
            {"role": "user", "content": prompt}
        ]
        
        payload = {
            "model": self.model_name,
            "messages": messages,
        }
        
        try:
            response = requests.post(
                self.api_url,
                headers=headers,
                json=payload,
                timeout=30
            )
            response.raise_for_status()
            
            result = response.json()
            
            # Extract the response content
            if "choices" in result and len(result["choices"]) > 0:
                content = result["choices"][0]["message"]["content"]
                return content.strip()
            else:
                raise ValueError("Unexpected response format from API")
                
        except requests.RequestException as e:
            raise Exception(f"API request failed: {str(e)}")
        except (KeyError, IndexError, ValueError) as e:
            raise Exception(f"Failed to parse API response: {str(e)}")


# Initialize the LLM
llm = OpenWebUILLM()

# Create the prompt template
recommendation_prompt = PromptTemplate(
    input_variables=["favorite_menu", "todays_menu"],
    template="""You are a helpful food recommendation assistant. Your task is to suggest exactly one dish from today's menu based on the user's preferences.

User's favorite meals: {favorite_menu}

Today's available meals: {todays_menu}

Based on the user's favorite meals, please recommend exactly ONE meal from today's available options. 
Consider:
- Similarity to the user's favorite meals
- Flavor profiles that match their preferences
- Availability in today's menu

IMPORTANT: You must respond with ONLY the exact name of one dish from today's menu. Do not include any explanations, additional text, punctuation, or formatting. Just return the dish name exactly as it appears in today's menu.

Example format: Spaghetti Carbonara

Recommendation:"""
)

# Create the chain using the new RunnableSequence pattern
recommendation_chain = recommendation_prompt | llm


class MealRecommendationService(meal_recommendation_pb2_grpc.MealRecommendationServiceServicer):
    """gRPC service implementation for meal recommendations."""
    
    def RecommendMeal(self, request, context):
        """
        gRPC method to recommend meals.
        
        Args:
            request: MealRecommendationRequest with user_id, favorite_meals, and today_menu
            context: gRPC context
            
        Returns:
            MealRecommendationResponse with recommended meal and reasoning
        """
        try:
            # Extract data from request
            user_id = request.user_id
            favorite_meals = list(request.favorite_meals)
            today_menu = [meal.name for meal in request.today_menu]
            
            if not favorite_meals:
                return meal_recommendation_pb2.MealRecommendationResponse(
                    recommended_meal_name="No recommendation available",
                    reasoning="No favorite meals provided"
                )
            
            if not today_menu:
                return meal_recommendation_pb2.MealRecommendationResponse(
                    recommended_meal_name="No recommendation available", 
                    reasoning="No meals available today"
                )
            
            # Format for LLM input
            favorite_meals_str = ", ".join(favorite_meals)
            todays_meals_str = ", ".join(today_menu)
            
            # Use LangChain to generate recommendation
            recommendation = recommendation_chain.invoke({
                "favorite_menu": favorite_meals_str, 
                "todays_menu": todays_meals_str
            })
            
            # Create response
            return meal_recommendation_pb2.MealRecommendationResponse(
                recommended_meal_name=recommendation.strip(),
                reasoning=f"Recommended based on your favorites: {favorite_meals_str}"
            )
            
        except Exception as e:
            print(f"Error in gRPC recommendation: {str(e)}")
            return meal_recommendation_pb2.MealRecommendationResponse(
                recommended_meal_name="Error generating recommendation",
                reasoning=f"An error occurred: {str(e)}"
            )


def serve_grpc():
    """Start the gRPC server."""
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    meal_recommendation_pb2_grpc.add_MealRecommendationServiceServicer_to_server(
        MealRecommendationService(), server
    )
    
    grpc_port = int(os.getenv("GRPC_PORT", 50051))
    listen_addr = f'[::]:{grpc_port}'
    server.add_insecure_port(listen_addr)
    
    print(f"Starting gRPC server on port {grpc_port}")
    server.start()
    
    try:
        server.wait_for_termination()
    except KeyboardInterrupt:
        print("Stopping gRPC server")
        server.stop(0)

@app.get("/health")
async def health_check():
    """Health check endpoint."""
    return {"status": "healthy", "service": "LLM Recommendation Service"}


@app.post(
    "/recommend",
    response_model=RecommendResponse,
    summary="Generate personalized food recommendation",
    description="Accepts user's favorite meals and today's menu, returns a personalized meal recommendation via Ollama."
)
async def recommend(req: RecommendRequest) -> RecommendResponse:
    """
    Generate a personalized food recommendation using LangChain and Ollama.
    
    Args:
        req: Request containing user's favorite meals and today's menu
        
    Returns:
        RecommendResponse containing the recommendation
        
    Raises:
        HTTPException: If the API call fails or other errors occur
    """
    try:
        if not req.favorite_menu:
            raise HTTPException(
                status_code=400, 
                detail="favorite_menu cannot be empty"
            )
        
        if not req.todays_menu:
            raise HTTPException(
                status_code=400, 
                detail="todays_menu cannot be empty"
            )
        
        # Format arrays as comma-separated strings for better processing
        favorite_meals_str = ", ".join(req.favorite_menu)
        todays_meals_str = ", ".join(req.todays_menu)
        
        # TODO Use LangChain to generate recommendation
        recommendation = ""
        
        # Return the LLM response as the recommendation
        return RecommendResponse(recommendation=recommendation)
        
    except HTTPException:
        # Re-raise HTTP exceptions as-is
        raise
    except Exception as e:
        # Log the error (in production, use proper logging)
        print(f"Error generating recommendation: {str(e)}")
        raise HTTPException(
            status_code=500, 
            detail=f"Failed to generate recommendation: {str(e)}"
        )


@app.get("/")
async def root():
    """Root endpoint with service information."""
    return {
        "service": "LLM Recommendation Service",
        "version": "1.0.0",
        "description": "Generates personalized food recommendations using LangChain and Open WebUI",
        "endpoints": {
            "health": "/health",
            "recommend": "/recommend",
            "docs": "/docs"
        }
    }

# Entry point for direct execution
if __name__ == "__main__":
    """
    Entry point for `python main.py` invocation.
    Starts both FastAPI HTTP server and gRPC server.
    """
    import uvicorn
    import threading

    # Start gRPC server in a separate thread
    grpc_thread = threading.Thread(target=serve_grpc)
    grpc_thread.daemon = True
    grpc_thread.start()

    # Start FastAPI server
    port = int(os.getenv("PORT", 5000))
    
    print(f"Starting LLM Recommendation Service on port {port}")
    print(f"API Documentation available at: http://localhost:{port}/docs")
    
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=port,
        reload=False  # Set to False when running both servers
    )
