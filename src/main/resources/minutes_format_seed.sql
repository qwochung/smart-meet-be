-- ==========================================
-- SCRIPT KHỞI TẠO BẢNG VÀ SEED DỮ LIỆU MINUTES FORMAT TEMPLATE
-- Dự án: Smart Meet - AI Meeting Summary
--
-- Lớp prompt thứ hai (mức độ chi tiết), ghép với prompt_template (chủ đề cần trích xuất).
--
-- LƯU Ý: Script này KHÔNG bắt buộc phải chạy để hệ thống hoạt động.
--  - Bảng minutes_format_template được Hibernate tự tạo (spring.jpa.hibernate.ddl-auto=update).
--  - Nếu bảng trống, MinutesFormatTemplateServiceImpl tự rơi về đoạn prompt dựng sẵn trong
--    enum MinutesFormat (getDefaultDetailPrompt), nên biên bản vẫn sinh đúng.
-- Chạy script này khi muốn quản trị/chỉnh sửa nội dung prompt qua API /minutes-formats.
--
-- Cột minutes_format trên bảng rooms cũng do Hibernate tự thêm; các phòng cũ sẽ có
-- minutes_format = NULL và được suy ra theo MinutesFormat.defaultFor(typeCode) lúc chạy.
-- ==========================================

-- 1. Tạo bảng (nếu chạy tay trước khi khởi động ứng dụng)
CREATE TABLE IF NOT EXISTS minutes_format_template (
    id SERIAL PRIMARY KEY,
    format_code VARCHAR(50) UNIQUE NOT NULL,
    detail_prompt TEXT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Seed 3 mẫu biên bản
-- Dùng dollar-quoted string $$ của PostgreSQL để tránh lỗi ký tự đặc biệt.

INSERT INTO minutes_format_template (format_code, detail_prompt, is_active)
VALUES (
    'ACTION',
    $$MẪU BIÊN BẢN: ACTION MINUTES (biên bản hành động).
Các yêu cầu về mức độ chi tiết dưới đây được áp dụng CÙNG LÚC với hướng dẫn theo loại cuộc họp ở trên:
1. Chỉ ghi lại KẾT LUẬN và ĐẦU VIỆC. Không thuật lại diễn biến thảo luận, lập luận hay ý kiến trái chiều.
2. `executiveSummary`: tối đa 2 câu, chỉ nêu mục đích cuộc họp và kết quả đã chốt.
3. `decisionsMade` và `actionItems`: điền đầy đủ; mỗi mục là một câu ngắn gọn ở thể khẳng định.
4. `discussionTopics`: chỉ liệt kê TÊN chủ đề đã bàn (cụm danh từ ngắn), không diễn giải nội dung.
5. Các trường phân tích sâu (`painPoints`, `prosAndCons`, `qaPairs`): trả về mảng rỗng [],
   TRỪ KHI hướng dẫn theo loại cuộc họp ở trên yêu cầu bắt buộc phải điền trường đó.$$,
    true
) ON CONFLICT (format_code) DO NOTHING;

INSERT INTO minutes_format_template (format_code, detail_prompt, is_active)
VALUES (
    'DISCUSSION',
    $$MẪU BIÊN BẢN: DISCUSSION MINUTES (biên bản thảo luận).
Các yêu cầu về mức độ chi tiết dưới đây được áp dụng CÙNG LÚC với hướng dẫn theo loại cuộc họp ở trên:
1. Ghi lại cả QUÁ TRÌNH dẫn tới kết luận, không chỉ kết luận cuối cùng.
2. `executiveSummary`: 3-5 câu, nêu bối cảnh, các luồng ý kiến chính và kết quả.
3. `discussionTopics`: mỗi mục là một đoạn tóm tắt hoàn chỉnh gồm chủ đề, các quan điểm được nêu
   (ghi rõ tên người phát biểu nếu xác định được) và hướng xử lý.
4. `decisionsMade`: mỗi quyết định phải kèm LÝ DO ngắn gọn theo dạng "Quyết định ... vì ...".
5. Ghi nhận cả ý kiến trái chiều hoặc phương án đã bị loại, kèm lý do bị loại.
6. Vẫn giữ nguyên các quy tắc về `actionItems` và định dạng deadline ở phần hướng dẫn phía trên.$$,
    true
) ON CONFLICT (format_code) DO NOTHING;

-- VERBATIM mặc định KHÔNG đi qua Gemini (xem MeetingFinalizationServiceImpl.saveVerbatimMinutes),
-- prompt dưới đây chỉ dùng nếu sau này bật lại nhánh tóm tắt AI cho mẫu này.
INSERT INTO minutes_format_template (format_code, detail_prompt, is_active)
VALUES (
    'VERBATIM',
    $$MẪU BIÊN BẢN: VERBATIM MINUTES (biên bản gần như nguyên văn).
Các yêu cầu về mức độ chi tiết dưới đây được áp dụng CÙNG LÚC với hướng dẫn theo loại cuộc họp ở trên:
1. Giữ mức chi tiết tối đa, bám sát câu chữ gốc, không rút gọn hay diễn giải lại ý người nói.
2. `discussionTopics`: thuật lại tuần tự diễn biến theo đúng thứ tự phát biểu, trích dẫn nguyên văn
   các câu phát biểu quan trọng và ghi rõ tên người nói.
3. Không lược bỏ nội dung chỉ vì cho rằng nó không quan trọng.
4. Vẫn giữ nguyên các quy tắc về `actionItems` và định dạng deadline ở phần hướng dẫn phía trên.$$,
    true
) ON CONFLICT (format_code) DO NOTHING;
