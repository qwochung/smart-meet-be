package com.example.smartmeetbe.entity;

import com.example.smartmeetbe.constant.MinutesFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Loại cuộc họp do người dùng tự tạo trong Cài đặt tài khoản.
 * 6 loại dựng sẵn không nằm ở bảng này mà lấy từ enum {@code MeetingType};
 * việc bật/tắt hiển thị của cả hai nhóm nằm ở {@link UserMeetingTypePreference}.
 */
@Entity
@Table(name = "custom_meeting_type")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomMeetingType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mã lưu vào {@code rooms.type_code}, sinh tự động, duy nhất toàn hệ thống. */
    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "label", nullable = false, length = 120)
    private String label;

    @Column(name = "description", length = 500)
    private String description;

    /** System prompt sinh tự động từ label, description và danh sách mục đã tích. */
    @Column(name = "system_prompt", columnDefinition = "TEXT", nullable = false)
    private String systemPrompt;

    /** Danh sách {@code SummaryField} đã tích, lưu dạng chuỗi ngăn cách bởi dấu phẩy. */
    @Column(name = "extract_fields", nullable = false, length = 500)
    private String extractFields;

    /** Yêu cầu riêng người dùng tự nhập, được ghép vào cuối system prompt. Có thể null. */
    @Column(name = "custom_instruction", columnDefinition = "TEXT")
    private String customInstruction;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_minutes_format", length = 50)
    private MinutesFormat defaultMinutesFormat;
}
