package com.example.smartmeetbe.service;

import com.example.smartmeetbe.dto.response.DocumentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

public interface DocumentService {

    List<DocumentResponse> uploadDocuments(String roomCode, List<MultipartFile> files, String description, String userEmail);
    List<DocumentResponse> getDocuments(String roomCode);
    InputStream downloadDocument(String roomCode, Long documentId);
    void deleteDocument(String roomCode, Long documentId, String userEmail);
}

