package com.example.smartmeetbe.dto.response;

import com.example.smartmeetbe.constant.RoomStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoomResponse {
    Long id;
    String name;
    String roomCode;
    RoomStatus status;
    UserResponse hostUser;
    LocalDateTime scheduledAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
