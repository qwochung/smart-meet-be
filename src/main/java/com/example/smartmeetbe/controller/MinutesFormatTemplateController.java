package com.example.smartmeetbe.controller;

import com.example.smartmeetbe.constant.MinutesFormat;
import com.example.smartmeetbe.dto.response.ApiResponse;
import com.example.smartmeetbe.entity.MinutesFormatTemplate;
import com.example.smartmeetbe.service.MinutesFormatTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * CRUD cho lớp prompt "mức độ chi tiết" (mẫu biên bản), song song với {@link PromptTemplateController}.
 */
@RestController
@RequestMapping("/minutes-formats")
@RequiredArgsConstructor
public class MinutesFormatTemplateController {

    private final MinutesFormatTemplateService templateService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MinutesFormatTemplate>>> getAllTemplates() {
        List<MinutesFormatTemplate> templates = templateService.getAllTemplates();
        return ResponseEntity.ok(ApiResponse.<List<MinutesFormatTemplate>>builder()
                .success(true)
                .message("All minutes format templates retrieved successfully")
                .data(templates)
                .build());
    }

    @GetMapping("/active/{formatCode}")
    public ResponseEntity<ApiResponse<MinutesFormatTemplate>> getTemplateByFormatCode(@PathVariable MinutesFormat formatCode) {
        MinutesFormatTemplate template = templateService.getTemplateByFormatCode(formatCode);
        return ResponseEntity.ok(ApiResponse.<MinutesFormatTemplate>builder()
                .success(true)
                .message("Active minutes format template retrieved successfully")
                .data(template)
                .build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MinutesFormatTemplate>> createTemplate(@RequestBody MinutesFormatTemplate template) {
        MinutesFormatTemplate created = templateService.createTemplate(template);
        return ResponseEntity.ok(ApiResponse.<MinutesFormatTemplate>builder()
                .success(true)
                .message("Minutes format template created successfully")
                .data(created)
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MinutesFormatTemplate>> updateTemplate(
            @PathVariable Long id,
            @RequestBody MinutesFormatTemplate details) {
        MinutesFormatTemplate updated = templateService.updateTemplate(id, details);
        return ResponseEntity.ok(ApiResponse.<MinutesFormatTemplate>builder()
                .success(true)
                .message("Minutes format template updated successfully")
                .data(updated)
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Minutes format template deleted successfully")
                .build());
    }
}
