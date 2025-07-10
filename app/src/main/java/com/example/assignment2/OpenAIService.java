package com.example.assignment2;

import android.content.Context;
import android.util.Log;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.net.SocketTimeoutException;

public class OpenAIService {
    private static final String TAG = "OpenAIService";
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    
    private final OkHttpClient client;
    private final String apiKey;
    
    public interface ChatCallback {
        void onSuccess(String response);
        void onError(String error);
    }
    
    public OpenAIService(Context context) {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)  // Reduced from 30 to 10 seconds
                .readTimeout(15, TimeUnit.SECONDS)     // Reduced from 30 to 15 seconds
                .writeTimeout(10, TimeUnit.SECONDS)    // Reduced from 30 to 10 seconds
                .retryOnConnectionFailure(true)        // Enable retries
                .build();
                
        // Get API key from resources
        this.apiKey = context.getString(R.string.openai_api_key);
    }
    
    public void sendMessage(String userMessage, ChatCallback callback) {
        if (apiKey == null || apiKey.isEmpty()) {
            callback.onError("OpenAI API key not configured");
            return;
        }
        
        try {
            JSONObject requestBody = createRequestBody(userMessage);
            
            Request request = new Request.Builder()
                    .url(OPENAI_API_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
                    .build();
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "API call failed", e);
                    String errorMessage = e instanceof SocketTimeoutException ? 
                        "Request timed out. Please try again." : 
                        "Network error: " + e.getMessage();
                    callback.onError(errorMessage);
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (!response.isSuccessful()) {
                            String errorBody = response.body() != null ? response.body().string() : "";
                            Log.e(TAG, "API error: " + errorBody);
                            callback.onError("API error: " + response.code());
                            return;
                        }

                        String responseBody = response.body().string();
                        String aiResponse = parseResponse(responseBody);
                        
                        if (aiResponse == null || aiResponse.trim().isEmpty()) {
                            callback.onError("Empty response from API");
                            return;
                        }
                        
                        callback.onSuccess(aiResponse);
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing response", e);
                        callback.onError("Error processing response: " + e.getMessage());
                    } finally {
                        response.close();
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error preparing request", e);
            callback.onError("Error preparing request: " + e.getMessage());
        }
    }
    
    private JSONObject createRequestBody(String userMessage) throws JSONException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "gpt-4o-mini");
        requestBody.put("max_tokens", 150);
        requestBody.put("temperature", 0.7);
        
        JSONArray messages = new JSONArray();
        
        // System message to give context about the app
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "You are a helpful assistant for a task management app called Assignment2. " +
                "Help users with questions about creating tasks, finding tasks, using the app features, " +
                "and general task management advice. Keep responses concise and helpful.");
        messages.put(systemMessage);
        
        // User message
        JSONObject userMessageObj = new JSONObject();
        userMessageObj.put("role", "user");
        userMessageObj.put("content", userMessage);
        messages.put(userMessageObj);
        
        requestBody.put("messages", messages);
        
        return requestBody;
    }
    
    private String parseResponse(String responseBody) throws JSONException {
        JSONObject jsonResponse = new JSONObject(responseBody);
        JSONArray choices = jsonResponse.getJSONArray("choices");
        
        if (choices.length() > 0) {
            JSONObject firstChoice = choices.getJSONObject(0);
            JSONObject message = firstChoice.getJSONObject("message");
            return message.getString("content").trim();
        }
        
        return "Sorry, I couldn't generate a response.";
    }
} 