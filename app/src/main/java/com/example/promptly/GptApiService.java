package com.example.promptly;

import com.example.promptly.GptModels.GptRequest;
import com.example.promptly.GptModels.GptResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface GptApiService {
    @POST("v1/chat/completions")
    Call<GptResponse> getCompletion(@Body GptRequest request);
}