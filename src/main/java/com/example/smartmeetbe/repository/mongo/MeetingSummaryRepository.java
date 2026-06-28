package com.example.smartmeetbe.repository.mongo;

import com.example.smartmeetbe.document.MeetingSummary;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MeetingSummaryRepository extends MongoRepository<MeetingSummary, String> {
    Optional<MeetingSummary> findByRoomId(String roomId);
}
