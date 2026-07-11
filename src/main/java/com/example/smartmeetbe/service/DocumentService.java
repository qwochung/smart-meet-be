package com.example.smartmeetbe.service;

import com.example.smartmeetbe.dto.response.DocumentDownload;
import com.example.smartmeetbe.dto.response.DocumentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {

    List<DocumentResponse> uploadDocuments(String roomCode, List<MultipartFile> files, String description, String userEmail);
    List<DocumentResponse> getDocuments(String roomCode);
    DocumentDownload downloadDocument(String roomCode, Long documentId);
    void deleteDocument(String roomCode, Long documentId, String userEmail);
    DocumentResponse summarizeDocument(String roomCode, Long documentId, String userEmail, boolean force);
}

