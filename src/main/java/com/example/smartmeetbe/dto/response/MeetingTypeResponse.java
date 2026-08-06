package com.example.smartmeetbe.dto.response;

import com.example.smartmeetbe.constant.MinutesFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Một loại cuộc họp khả dụng với người dùng hiện tại, dùng cho cả màn Cài đặt tài khoản
 * lẫn dropdown ở màn tạo cuộc họp.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingTypeResponse {

    /** null với loại dựng sẵn; có giá trị với loại do người dùng tạo (dùng để sửa/xoá). */
    private Long id;

    private String code;
    private String label;
    private String description;

    /** true = loại dựng sẵn, chỉ bật/tắt được, không sửa/xoá được. */
    private boolean builtIn;

    /** Có hiển thị trong dropdown tạo cuộc họp hay không. */
    private boolean enabled;

    /** Tên các {@code SummaryField} mà loại này trích xuất; rỗng với loại dựng sẵn. */
    private List<String> extractFields;

    /** Yêu cầu riêng người dùng đã nhập; null với loại dựng sẵn. */
    private String customInstruction;

    private MinutesFormat defaultMinutesFormat;
}
