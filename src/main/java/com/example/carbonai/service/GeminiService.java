package com.example.carbonai.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GeminiService {

    @Value("${app.gemini-api-key}")
    private String apiKey;

    @Value("${app.gemini-model:gemini-2.5-flash}")
    private String modelName;

    private Client client;

    //PostConstruct allows us to finish creating the GeminiService object before we try to use it.
    //This allows the apiKey to be injected into the Client object.
    @PostConstruct
    public void init() {
        //using Builder pattern to create a Client object
        //This basically says "Get the client builder, set the API key on it, and then build the final, ready-to-use client object."
        this.client = Client.builder().apiKey(apiKey).build();
    }

    public String generateAdvice(String prompt) {
        try {
            GenerateContentResponse response = client.models.generateContent(
                    modelName,
                    prompt,
                    null // No specific configuration
            );
            String text = response.text();
            if (text == null || text.isBlank()) {
                throw new IllegalStateException("Gemini returned an empty response for model: " + modelName);
            }
            return text;
        } catch (Exception e) {
            System.err.println("Error calling Gemini API with model " + modelName + ": " + e.getMessage());
            throw new RuntimeException("Error communicating with the Gemini API.", e);
        }
    }
}
