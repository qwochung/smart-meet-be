package com.example.smartmeetbe.socket.service;

import com.example.smartmeetbe.entity.User;
import com.example.smartmeetbe.repository.RoomRepository;
import com.example.smartmeetbe.service.JoinRoomService;
import com.example.smartmeetbe.service.UserService;
import com.example.smartmeetbe.socket.dto.JoinRoomRequest;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
public class RoomSocketService {
    RoomRepository roomRepository;
    SimpMessagingTemplate messagingTemplate;
    UserService userService;

    public void handleJoinRoom(String roomCode, JoinRoomRequest request){
        if (!roomRepository.existsByRoomCode(roomCode)){
            log.warn("Room with code {} does not exist. Ignoring join request: {}", roomCode, request);
            return;
        }
        if (request.getActorId() == null || request.getRole() == null || request.getType() == null){
            log.warn("Invalid join request for room {}: {}", roomCode, request);
            return;
        }

        User actor = userService.findById(request.getActorId());
        request.setUserName(actor.getName());
        request.setAvatar(actor.getAvatar());

        log.info("Received join response for room {}: {}", roomCode, request);

        switch (request.getType()){
            case JOIN_REQUEST -> {
                String hostChanel = "/topic/room/" + roomCode + "/host-events";
                log.info("Sending join request to host channel: {}", hostChanel);
                messagingTemplate.convertAndSend(hostChanel, request);
            }
            case JOIN_APPROVED -> {

                String participantChannel = "/topic/room/" + roomCode + "/participant-events/" + request.getParticipantId();
                log.info("Sending join response to participant channel: {}", participantChannel);

                messagingTemplate.convertAndSend(participantChannel, request);
            }
        }
        
    }
}
