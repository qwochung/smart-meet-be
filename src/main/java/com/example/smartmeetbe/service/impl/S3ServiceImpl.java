package com.example.smartmeetbe.service.impl;

import com.example.smartmeetbe.service.S3Service;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class S3ServiceImpl implements S3Service {
    
    final S3Client s3Client;
    
    @Value("${app.storage.s3.bucket-name}")
    String bucketName;
    
    @Value("${app.storage.s3.region}")
    String region;
    
    @Override
    public String uploadFile(MultipartFile file, String category, String filename) {
        try {
            String s3Key = String.format("%s/%s", category, filename);
            
            byte[] fileContent = file.getBytes();
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .contentLength((long) fileContent.length)
                    .build();
            
            s3Client.putObject(putObjectRequest,
                    software.amazon.awssdk.core.sync.RequestBody.fromBytes(fileContent));
            
            log.info("File uploaded to S3: s3://{}/{}", bucketName, s3Key);
            return s3Key;
            
        } catch (IOException e) {
            log.error("Error uploading file to S3: {}", e.getMessage());
            throw new RuntimeException("Failed to upload file to S3", e);
        } catch (S3Exception e) {
            log.error("S3 error during upload: {}", e.getMessage());
            throw new RuntimeException("S3 error during upload", e);
        }
    }
    
    @Override
    public String generateDownloadUrl(String s3Key) {
        String shortUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, s3Key);
        log.info("Generated public file URL for: {}", s3Key);
        return shortUrl;
    }
    
    @Override
    public InputStream downloadFile(String s3Key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            
            InputStream response = s3Client.getObject(getObjectRequest);
            
            log.info("File downloaded from S3: {}", s3Key);
            return response;
            
        } catch (S3Exception e) {
            log.error("S3 error downloading file: {}", e.getMessage());
            throw new RuntimeException("S3 error downloading file", e);
        }
    }
    
    @Override
    public void deleteFile(String s3Key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            
            s3Client.deleteObject(deleteObjectRequest);
            
            log.info("File deleted from S3: {}", s3Key);
            
        } catch (S3Exception e) {
            log.error("S3 error deleting file: {}", e.getMessage());
            throw new RuntimeException("S3 error deleting file", e);
        }
    }
}
