package com.example.smartmeetbe.strategy;

import com.example.smartmeetbe.constant.MinutesFormat;
import com.example.smartmeetbe.dto.response.MasterMeetingSummaryDto;

public interface MeetingSummaryStrategy {

    /**
     * Mã loại cuộc họp mà strategy này phụ trách. Các strategy dựng sẵn trả về tên enum
     * {@code MeetingType}; strategy dùng cho loại người dùng tự tạo trả về
     * {@link #CUSTOM_TYPE_CODE} và được dùng như nhánh dự phòng.
     */
    String getTypeCode();

    /** Mã đánh dấu strategy xử lý mọi loại cuộc họp do người dùng tự tạo. */
    String CUSTOM_TYPE_CODE = "__CUSTOM__";

    /**
     * @param typeCode      mã loại cuộc họp của phòng; cần cho strategy tùy chỉnh vì một
     *                      strategy phục vụ nhiều mã khác nhau
     * @param minutesFormat mức độ chi tiết; null sẽ rơi về mặc định theo loại cuộc họp
     */
    MasterMeetingSummaryDto generateSummary(String roomId, String typeCode,
                                            String fullRawTranscript, MinutesFormat minutesFormat);
}
