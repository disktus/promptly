package com.example.promptly;

import com.example.promptly.GeminiModels.GeminiRequest;
import com.example.promptly.GeminiModels.GeminiResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface GeminiApiService {
    // 최신 Gemini 2.0 Flash 모델 사용
    @POST("v1beta/models/gemini-2.0-flash:generateContent")
    Call<GeminiResponse> generateContent(
        @Query("key") String apiKey,
        @Body GeminiRequest request
    );
}