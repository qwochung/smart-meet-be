package com.example.smartmeetbe.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Một dòng bật/tắt loại cuộc họp trong Cài đặt tài khoản. */
@Data
public class MeetingTypePreferenceRequest {

    @NotBlank
    private String typeCode;

    @NotNull
    private Boolean enabled;
}
