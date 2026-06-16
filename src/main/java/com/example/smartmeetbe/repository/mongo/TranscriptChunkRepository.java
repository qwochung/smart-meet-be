package com.example.smartmeetbe.repository.mongo;

import com.example.smartmeetbe.document.TranscriptChunk;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TranscriptChunkRepository extends MongoRepository<TranscriptChunk, String> {

    List<TranscriptChunk> findByRoomIdOrderByStartTimeMsAscCreatedAtAsc(String roomId);

    List<TranscriptChunk> findByRoomIdAndIdGreaterThanOrderByStartTimeMsAscCreatedAtAsc(String roomId, String id);
}
