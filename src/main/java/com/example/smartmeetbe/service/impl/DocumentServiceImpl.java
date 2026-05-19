package com.example.smartmeetbe.service.impl;

import com.example.smartmeetbe.constant.ErrorCode;
import com.example.smartmeetbe.dto.response.DocumentResponse;
import com.example.smartmeetbe.entity.Document;
import com.example.smartmeetbe.entity.JoinRoom;
import com.example.smartmeetbe.entity.Room;
import com.example.smartmeetbe.exception.AppException;
import com.example.smartmeetbe.repository.DocumentRepository;
import com.example.smartmeetbe.repository.JoinRoomRepository;
import com.example.smartmeetbe.repository.RoomRepository;
import com.example.smartmeetbe.service.DocumentService;
import com.example.smartmeetbe.service.S3Service;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {
    
    final RoomRepository roomRepository;
    final DocumentRepository documentRepository;
    final JoinRoomRepository joinRoomRepository;
    final S3Service s3Service;
    
    @Value("${app.storage.s3.bucket-name}")
    String bucketName;
    
    @Value("${app.storage.s3.region}")
    String region;
    
    private static final int MAX_FILES_PER_UPLOAD = 5;
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024; // 50 MB
    private static final Set<String> ALLOWED_FILE_TYPES = Set.of("pdf", "doc", "docx", "txt", "ppt", "pptx");
    
    @Override
    @Transactional
    public List<DocumentResponse> uploadDocuments(String roomCode, List<MultipartFile> files, String description, String userEmail) {
        // 1. Validate room exists
        Room room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        
        // 2. Validate user is participant in room
        isParticipantOfRoom(room.getId(), userEmail);
        
        // 3. Validate file count
        if (files == null || files.isEmpty()) {
            throw new RuntimeException("No files provided");
        }
        if (files.size() > MAX_FILES_PER_UPLOAD) {
            throw new RuntimeException("Maximum " + MAX_FILES_PER_UPLOAD + " files allowed per upload");
        }
        
        // 4. Check total documents count
        long currentDocCount = documentRepository.countByRoomId(room.getId());
        if (currentDocCount + files.size() > 100) { // reasonable limit
            throw new RuntimeException("Document limit exceeded for this room");
        }
        
        List<DocumentResponse> uploadedDocuments = new ArrayList<>();
        
        // 5. Upload each file
        for (MultipartFile file : files) {
            DocumentResponse doc = uploadSingleFile(room, file, description, userEmail);
            uploadedDocuments.add(doc);
        }
        
        log.info("Uploaded {} documents to room: {} by user: {}", uploadedDocuments.size(), roomCode, userEmail);
        return uploadedDocuments;
    }
    
    @Transactional
    public DocumentResponse uploadSingleFile(Room room, MultipartFile file, String description, String userEmail) {
        // Validate file
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }
        
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isEmpty()) {
            throw new RuntimeException("Invalid filename");
        }
        
        // Get file type
        String fileType = getFileType(originalFileName);
        validateFileType(fileType);
        
        // Validate file size
        long fileSize = file.getSize();
        if (fileSize > MAX_FILE_SIZE) {
            throw new RuntimeException("File size exceeds limit (max 50MB)");
        }
        
        // Generate unique filename
        String storedFileName = generateUniqueFileName(originalFileName);
        
        // Determine category
        String category = "file"; // default for documents
        
        // Upload to S3
        String s3Key = s3Service.uploadFile(file, category, storedFileName);
        
        // Save to database - store only the relative path
        Document document = Document.builder()
                .room(room)
                .roomId(room.getId())
                .originalFileName(originalFileName)
                .storedFileName(storedFileName)
                .filePath(s3Key)
                .fileType(fileType)
                .fileSize(fileSize)
                .category(category)
                .uploadedBy(userEmail)
                .description(description)
                .build();
        
        Document savedDoc = documentRepository.save(document);
        
        log.info("Document uploaded: {} with S3 key: {}", originalFileName, s3Key);
        
        return mapDocumentToResponse(savedDoc);
    }
    
    @Override
    public List<DocumentResponse> getDocuments(String roomCode) {
        // Validate room exists
        Room room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        
        return documentRepository.findByRoomId(room.getId())
                .stream()
                .map(this::mapDocumentToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public InputStream downloadDocument(String roomCode, Long documentId) {
        // Validate room exists
        Room room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        
        // Validate document exists in room
        Document document = documentRepository.findByIdAndRoomId(documentId, room.getId())
                .orElseThrow(() -> new RuntimeException("Document not found in this room"));
        
        // Download from S3 using the stored file path
        return s3Service.downloadFile(document.getFilePath());
    }
    
    @Override
    @Transactional
    public void deleteDocument(String roomCode, Long documentId, String userEmail) {
        // 1. Validate room exists
        Room room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        
        // 2. Validate document exists in room
        Document document = documentRepository.findByIdAndRoomId(documentId, room.getId())
                .orElseThrow(() -> new RuntimeException("Document not found in this room"));
        
        // 3. Check if user is the uploader
        if (!document.getUploadedBy().equals(userEmail)) {
            throw new RuntimeException("Only the document uploader can delete this file");
        }
        
        // 4. Delete from S3 using the stored file path
        s3Service.deleteFile(document.getFilePath());
        
        // 5. Delete from database
        documentRepository.delete(document);
        
        log.info("Document deleted: {} from room: {} by user: {}", document.getOriginalFileName(), roomCode, userEmail);
    }
    
    private void isParticipantOfRoom(Long roomId, String userEmail) {
        Optional<JoinRoom> participantJoinRoom = joinRoomRepository.findByRoomIdAndUser_Email(roomId, userEmail);
        if (participantJoinRoom.isEmpty()) {
            throw new RuntimeException("User is not a participant of this room");
        }
    }
    
    private String getFileType(String filename) {
        int lastDot = filename.lastIndexOf(".");
        if (lastDot > 0) {
            return filename.substring(lastDot + 1).toLowerCase();
        }
        throw new RuntimeException("Invalid file: no extension");
    }
    
    private void validateFileType(String fileType) {
        if (!ALLOWED_FILE_TYPES.contains(fileType)) {
            throw new RuntimeException("File type not allowed. Allowed types: " + ALLOWED_FILE_TYPES);
        }
    }
    
    private String generateUniqueFileName(String originalFileName) {
        String fileType = getFileType(originalFileName);
        String fileName = originalFileName.substring(0, originalFileName.lastIndexOf("."));
        return fileName + "_" + System.currentTimeMillis() + "." + fileType;
    }
    
    private DocumentResponse mapDocumentToResponse(Document doc) {
        // Generate full URL from stored file path
        String fileUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, doc.getFilePath());
        
        return DocumentResponse.builder()
                .id(doc.getId())
                .originalFileName(doc.getOriginalFileName())
                .fileType(doc.getFileType())
                .fileSize(doc.getFileSize())
                .category(doc.getCategory())
                .uploadedBy(doc.getUploadedBy())
                .description(doc.getDescription())
                .fileUrl(fileUrl)
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
