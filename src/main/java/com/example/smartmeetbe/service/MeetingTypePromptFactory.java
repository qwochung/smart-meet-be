package com.example.smartmeetbe.service;

import com.example.smartmeetbe.constant.SummaryField;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sinh system prompt cho loại cuộc họp do người dùng tự tạo, dựa trên tên, mô tả và
 * các mục nội dung họ đã tích chọn. Nhờ vậy người dùng không cần biết prompt engineering.
 */
@Component
public class MeetingTypePromptFactory {

    /**
     * @param label       tên loại cuộc họp do người dùng đặt
     * @param description mô tả bối cảnh, có thể để trống
     * @param fields      các mục nội dung cần AI trích xuất
     * @param customInstruction yêu cầu riêng người dùng tự nhập, có thể để trống
     */
    public String build(String label, String description, Set<SummaryField> fields, String customInstruction) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Bạn là một trợ lý biên tập biên bản cuộc họp chuyên nghiệp. ")
                .append("Loại cuộc họp: \"").append(label).append("\".\n");

        if (description != null && !description.isBlank()) {
            prompt.append("Bối cảnh cuộc họp do người dùng mô tả: ").append(description.trim()).append("\n");
        }

        prompt.append("Nhiệm vụ của bạn là phân tích đoạn hội thoại cuộc họp (transcript) thô ")
                .append("và điền vào cấu trúc Master JSON Schema theo các quy tắc sau:\n");

        int index = 1;
        prompt.append(index++).append(". `executiveSummary`: tóm tắt ngắn gọn mục đích và kết quả chính của cuộc họp.\n");

        for (SummaryField field : orderedFields(fields)) {
            prompt.append(index++).append(". ").append(field.getPromptInstruction()).append("\n");
        }

        String skipped = Arrays.stream(SummaryField.values())
                .filter(field -> !fields.contains(field))
                .map(field -> "`" + field.getJsonField() + "`")
                .collect(Collectors.joining(", "));

        if (!skipped.isEmpty()) {
            prompt.append(index++).append(". Các trường còn lại (").append(skipped)
                    .append(") BẮT BUỘC trả về mảng rỗng [] thay vì bỏ qua hay trả về null.\n");
        }

        if (customInstruction != null && !customInstruction.isBlank()) {
            // Đặt cuối cùng để áp lên mọi mục ở trên, nhưng nói rõ không được phá cấu trúc JSON
            prompt.append(index).append(". Yêu cầu bổ sung từ người dùng, áp dụng cho toàn bộ các mục ở trên. ")
                    .append("Tuân thủ yêu cầu này trong phạm vi không làm thay đổi cấu trúc JSON đã quy định: ")
                    .append(customInstruction.trim()).append("\n");
        }

        return prompt.toString().trim();
    }

    /** Giữ thứ tự khai báo của enum để prompt sinh ra ổn định giữa các lần lưu. */
    private List<SummaryField> orderedFields(Set<SummaryField> fields) {
        return Arrays.stream(SummaryField.values())
                .filter(fields::contains)
                .toList();
    }

    /** Chuỗi lưu trong cột {@code extract_fields} -> tập enum, bỏ qua giá trị không hợp lệ. */
    public Set<SummaryField> parseFields(String stored) {
        if (stored == null || stored.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(stored.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> {
                    try {
                        return SummaryField.valueOf(value);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public String formatFields(Set<SummaryField> fields) {
        return orderedFields(fields).stream()
                .map(SummaryField::name)
                .collect(Collectors.joining(","));
    }
}
