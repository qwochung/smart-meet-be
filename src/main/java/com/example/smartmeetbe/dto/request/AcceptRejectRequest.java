package com.example.smartmeetbe.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AcceptRejectRequest {

    @NotNull(message = "userId is required")
    private Long userId;
}