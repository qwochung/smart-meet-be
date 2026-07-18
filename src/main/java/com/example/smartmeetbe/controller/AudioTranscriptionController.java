package com.example.smartmeetbe.controller;

import com.example.smartmeetbe.dto.request.AudioChunkRequest;
import com.example.smartmeetbe.service.AudioProcessingService;
import com.example.smartmeetbe.service.impl.AudioInFlightTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AudioTranscriptionController {

    private final AudioProcessingService audioProcessingService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AudioInFlightTracker inFlightTracker;

    @MessageMapping("/ws/rooms/{roomId}/audio")
    public void handleAudioChunk(
            @DestinationVariable String roomId,
            @Payload AudioChunkRequest payload) {

        log.info("Received audio chunk {} from participant {} in room {}",
                payload.getChunkIndex(), payload.getParticipantId(), roomId);

        // +1 NGAY khi nhận (đồng bộ) để finalize không "qua mặt" chunk vừa tới
        inFlightTracker.begin(roomId);

        audioProcessingService.processAudio(roomId, payload)
                .whenComplete((result, error) -> {
                    try {
                        if (error != null) {
                            log.error("Failed to process audio chunk {} in room {}: {}",
                                    payload.getChunkIndex(), roomId, error.getMessage());
                        } else if (result != null) {
                            String text = (String) result.get("text");
                            boolean isFinal = Boolean.TRUE.equals(result.get("isFinal"));

                            if (text != null && !text.isBlank()) {
                                Map<String, Object> message = new HashMap<>();
                                message.put("roomId", roomId);
                                message.put("participantId", payload.getParticipantId());
                                message.put("participantName", payload.getParticipantName());
                                message.put("chunkIndex", payload.getChunkIndex());
                                message.put("text", text);
                                message.put("isFinal", isFinal);

                                messagingTemplate.convertAndSend(
                                        "/topic/rooms/" + roomId + "/transcript",
                                        message
                                );
                            }
                        }
                    } finally {
                        // -1 khi xử lý xong, và ack CHO MỌI chunk (kể cả im lặng/lỗi)
                        // để FE biết chunk đã được BE xử lý xong -> không đợi treo.
                        inFlightTracker.end(roomId);
                        publishChunkAck(roomId, payload.getChunkIndex());
                    }
                });
    }

    private void publishChunkAck(String roomId, Integer chunkIndex) {
        Map<String, Object> ack = new HashMap<>();
        ack.put("roomId", roomId);
        ack.put("chunkIndex", chunkIndex);
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/chunk-ack", ack);
    }
}
