package com.example.smartmeetbe.constant;

/**
 * Các mục nội dung mà AI có thể trích xuất từ transcript. Khi người dùng tự tạo một loại
 * cuộc họp, họ tích chọn các mục ở đây và hệ thống tự sinh system prompt tương ứng
 * (xem {@code MeetingTypePromptFactory}).
 *
 * <p>{@code executiveSummary} không nằm trong danh sách vì luôn được sinh cho mọi loại.
 */
public enum SummaryField {

    DISCUSSION_TOPICS("discussionTopics", "Chủ đề thảo luận",
            "`discussionTopics`: liệt kê các chủ đề đã được bàn tới trong cuộc họp."),

    DECISIONS_MADE("decisionsMade", "Quyết định",
            "`decisionsMade`: liệt kê các quyết định, kết luận đã được thống nhất."),

    ACTION_ITEMS("actionItems", "Đầu việc / phân công",
            """
            `actionItems`: trích xuất các đầu việc phát sinh cùng người phụ trách.
               BẮT BUỘC định dạng trường `deadline` theo kiểu YYYY-MM-DD. Nếu không nhắc tới hạn cụ thể,
               bắt buộc trả về chuỗi rỗng "". Không được tự bịa ngày hoặc dùng từ tương đối."""),

    QA_PAIRS("qaPairs", "Hỏi & Đáp",
            "`qaPairs`: trích xuất các cặp câu hỏi - câu trả lời đã xuất hiện trong cuộc họp."),

    PAIN_POINTS("painPoints", "Khó khăn / rào cản",
            "`painPoints`: trích xuất các khó khăn, rào cản, vấn đề còn tồn đọng được nêu ra."),

    PROS_AND_CONS("prosAndCons", "Ưu / nhược điểm",
            "`prosAndCons`: với mỗi ý tưởng hoặc phương án được nêu, tóm tắt ưu điểm và nhược điểm.");

    private final String jsonField;
    private final String label;
    private final String promptInstruction;

    SummaryField(String jsonField, String label, String promptInstruction) {
        this.jsonField = jsonField;
        this.label = label;
        this.promptInstruction = promptInstruction;
    }

    public String getJsonField() {
        return jsonField;
    }

    public String getLabel() {
        return label;
    }

    public String getPromptInstruction() {
        return promptInstruction;
    }
}
