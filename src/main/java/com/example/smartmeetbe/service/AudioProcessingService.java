package com.example.smartmeetbe.service;

import com.example.smartmeetbe.dto.request.AudioChunkRequest;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface AudioProcessingService {
    CompletableFuture<Map<String, Object>> processAudio(String roomId, AudioChunkRequest payload);
}
