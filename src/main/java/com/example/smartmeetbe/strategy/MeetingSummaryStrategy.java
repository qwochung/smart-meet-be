package com.example.smartmeetbe.strategy;

import com.example.smartmeetbe.constant.MeetingType;
import com.example.smartmeetbe.dto.response.MasterMeetingSummaryDto;

public interface MeetingSummaryStrategy {
    MeetingType getTypeCode();
    MasterMeetingSummaryDto generateSummary(String roomId, String fullRawTranscript);
}
