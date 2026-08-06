package com.example.smartmeetbe.service;

import com.example.smartmeetbe.constant.MinutesFormat;
import com.example.smartmeetbe.entity.MinutesFormatTemplate;
import java.util.List;

public interface MinutesFormatTemplateService {
    List<MinutesFormatTemplate> getAllTemplates();
    MinutesFormatTemplate getTemplateByFormatCode(MinutesFormat formatCode);
    MinutesFormatTemplate createTemplate(MinutesFormatTemplate template);
    MinutesFormatTemplate updateTemplate(Long id, MinutesFormatTemplate details);
    void deleteTemplate(Long id);

    /**
     * Đoạn chỉ dẫn "mức độ chi tiết" dùng khi ghép prompt, tự rơi về bản dựng sẵn
     * trong {@link MinutesFormat} nếu chưa có bản ghi active trong database.
     */
    String resolveDetailPrompt(MinutesFormat formatCode);
}
