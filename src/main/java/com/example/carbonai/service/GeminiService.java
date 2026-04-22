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
                    "gemini-1.5-flash-latest", // Using the latest flash model
                    prompt,
                    null // No specific configuration
            );
            return response.text();
        } catch (Exception e) {
            System.err.println("Error calling Gemini API: " + e.getMessage());
            throw new RuntimeException("Error communicating with the Gemini API.", e);
        }
    }
}
