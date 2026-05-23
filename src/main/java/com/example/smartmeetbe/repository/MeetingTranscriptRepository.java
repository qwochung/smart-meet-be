package com.example.smartmeetbe.repository;

import com.example.smartmeetbe.entity.MeetingTranscript;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MeetingTranscriptRepository extends JpaRepository<MeetingTranscript, Long> {

    List<MeetingTranscript> findByRoomIdOrderByChunkIndexAsc(String roomId);

    List<MeetingTranscript> findByRoomIdAndParticipantIdOrderByChunkIndexAsc(String roomId, String participantId);
}
