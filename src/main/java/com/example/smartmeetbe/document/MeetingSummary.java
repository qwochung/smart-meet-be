package com.example.smartmeetbe.document;

import com.example.smartmeetbe.dto.response.MasterMeetingSummaryDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
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

    private String executiveSummary;
    private List<String> discussionTopics;
    private List<String> decisionsMade;
    private List<MasterMeetingSummaryDto.ActionItemDto> actionItems;
    private List<MasterMeetingSummaryDto.QaPairDto> qaPairs;
    private List<String> painPoints;
    private List<MasterMeetingSummaryDto.ProConDto> prosAndCons;
}
