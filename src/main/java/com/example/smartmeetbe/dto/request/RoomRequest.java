package com.example.smartmeetbe.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoomRequest {
    @NotBlank
    String name;
    String description;
    LocalDateTime scheduledAt;
}