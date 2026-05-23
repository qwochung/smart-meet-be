package com.example.smartmeetbe.controller;

import com.example.smartmeetbe.dto.request.AudioChunkRequest;
import com.example.smartmeetbe.service.AudioProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AudioTranscriptionController {

    private final AudioProcessingService audioProcessingService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/ws/rooms/{roomId}/audio")
    public void handleAudioChunk(
            @DestinationVariable String roomId,
            @Payload AudioChunkRequest payload) {

        log.info("Received audio chunk {} from participant {} in room {}",
                payload.getChunkIndex(), payload.getParticipantId(), roomId);

        audioProcessingService.processAudio(roomId, payload)
                .thenAccept(cleanText -> {
                    if (!cleanText.isBlank()) {
                        messagingTemplate.convertAndSend(
                                "/topic/rooms/" + roomId + "/transcript",
                                Map.of(
                                        "roomId", roomId,
                                        "participantId", payload.getParticipantId(),
                                        "participantName", payload.getParticipantName(),
                                        "chunkIndex", payload.getChunkIndex(),
                                        "text", cleanText
                                )
                        );
                    }
                })
                .exceptionally(error -> {
                    log.error("Failed to process audio chunk {} in room {}: {}",
                            payload.getChunkIndex(), roomId, error.getMessage());
                    return null;
                });
    }
}
