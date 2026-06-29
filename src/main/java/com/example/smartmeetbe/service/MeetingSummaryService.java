package com.example.smartmeetbe.service;

import com.example.smartmeetbe.dto.response.MasterMeetingSummaryDto;
import com.example.smartmeetbe.dto.response.MeetingSummaryDetailResponse;

public interface MeetingSummaryService {

    /** Lấy biên bản đầy đủ (tóm tắt AI + metadata phòng họp) để hiển thị/chỉnh sửa. */
    MeetingSummaryDetailResponse getSummaryDetail(String roomId);

    /** Cập nhật nội dung tóm tắt biên bản đã được người dùng chỉnh sửa. */
    MasterMeetingSummaryDto updateSummary(String roomId, MasterMeetingSummaryDto edited);
}
