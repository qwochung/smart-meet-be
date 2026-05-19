package com.example.smartmeetbe.controller;

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
public class WhiteboardController {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/whiteboard/{roomCode}")
    public void handleWhiteboard(
            @DestinationVariable String roomCode,
            @Payload Map<String, Object> payload) {

        String type     = (String) payload.getOrDefault("type", "DRAW");
        String senderId = (String) payload.getOrDefault("senderId", "");

        // Giới hạn kích thước dataURL (~3MB base64 ≈ 2.25MB PNG)
        if ("DRAW".equals(type)) {
            String dataURL = (String) payload.get("dataURL");
            if (dataURL == null || dataURL.length() > 3_000_000) {
                log.warn("Whiteboard DRAW payload too large or null from sender {} in room {}",
                        senderId, roomCode);
                return;
            }
        }

        log.debug("Whiteboard {} from {} in room {}", type, senderId, roomCode);

        // Broadcast đến tất cả participant trong phòng (kể cả sender — FE tự lọc)
        messagingTemplate.convertAndSend(
                "/topic/whiteboard/" + roomCode,
                payload
        );
    }
}