package com.example.smartmeetbe.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "documents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Document extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    Room room;

    @Column(name = "room_id", insertable = false, updatable = false)
    Long roomId;

    @Column(nullable = false)
    String originalFileName;

    @Column(nullable = false)
    String storedFileName;

    @Column(name = "file_path", nullable = false)
    String filePath; // relative path: file/name_timestamp.ext

    @Column(nullable = false)
    String fileType; // pdf, doc, txt, ppt

    @Column(nullable = false)
    Long fileSize; // bytes

    @Column(nullable = false, length = 10)
    String category; // file, image, audio

    @Column(nullable = false)
    String uploadedBy; // user email who uploaded

    @Column(length = 500)
    String description;
}
