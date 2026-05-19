package com.example.smartmeetbe.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface S3Service {
    String uploadFile(MultipartFile file, String category, String filename);
    String generateDownloadUrl(String s3Key);
    InputStream downloadFile(String s3Key);
    void deleteFile(String s3Key);
}

