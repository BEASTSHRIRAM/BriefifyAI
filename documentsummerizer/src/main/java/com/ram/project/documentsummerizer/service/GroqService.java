package com.ram.project.documentsummerizer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Service
public class GroqService {

    private static final Logger logger = Logger.getLogger(GroqService.class.getName());
    private final WebClient webClient;

    @Value("${groq.api.key}")
    private String groqApiKey;

    public GroqService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://api.groq.com/openai/v1") // Groq's OpenAI-compatible endpoint
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String summarizeText(String text) {
        String prompt = "Please summarize the following document concisely and accurately. Focus on key information, findings, and conclusions. If the document is about legal or medical topics, highlight critical clauses or diagnoses:\n\n" + text;

        // Groq API request body (OpenAI API compatible format)
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("messages", Collections.singletonList(message));
        requestBody.put("model", "llama3-8b-8192"); // Or "llama3-70b-8192" for a larger model if needed
        requestBody.put("temperature", 0.3); // Lower temperature for more factual summaries

        try {
            Map<String, Object> response = webClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + groqApiKey)
                    .body(BodyInserters.fromValue(requestBody))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(); // Block for synchronous call (for simplicity in this project)

            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    if (firstChoice.containsKey("message")) {
                        Map<String, Object> messageContent = (Map<String, Object>) firstChoice.get("message");
                        if (messageContent.containsKey("content")) {
                            String summary = (String) messageContent.get("content");
                            logger.info("Summary generated successfully.");
                            return summary;
                        }
                    }
                }
            }
            logger.warning("Groq API response did not contain expected summary content.");
            return "Error: Could not generate summary.";
        } catch (Exception e) {
            logger.severe("Error calling Groq API: " + e.getMessage());
            return "Error: Failed to connect to summarization service. " + e.getMessage();
        }
    }
}