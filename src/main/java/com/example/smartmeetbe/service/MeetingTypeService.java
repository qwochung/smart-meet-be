package com.example.smartmeetbe.service;

import com.example.smartmeetbe.constant.MinutesFormat;
import com.example.smartmeetbe.dto.request.CustomMeetingTypeRequest;
import com.example.smartmeetbe.dto.request.MeetingTypePreferenceRequest;
import com.example.smartmeetbe.dto.response.MeetingTypeResponse;
import com.example.smartmeetbe.entity.CustomMeetingType;

import java.util.List;
import java.util.Optional;

public interface MeetingTypeService {

    /** Toàn bộ loại khả dụng (dựng sẵn + tự tạo) của người dùng, kèm trạng thái bật/tắt. */
    List<MeetingTypeResponse> getMeetingTypes(String email);

    /** Chỉ các loại đang bật, dùng cho dropdown tạo cuộc họp. */
    List<MeetingTypeResponse> getEnabledMeetingTypes(String email);

    MeetingTypeResponse createCustomType(String email, CustomMeetingTypeRequest request);

    MeetingTypeResponse updateCustomType(String email, Long id, CustomMeetingTypeRequest request);

    void deleteCustomType(String email, Long id);

    List<MeetingTypeResponse> updatePreferences(String email, List<MeetingTypePreferenceRequest> preferences);

    /**
     * Kiểm tra mã loại người dùng gửi lên có hợp lệ với họ không.
     * Mã rỗng, không tồn tại hoặc thuộc người khác đều rơi về {@code GENERAL}.
     */
    String resolveTypeCodeForUser(String email, String requestedCode);

    /** Mẫu biên bản gợi ý cho một mã loại bất kỳ (dựng sẵn hoặc tự tạo). */
    MinutesFormat resolveDefaultMinutesFormat(String typeCode);

    Optional<CustomMeetingType> findCustomTypeByCode(String code);
}
