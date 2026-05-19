package com.example.smartmeetbe.repository;

import com.example.smartmeetbe.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByRoomId(Long roomId);
    
    Optional<Document> findByIdAndRoomId(Long documentId, Long roomId);
    
    long countByRoomId(Long roomId);
    
    List<Document> findByRoomIdAndCategory(Long roomId, String category);
}

