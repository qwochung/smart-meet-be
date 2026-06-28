package com.example.smartmeetbe.service.impl;

import com.example.smartmeetbe.service.GeminiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiServiceImpl implements GeminiService {

    private final RestTemplate aiRestTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.prompt}")
    private String geminiPrompt;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    @Override
    public String smoothTranscript(String rawTranscript) {
        if (apiKey == null || apiKey.trim().isEmpty() || "YOUR_GEMINI_API_KEY".equals(apiKey)) {
            log.warn("Gemini API key is not configured or holds default placeholder. Skipping transcript smoothing.");
            return rawTranscript;
        }

        if (rawTranscript == null || rawTranscript.trim().isEmpty()) {
            return rawTranscript;
        }

        try {
            String url = GEMINI_API_URL + apiKey;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String fullInputText = geminiPrompt + "\n\n" + rawTranscript;

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of(
                                    "parts", List.of(
                                            Map.of("text", fullInputText)
                                    )
                            )
                    )
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.info("Sending transcript smoothing request to Gemini API...");
            ResponseEntity<Map> response = aiRestTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                List<?> candidates = (List<?>) body.get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map<?, ?> candidate = (Map<?, ?>) candidates.get(0);
                    Map<?, ?> content = (Map<?, ?>) candidate.get("content");
                    if (content != null) {
                        List<?> parts = (List<?>) content.get("parts");
                        if (parts != null && !parts.isEmpty()) {
                            Map<?, ?> part = (Map<?, ?>) parts.get(0);
                            String smoothedText = (String) part.get("text");
                            if (smoothedText != null && !smoothedText.trim().isEmpty()) {
                                log.info("Successfully smoothed transcript using Gemini.");
                                return smoothedText.trim();
                            }
                        }
                    }
                }
            }
            log.warn("Gemini response was successful but could not parse the response body. Returning raw transcript.");
        } catch (Exception e) {
            log.error("Failed to smooth transcript using Gemini API: {}", e.getMessage(), e);
        }

        return rawTranscript;
    }

    @Override
    public String generateSummary(String systemInstruction, String rawTranscript, String jsonSchema) {
        if (apiKey == null || apiKey.trim().isEmpty() || "YOUR_GEMINI_API_KEY".equals(apiKey)) {
            log.warn("Gemini API key is not configured. Skipping summary generation.");
            return "{}";
        }

        if (rawTranscript == null || rawTranscript.trim().isEmpty()) {
            return "{}";
        }

        try {
            String url = GEMINI_API_URL + apiKey;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> schemaMap = null;
            if (jsonSchema != null && !jsonSchema.trim().isEmpty()) {
                try {
                    schemaMap = objectMapper.readValue(jsonSchema, Map.class);
                } catch (Exception e) {
                    log.error("Failed to parse JSON schema string: {}", e.getMessage());
                }
            }

            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("responseMimeType", "application/json");
            if (schemaMap != null) {
                generationConfig.put("responseSchema", schemaMap);
            }

            Map<String, Object> requestBody = new HashMap<>(Map.of(
                    "contents", List.of(
                            Map.of(
                                    "parts", List.of(
                                            Map.of("text", rawTranscript)
                                    )
                            )
                    )
            ));

            if (systemInstruction != null && !systemInstruction.trim().isEmpty()) {
                requestBody.put("systemInstruction", Map.of(
                        "parts", List.of(
                                Map.of("text", systemInstruction)
                        )
                ));
            }

            requestBody.put("generationConfig", generationConfig);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.info("Sending dynamic prompting summary request to Gemini API...");
            ResponseEntity<Map> response = aiRestTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                List<?> candidates = (List<?>) body.get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map<?, ?> candidate = (Map<?, ?>) candidates.get(0);
                    Map<?, ?> content = (Map<?, ?>) candidate.get("content");
                    if (content != null) {
                        List<?> parts = (List<?>) content.get("parts");
                        if (parts != null && !parts.isEmpty()) {
                            Map<?, ?> part = (Map<?, ?>) parts.get(0);
                            String summaryText = (String) part.get("text");
                            if (summaryText != null && !summaryText.trim().isEmpty()) {
                                log.info("Successfully generated structured summary using Gemini.");
                                return summaryText.trim();
                            }
                        }
                    }
                }
            }
            log.warn("Gemini response was successful but could not parse the response body.");
        } catch (Exception e) {
            log.error("Failed to generate summary using Gemini API: {}", e.getMessage(), e);
        }

        return "{}";
    }
}
