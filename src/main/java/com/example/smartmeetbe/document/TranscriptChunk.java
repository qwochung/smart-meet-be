package com.example.smartmeetbe.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "transcript_chunks")
@CompoundIndexes({
        @CompoundIndex(name = "room_participant_chunk", def = "{'roomId': 1, 'participantId': 1, 'chunkIndex': 1}"),
        @CompoundIndex(name = "room_start_time", def = "{'roomId': 1, 'startTimeMs': 1}")
})
public class TranscriptChunk extends MongoAuditable {

    @Id
    private String id;

    private String roomId;

    private String participantId;

    private String participantName;

    private Long startTimeMs;

    private Long endTimeMs;

    private String content;

    private Integer chunkIndex;

    @Builder.Default
    private Boolean isForceCut = false;
}
