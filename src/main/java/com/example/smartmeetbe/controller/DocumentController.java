package com.example.smartmeetbe.controller;

import com.example.smartmeetbe.dto.response.ApiResponse;
import com.example.smartmeetbe.dto.response.DocumentDownload;
import com.example.smartmeetbe.dto.response.DocumentResponse;
import org.springframework.core.io.InputStreamResource;
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    public ResponseEntity<InputStreamResource> downloadDocument(
            @PathVariable String code,
            @PathVariable Long id) {

        DocumentDownload download = documentService.downloadDocument(code, id);

        // Sanitize tên file cho header, giữ bản UTF-8 qua filename*
        String rawName = download.fileName() != null ? download.fileName() : "document";
        String asciiName = rawName.replaceAll("[\\\\/\"\\r\\n]", "_");
        String encodedName = URLEncoder.encode(rawName, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + asciiName + "\"; filename*=UTF-8''" + encodedName)
                .body(new InputStreamResource(download.stream()));
    }

    @PostMapping("/{id}/summary")
    public ResponseEntity<ApiResponse<DocumentResponse>> summarizeDocument(
            @PathVariable String code,
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean force) {

        String userEmail = SecurityUtil.getCurrentUser();
        DocumentResponse document = documentService.summarizeDocument(code, id, userEmail, force);

        return ResponseEntity.ok(ApiResponse.<DocumentResponse>builder()
                .success(true)
                .message("Document summarized successfully")
                .data(document)
                .build());
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



