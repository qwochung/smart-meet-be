package com.example.smartmeetbe.socket.controller;

import com.example.smartmeetbe.socket.dto.JoinRoomRequest;
import com.example.smartmeetbe.socket.service.RoomSocketService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class RoomSocketController {
    RoomSocketService roomSocketService;
    
    @MessageMapping("/room/{roomCode}/join")
    public void handleJoinRoom(@DestinationVariable String roomCode, JoinRoomRequest request) {
       roomSocketService.handleJoinRoom(roomCode, request);
    }
}
