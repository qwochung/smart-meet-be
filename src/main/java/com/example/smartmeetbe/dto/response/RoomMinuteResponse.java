package com.example.smartmeetbe.dto.response;

import com.example.smartmeetbe.constant.RoomStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomMinuteResponse {
    String roomCode;
    String name;
    String description;
    LocalDateTime expiresAt;
    RoomStatus status;
    String summary;
}
