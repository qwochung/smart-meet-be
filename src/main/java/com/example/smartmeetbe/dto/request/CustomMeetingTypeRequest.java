package com.example.smartmeetbe.dto.request;

import com.example.smartmeetbe.constant.MinutesFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CustomMeetingTypeRequest {

    @NotBlank
    @Size(max = 120)
    private String label;

    @Size(max = 500)
    private String description;

    /** Tên các {@code SummaryField} người dùng đã tích; bắt buộc có ít nhất một mục hợp lệ. */
    private List<String> extractFields;

    /** Yêu cầu riêng của người dùng, ghép vào cuối system prompt. Tùy chọn. */
    @Size(max = 1000)
    private String customInstruction;

    /** Mẫu biên bản gợi ý mặc định cho loại này; bỏ trống sẽ dùng ACTION. */
    private MinutesFormat defaultMinutesFormat;
}
