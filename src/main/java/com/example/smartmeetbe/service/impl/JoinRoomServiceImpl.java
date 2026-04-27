package com.example.smartmeetbe.service.impl;

import com.example.smartmeetbe.constant.JoinRoomStatus;
import com.example.smartmeetbe.constant.RoomEventType;
import com.example.smartmeetbe.constant.RoomStatus;
import com.example.smartmeetbe.entity.JoinRoom;
import com.example.smartmeetbe.entity.Room;
import com.example.smartmeetbe.entity.User;
import com.example.smartmeetbe.repository.JoinRoomRepository;
import com.example.smartmeetbe.service.JoinRoomService;
import com.example.smartmeetbe.service.RoomService;
import com.example.smartmeetbe.service.UserService;
import com.example.smartmeetbe.socket.dto.JoinRoomRequest;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@AllArgsConstructor
public class JoinRoomServiceImpl implements JoinRoomService {
    JoinRoomRepository joinRoomRepository;
    RoomService roomService;
    UserService userService;
    SimpMessagingTemplate messagingTemplate;
    
    @Override
    public void acceptJoinRoom(String code, Long userId){
        // TODO: Check permissions
        
        Room room = roomService.findByRoomCodeAndStatus(code, RoomStatus.WAITING);
        User user = userService.findById(userId);

        JoinRoom joinRoom = JoinRoom.builder()
                .room(room)
                .user(user)
                .status(JoinRoomStatus.APPROVED)
                .build();
        
        joinRoomRepository.save(joinRoom);

        JoinRoomRequest request = new JoinRoomRequest();
        request.setType(RoomEventType.JOIN_APPROVED);
        request.setParticipantId(userId);
        request.setUserName(user.getName());
        
        String destination = "/topic/room/" + code + "/user/" + userId;
        messagingTemplate.convertAndSend( destination, request);
    }
}
