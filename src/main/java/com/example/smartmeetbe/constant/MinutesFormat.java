package com.example.smartmeetbe.constant;

/**
 * Mẫu biên bản (minutes format) - trục lựa chọn thứ hai bên cạnh {@link MeetingType}.
 *
 * <p>{@code MeetingType} quyết định NỘI DUNG nào cần trích xuất từ transcript,
 * còn {@code MinutesFormat} quyết định MỨC ĐỘ CHI TIẾT của biên bản đầu ra.
 * Hai chiều này độc lập nhau nên prompt gửi cho Gemini được ghép từ hai lớp:
 * lớp chủ đề (prompt_template theo typeCode) + lớp chi tiết (minutes_format_template theo formatCode).
 */
public enum MinutesFormat {

    /** Chỉ kết luận và đầu việc, bỏ diễn biến thảo luận. Mặc định toàn hệ thống. */
    ACTION("""
            MẪU BIÊN BẢN: ACTION MINUTES (biên bản hành động).
            Các yêu cầu về mức độ chi tiết dưới đây được áp dụng CÙNG LÚC với hướng dẫn theo loại cuộc họp ở trên:
            1. Chỉ ghi lại KẾT LUẬN và ĐẦU VIỆC. Không thuật lại diễn biến thảo luận, lập luận hay ý kiến trái chiều.
            2. `executiveSummary`: tối đa 2 câu, chỉ nêu mục đích cuộc họp và kết quả đã chốt.
            3. `decisionsMade` và `actionItems`: điền đầy đủ; mỗi mục là một câu ngắn gọn ở thể khẳng định.
            4. `discussionTopics`: chỉ liệt kê TÊN chủ đề đã bàn (cụm danh từ ngắn), không diễn giải nội dung.
            5. Các trường phân tích sâu (`painPoints`, `prosAndCons`, `qaPairs`): trả về mảng rỗng [],
               TRỪ KHI hướng dẫn theo loại cuộc họp ở trên yêu cầu bắt buộc phải điền trường đó."""),

    /** Tóm tắt diễn biến thảo luận, quan điểm các bên và lý do dẫn tới quyết định. */
    DISCUSSION("""
            MẪU BIÊN BẢN: DISCUSSION MINUTES (biên bản thảo luận).
            Các yêu cầu về mức độ chi tiết dưới đây được áp dụng CÙNG LÚC với hướng dẫn theo loại cuộc họp ở trên:
            1. Ghi lại cả QUÁ TRÌNH dẫn tới kết luận, không chỉ kết luận cuối cùng.
            2. `executiveSummary`: 3-5 câu, nêu bối cảnh, các luồng ý kiến chính và kết quả.
            3. `discussionTopics`: mỗi mục là một đoạn tóm tắt hoàn chỉnh gồm chủ đề, các quan điểm được nêu
               (ghi rõ tên người phát biểu nếu xác định được) và hướng xử lý.
            4. `decisionsMade`: mỗi quyết định phải kèm LÝ DO ngắn gọn theo dạng "Quyết định ... vì ...".
            5. Ghi nhận cả ý kiến trái chiều hoặc phương án đã bị loại, kèm lý do bị loại.
            6. Vẫn giữ nguyên các quy tắc về `actionItems` và định dạng deadline ở phần hướng dẫn phía trên."""),

    /** Gần như nguyên văn. Không đi qua bước tóm tắt AI, dùng transcript đã làm mượt. */
    VERBATIM("""
            MẪU BIÊN BẢN: VERBATIM MINUTES (biên bản gần như nguyên văn).
            Các yêu cầu về mức độ chi tiết dưới đây được áp dụng CÙNG LÚC với hướng dẫn theo loại cuộc họp ở trên:
            1. Giữ mức chi tiết tối đa, bám sát câu chữ gốc, không rút gọn hay diễn giải lại ý người nói.
            2. `discussionTopics`: thuật lại tuần tự diễn biến theo đúng thứ tự phát biểu, trích dẫn nguyên văn
               các câu phát biểu quan trọng và ghi rõ tên người nói theo dạng "[Tên]: \\"...\\"".
            3. Không lược bỏ nội dung chỉ vì cho rằng nó không quan trọng.
            4. Vẫn giữ nguyên các quy tắc về `actionItems` và định dạng deadline ở phần hướng dẫn phía trên.""");

    private final String defaultDetailPrompt;

    MinutesFormat(String defaultDetailPrompt) {
        this.defaultDetailPrompt = defaultDetailPrompt;
    }

    /**
     * Đoạn chỉ dẫn "mức độ chi tiết" dựng sẵn trong mã nguồn, dùng khi bảng
     * {@code minutes_format_template} chưa được seed hoặc bản ghi bị vô hiệu hoá.
     */
    public String getDefaultDetailPrompt() {
        return defaultDetailPrompt;
    }

    /**
     * Mẫu biên bản gợi ý mặc định theo từng loại cuộc họp dựng sẵn, dùng khi request không
     * chỉ định {@code minutesFormat}. Loại do người dùng tự tạo có gợi ý riêng lưu trong
     * {@code custom_meeting_type.default_minutes_format}, nên ở đây rơi về {@link #ACTION}.
     */
    public static MinutesFormat defaultFor(String typeCode) {
        return defaultFor(MeetingType.fromCode(typeCode));
    }

    public static MinutesFormat defaultFor(MeetingType typeCode) {
        if (typeCode == null) {
            return ACTION;
        }
        return switch (typeCode) {
            // Cần lưu lại lý lẽ / quan điểm các bên, không chỉ kết luận
            case CLIENT_SALES, BRAINSTORMING -> DISCUSSION;
            // SCRUM_SYNC, WEBINAR, GENERAL: họp vận hành / một chiều, chỉ cần kết luận và đầu việc
            default -> ACTION;
        };
    }
}
