package com.example.smartmeetbe.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoomRequest {
    String name;
    String description;
    Long hostId;
    LocalDateTime scheduledAt;
}
