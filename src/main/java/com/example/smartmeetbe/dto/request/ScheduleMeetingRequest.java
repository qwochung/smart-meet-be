package com.example.smartmeetbe.dto.request;

import com.example.smartmeetbe.constant.RecurrenceType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScheduleMeetingRequest {
    @NotBlank
    String name;

    String description;

    // Thời điểm bắt đầu của lần họp đầu tiên
    @NotNull
    LocalDateTime scheduledAt;

    // Kiểu lặp: NONE | DAILY | WEEKLY | MONTHLY
    @NotNull
    RecurrenceType recurrenceType;

    // Số lần lặp (tổng số buổi sẽ tạo); 1 = không lặp
    @Min(1)
    @Max(12)
    int occurrences = 1;

    com.example.smartmeetbe.constant.MeetingType typeCode;
}
