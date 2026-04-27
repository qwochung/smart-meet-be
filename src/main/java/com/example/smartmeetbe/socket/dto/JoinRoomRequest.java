package com.example.smartmeetbe.socket.dto;

import com.example.smartmeetbe.constant.Role;
import com.example.smartmeetbe.constant.RoomEventType;
import lombok.Data;

@Data
public class JoinRoomRequest {
    private Long actorId;
    private Long participantId;
    private String userName;
    private String avatar;
    private Role role;
    private RoomEventType type;
}
