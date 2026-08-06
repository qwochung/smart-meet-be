package com.example.smartmeetbe.controller;

import com.example.smartmeetbe.constant.SummaryField;
import com.example.smartmeetbe.dto.request.CustomMeetingTypeRequest;
import com.example.smartmeetbe.dto.request.MeetingTypePreferenceRequest;
import com.example.smartmeetbe.dto.response.ApiResponse;
import com.example.smartmeetbe.dto.response.MeetingTypeResponse;
import com.example.smartmeetbe.service.MeetingTypeService;
import com.example.smartmeetbe.utils.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Quản lý loại cuộc họp của người dùng hiện tại: bật/tắt loại dựng sẵn,
 * tạo - sửa - xoá loại tự tạo.
 */
@RestController
@RequestMapping("/meeting-types")
@RequiredArgsConstructor
public class MeetingTypeController {

    private final MeetingTypeService meetingTypeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MeetingTypeResponse>>> getMeetingTypes(
            @RequestParam(name = "enabledOnly", defaultValue = "false") boolean enabledOnly) {
        String email = SecurityUtil.getCurrentUser();
        List<MeetingTypeResponse> types = enabledOnly
                ? meetingTypeService.getEnabledMeetingTypes(email)
                : meetingTypeService.getMeetingTypes(email);
        return ResponseEntity.ok(ApiResponse.<List<MeetingTypeResponse>>builder()
                .success(true)
                .message("Meeting types retrieved successfully")
                .data(types)
                .build());
    }

    /** Danh mục các mục nội dung để dựng checkbox ở màn Cài đặt tài khoản. */
    @GetMapping("/summary-fields")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getSummaryFields() {
        List<Map<String, String>> fields = Arrays.stream(SummaryField.values())
                .map(field -> Map.of(
                        "value", field.name(),
                        "label", field.getLabel()))
                .toList();
        return ResponseEntity.ok(ApiResponse.<List<Map<String, String>>>builder()
                .success(true)
                .message("Summary fields retrieved successfully")
                .data(fields)
                .build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MeetingTypeResponse>> createCustomType(
            @Valid @RequestBody CustomMeetingTypeRequest request) {
        String email = SecurityUtil.getCurrentUser();
        MeetingTypeResponse created = meetingTypeService.createCustomType(email, request);
        return ResponseEntity.ok(ApiResponse.<MeetingTypeResponse>builder()
                .success(true)
                .message("Custom meeting type created successfully")
                .data(created)
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MeetingTypeResponse>> updateCustomType(
            @PathVariable Long id,
            @Valid @RequestBody CustomMeetingTypeRequest request) {
        String email = SecurityUtil.getCurrentUser();
        MeetingTypeResponse updated = meetingTypeService.updateCustomType(email, id, request);
        return ResponseEntity.ok(ApiResponse.<MeetingTypeResponse>builder()
                .success(true)
                .message("Custom meeting type updated successfully")
                .data(updated)
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomType(@PathVariable Long id) {
        String email = SecurityUtil.getCurrentUser();
        meetingTypeService.deleteCustomType(email, id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Custom meeting type deleted successfully")
                .build());
    }

    /** Lưu trạng thái tích/bỏ tích của nhiều loại trong một lần. */
    @PutMapping("/preferences")
    public ResponseEntity<ApiResponse<List<MeetingTypeResponse>>> updatePreferences(
            @Valid @RequestBody List<MeetingTypePreferenceRequest> preferences) {
        String email = SecurityUtil.getCurrentUser();
        List<MeetingTypeResponse> types = meetingTypeService.updatePreferences(email, preferences);
        return ResponseEntity.ok(ApiResponse.<List<MeetingTypeResponse>>builder()
                .success(true)
                .message("Meeting type preferences updated successfully")
                .data(types)
                .build());
    }
}
