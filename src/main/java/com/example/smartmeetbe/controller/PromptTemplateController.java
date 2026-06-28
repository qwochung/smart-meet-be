package com.example.smartmeetbe.controller;

import com.example.smartmeetbe.constant.MeetingType;
import com.example.smartmeetbe.dto.response.ApiResponse;
import com.example.smartmeetbe.entity.PromptTemplate;
import com.example.smartmeetbe.service.PromptTemplateService;
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

@RestController
@RequestMapping("/prompts")
@RequiredArgsConstructor
public class PromptTemplateController {

    private final PromptTemplateService promptService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PromptTemplate>>> getAllPrompts() {
        List<PromptTemplate> prompts = promptService.getAllPrompts();
        return ResponseEntity.ok(ApiResponse.<List<PromptTemplate>>builder()
                .success(true)
                .message("All prompts retrieved successfully")
                .data(prompts)
                .build());
    }

    @GetMapping("/active/{typeCode}")
    public ResponseEntity<ApiResponse<PromptTemplate>> getPromptByTypeCode(@PathVariable MeetingType typeCode) {
        PromptTemplate prompt = promptService.getPromptByTypeCode(typeCode);
        return ResponseEntity.ok(ApiResponse.<PromptTemplate>builder()
                .success(true)
                .message("Active prompt retrieved successfully")
                .data(prompt)
                .build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PromptTemplate>> createPrompt(@RequestBody PromptTemplate prompt) {
        PromptTemplate created = promptService.createPrompt(prompt);
        return ResponseEntity.ok(ApiResponse.<PromptTemplate>builder()
                .success(true)
                .message("Prompt created successfully")
                .data(created)
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PromptTemplate>> updatePrompt(
            @PathVariable Long id, 
            @RequestBody PromptTemplate promptDetails) {
        PromptTemplate updated = promptService.updatePrompt(id, promptDetails);
        return ResponseEntity.ok(ApiResponse.<PromptTemplate>builder()
                .success(true)
                .message("Prompt updated successfully")
                .data(updated)
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePrompt(@PathVariable Long id) {
        promptService.deletePrompt(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Prompt deleted successfully")
                .build());
    }
}
