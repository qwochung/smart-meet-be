package com.example.smartmeetbe.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lựa chọn bật/tắt một loại cuộc họp của từng người dùng.
 * Không có bản ghi nghĩa là loại đó đang bật — chỉ lưu khi người dùng bỏ tích
 * hoặc bật lại, nên người dùng mới không cần seed dữ liệu.
 */
@Entity
@Table(
        name = "user_meeting_type_preference",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "type_code"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMeetingTypePreference extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Mã loại cuộc họp: tên enum dựng sẵn hoặc mã của loại tự tạo. */
    @Column(name = "type_code", nullable = false, length = 50)
    private String typeCode;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;
}
