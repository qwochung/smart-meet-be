package com.example.smartmeetbe.strategy;

import com.example.smartmeetbe.constant.MeetingType;
import com.example.smartmeetbe.dto.response.MasterMeetingSummaryDto;
import com.example.smartmeetbe.repository.PromptTemplateRepository;
import com.example.smartmeetbe.service.GeminiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
public abstract class AbstractSummaryStrategy implements MeetingSummaryStrategy {

    protected final GeminiService geminiService;
    protected final PromptTemplateRepository promptRepository;
    protected final ObjectMapper objectMapper;

    protected AbstractSummaryStrategy(GeminiService geminiService,
                                     PromptTemplateRepository promptRepository,
                                     ObjectMapper objectMapper) {
        this.geminiService = geminiService;
        this.promptRepository = promptRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public MasterMeetingSummaryDto generateSummary(String roomId, String fullRawTranscript) {
        MeetingType typeCode = getTypeCode();

        // 1. Tải active prompt từ database
        var promptTemplate = promptRepository.findByTypeCodeAndIsActiveTrue(typeCode)
                .orElseGet(() -> {
                    log.warn("Active prompt for type {} not found, fallback to GENERAL", typeCode);
                    return promptRepository.findByTypeCodeAndIsActiveTrue(MeetingType.GENERAL)
                            .orElseThrow(() -> new IllegalStateException("Default GENERAL prompt not configured in database. Please seed prompt data."));
                });

        // 2. Gọi Gemini API để sinh tóm tắt dạng JSON dựa theo Schema của Prompt
        String jsonResult = geminiService.generateSummary(
                promptTemplate.getSystemPrompt(),
                fullRawTranscript,
                promptTemplate.getJsonSchema()
        );

        // 3. Giải mã và chuyển đổi kết quả sang Master DTO
        try {
            return objectMapper.readValue(jsonResult, MasterMeetingSummaryDto.class);
        } catch (Exception e) {
            log.error("Failed to parse JSON summary from Gemini for room {} and type {}: {}. Raw JSON: {}", 
                    roomId, typeCode, e.getMessage(), jsonResult);
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
}
