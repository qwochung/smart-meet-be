package com.example.smartmeetbe.service;

public interface TextDedupService {
    String deduplicate(String roomId, String participantId, String newText);
}
