package com.example.smartmeetbe.document;

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
@Document(collection = "meeting_summaries")
public class MeetingSummary extends MongoAuditable {

    @Id
    private String id;

    @Indexed(unique = true)
    private String roomId;

    private String summaryText;

    @Builder.Default
    private List<String> actionItems = new ArrayList<>();

    private String modelVersion;
}
