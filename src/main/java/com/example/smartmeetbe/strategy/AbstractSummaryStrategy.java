package com.example.smartmeetbe.strategy;

import com.example.smartmeetbe.constant.MeetingType;
import com.example.smartmeetbe.constant.MinutesFormat;
import com.example.smartmeetbe.dto.response.MasterMeetingSummaryDto;
import com.example.smartmeetbe.repository.PromptTemplateRepository;
import com.example.smartmeetbe.service.GeminiService;
import com.example.smartmeetbe.service.MinutesFormatTemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
public abstract class AbstractSummaryStrategy implements MeetingSummaryStrategy {

    /** Cặp prompt gửi cho Gemini: chỉ dẫn chủ đề + schema JSON của kết quả. */
    public record PromptBundle(String systemPrompt, String jsonSchema) {
    }

    protected final GeminiService geminiService;
    protected final PromptTemplateRepository promptRepository;
    protected final MinutesFormatTemplateService minutesFormatTemplateService;
    protected final ObjectMapper objectMapper;

    protected AbstractSummaryStrategy(GeminiService geminiService,
                                     PromptTemplateRepository promptRepository,
                                     MinutesFormatTemplateService minutesFormatTemplateService,
                                     ObjectMapper objectMapper) {
        this.geminiService = geminiService;
        this.promptRepository = promptRepository;
        this.minutesFormatTemplateService = minutesFormatTemplateService;
        this.objectMapper = objectMapper;
    }

    @Override
    public MasterMeetingSummaryDto generateSummary(String roomId, String typeCode,
                                                   String fullRawTranscript, MinutesFormat minutesFormat) {
        MinutesFormat format = (minutesFormat != null) ? minutesFormat : MinutesFormat.defaultFor(typeCode);

        // 1. Tải prompt theo loại cuộc họp (loại dựng sẵn và loại tự tạo lấy từ nguồn khác nhau)
        PromptBundle bundle = loadPrompt(typeCode);

        // 2. Ghép hai lớp prompt: chủ đề cần trích xuất + mức độ chi tiết
        String systemPrompt = composeSystemPrompt(bundle.systemPrompt(), format);

        // 3. Gọi Gemini API để sinh tóm tắt dạng JSON dựa theo Schema
        String jsonResult = geminiService.generateSummary(systemPrompt, fullRawTranscript, bundle.jsonSchema());

        // 4. Giải mã và chuyển đổi kết quả sang Master DTO
        try {
            return objectMapper.readValue(jsonResult, MasterMeetingSummaryDto.class);
        } catch (Exception e) {
            log.error("Failed to parse JSON summary from Gemini for room {}, type {} and format {}: {}. Raw JSON: {}",
                    roomId, typeCode, format, e.getMessage(), jsonResult);
            // Trả về DTO trống kèm thông báo lỗi để giao diện không bị crash
            return MasterMeetingSummaryDto.builder()
                    .executiveSummary("Lỗi hệ thống khi tóm tắt cuộc họp: Không thể xử lý kết quả trả về từ AI.")
                    .discussionTopics(List.of())
                    .decisionsMade(List.of())
                    .actionItems(List.of())
                    .qaPairs(List.of())
                    .painPoints(List.of())
                    .prosAndCons(List.of())
                    .build();
        }
    }

    /**
     * Mặc định lấy prompt của loại dựng sẵn từ bảng {@code prompt_template}.
     * {@code CustomTypeSummaryStrategy} ghi đè để đọc prompt do người dùng cấu hình.
     */
    protected PromptBundle loadPrompt(String typeCode) {
        MeetingType type = MeetingType.fromCode(getTypeCode());
        var promptTemplate = promptRepository.findByTypeCodeAndIsActiveTrue(type)
                .orElseGet(() -> {
                    log.warn("Active prompt for type {} not found, fallback to GENERAL", type);
                    return promptRepository.findByTypeCodeAndIsActiveTrue(MeetingType.GENERAL)
                            .orElseThrow(() -> new IllegalStateException("Default GENERAL prompt not configured in database. Please seed prompt data."));
                });
        return new PromptBundle(promptTemplate.getSystemPrompt(), promptTemplate.getJsonSchema());
    }

    /**
     * Nối chỉ dẫn về mức độ chi tiết vào cuối system prompt của loại cuộc họp.
     * Đặt ở cuối để phần "mức độ chi tiết" là chỉ dẫn Gemini đọc sau cùng, giảm nguy cơ bị
     * hướng dẫn theo chủ đề ở trên lấn át.
     */
    private String composeSystemPrompt(String typePrompt, MinutesFormat format) {
        String detailPrompt = minutesFormatTemplateService.resolveDetailPrompt(format);
        if (detailPrompt == null || detailPrompt.isBlank()) {
            return typePrompt;
        }
        return typePrompt + "\n\n---\n" + detailPrompt;
    }
}
