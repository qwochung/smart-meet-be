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
    // Mã loại cuộc họp: tên enum dựng sẵn hoặc mã loại người dùng tự tạo
    String typeCode;

    // Mẫu biên bản; bỏ trống sẽ lấy gợi ý mặc định theo typeCode
    com.example.smartmeetbe.constant.MinutesFormat minutesFormat;
}