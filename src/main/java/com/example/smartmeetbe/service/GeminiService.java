package com.example.smartmeetbe.service;

public interface GeminiService {
    String smoothTranscript(String rawTranscript);
    String generateSummary(String systemInstruction, String rawTranscript, String jsonSchema);
}
