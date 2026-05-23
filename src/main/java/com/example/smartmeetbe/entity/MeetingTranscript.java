package com.example.smartmeetbe.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "meeting_transcripts")
@EqualsAndHashCode(callSuper = true)
public class MeetingTranscript extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String roomId;

    @Column(nullable = false)
    private String participantId;

    private String participantName;

    private Long startTimeMs;

    private Long endTimeMs;

    @Column(columnDefinition = "TEXT")
    private String content;

    private Integer chunkIndex;
}
