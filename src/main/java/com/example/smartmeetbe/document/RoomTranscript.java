package com.example.smartmeetbe.document;

import com.example.smartmeetbe.constant.MergeStatus;
import com.example.smartmeetbe.dto.response.TranscriptSegmentDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "room_transcripts")
public class RoomTranscript extends MongoAuditable {

    @Id
    private String id;

    @Indexed(unique = true)
    private String roomId;

    private String fullText;

    private String smoothedText;

    @Builder.Default
    private List<TranscriptSegmentDto> segments = new ArrayList<>();

    @Builder.Default
    private Integer version = 0;

    @Builder.Default
    private Integer processedChunkCount = 0;

    private String lastProcessedChunkId;

    @Builder.Default
    private MergeStatus status = MergeStatus.LIVE;
}
