package com.example.mamacook.AI;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {

    @POST("/analyze-recipe")
    Call<AIResponse> analyzeRecipe(@Body AIRequest request);
}