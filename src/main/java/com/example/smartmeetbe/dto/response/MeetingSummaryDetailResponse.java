package com.example.smartmeetbe.dto.response;

import com.example.smartmeetbe.constant.MinutesFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Dữ liệu biên bản đầy đủ trả về cho FE để hiển thị / chỉnh sửa trước khi xuất file.
 * Gồm phần tóm tắt AI (có thể sửa) và metadata phòng họp (chỉ đọc).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingSummaryDetailResponse {

    // Metadata phòng họp (chỉ đọc)
    private String roomId;
    private String meetingName;
    private String meetingDate;
    private String hostName;
    private List<String> attendees;
    private int attendeeCount;
    private Long durationMinutes; // null nếu chưa theo dõi được thời lượng

    // Mẫu biên bản đã áp dụng; FE dùng để chọn cách hiển thị
    private MinutesFormat minutesFormat;

    // Chỉ có giá trị với mẫu VERBATIM: bản ghi gần như nguyên văn (chỉ đọc)
    private String verbatimText;

    // Nội dung tóm tắt AI (có thể chỉnh sửa)
    private MasterMeetingSummaryDto summary;
}
