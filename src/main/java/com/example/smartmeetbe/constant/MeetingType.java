package com.example.smartmeetbe.constant;

import java.util.Arrays;

/**
 * 5 loại cuộc họp dựng sẵn của hệ thống. Người dùng có thể bật/tắt từng loại trong
 * Cài đặt tài khoản, nhưng không sửa/xoá được; loại do người dùng tự tạo nằm ở
 * bảng {@code custom_meeting_type} và được nhận diện qua mã dạng chuỗi.
 */
public enum MeetingType {

    SCRUM_SYNC("Họp dự án / Đồng bộ", "Cập nhật tiến độ, rào cản và kế hoạch tiếp theo của nhóm."),
    CLIENT_SALES("Gặp đối tác / Khách hàng", "Nhu cầu, mối quan tâm của khách hàng và các bước tiếp theo."),
    BRAINSTORMING("Lên ý tưởng / Sáng tạo", "Các ý tưởng được nêu ra cùng ưu và nhược điểm của từng phương án."),
    WEBINAR("Hội thảo / Đào tạo", "Nội dung trình bày, câu hỏi của người tham dự và khuyến nghị."),
    GENERAL("Tiêu chuẩn / Cơ bản", "Mẫu chung, phù hợp khi cuộc họp không có đặc thù rõ ràng.");

    private final String label;
    private final String description;

    MeetingType(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    /** Mã có phải là một loại dựng sẵn hay không (ngược lại là loại do người dùng tạo). */
    public static boolean isBuiltIn(String code) {
        return code != null && Arrays.stream(values()).anyMatch(type -> type.name().equals(code));
    }

    /** Trả về enum tương ứng, hoặc null nếu mã là loại do người dùng tự tạo. */
    public static MeetingType fromCode(String code) {
        return isBuiltIn(code) ? valueOf(code) : null;
    }
}
