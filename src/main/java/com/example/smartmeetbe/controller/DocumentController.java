package com.example.smartmeetbe.controller;

import com.example.smartmeetbe.dto.response.ApiResponse;
import com.example.smartmeetbe.dto.response.DocumentResponse;
import com.example.smartmeetbe.service.DocumentService;
import com.example.smartmeetbe.utils.SecurityUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/rooms/{code}/documents")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class DocumentController {
    
    DocumentService documentService;

    @PostMapping
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> uploadDocuments(
            @PathVariable String code,
            @RequestParam(value = "files") List<MultipartFile> files,
            @RequestParam(value = "description", required = false) String description) {
        
        String userEmail = SecurityUtil.getCurrentUser();
        List<DocumentResponse> documents = documentService.uploadDocuments(code, files, description, userEmail);
        
        return ResponseEntity.ok(ApiResponse.<List<DocumentResponse>>builder()
                .success(true)
                .message("Documents uploaded successfully")
                .data(documents)
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getDocuments(@PathVariable String code) {
        List<DocumentResponse> documents = documentService.getDocuments(code);
        
        return ResponseEntity.ok(ApiResponse.<List<DocumentResponse>>builder()
                .success(true)
                .message("Documents retrieved successfully")
                .data(documents)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> downloadDocument(
            @PathVariable String code,
            @PathVariable Long id) {
        
        try {
            InputStream fileStream = documentService.downloadDocument(code, id);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"document\"")
                    .body(fileStream);
                    
        } catch (Exception e) {
            log.error("Error downloading document: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Failed to download document: " + e.getMessage())
                            .build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable String code,
            @PathVariable Long id) {
        
        String userEmail = SecurityUtil.getCurrentUser();
        documentService.deleteDocument(code, id, userEmail);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Document deleted successfully")
                .build());
    }
}



