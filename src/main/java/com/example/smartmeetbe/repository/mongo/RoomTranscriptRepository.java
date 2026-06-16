package com.example.smartmeetbe.repository.mongo;

import com.example.smartmeetbe.document.RoomTranscript;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomTranscriptRepository extends MongoRepository<RoomTranscript, String> {

    Optional<RoomTranscript> findByRoomId(String roomId);
}
