package com.example.smartmeetbe.strategy.impl;

import com.example.smartmeetbe.constant.MasterSummarySchema;
import com.example.smartmeetbe.constant.MeetingType;
import com.example.smartmeetbe.entity.CustomMeetingType;
import com.example.smartmeetbe.repository.PromptTemplateRepository;
import com.example.smartmeetbe.service.GeminiService;
import com.example.smartmeetbe.service.MeetingTypeService;
import com.example.smartmeetbe.service.MinutesFormatTemplateService;
import com.example.smartmeetbe.strategy.AbstractSummaryStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Chiến lược dùng chung cho mọi loại cuộc họp do người dùng tự tạo: prompt lấy từ
 * {@code custom_meeting_type.system_prompt} (được sinh từ các mục người dùng đã tích),
 * schema dùng Master Schema chung.
 *
 * <p>Nếu loại đã bị xoá mà phòng cũ vẫn tham chiếu tới, tự rơi về prompt của GENERAL.
 */
@Slf4j
@Component
public class CustomTypeSummaryStrategy extends AbstractSummaryStrategy {

    private final MeetingTypeService meetingTypeService;

    public CustomTypeSummaryStrategy(GeminiService geminiService,
                                     PromptTemplateRepository promptRepository,
                                     MinutesFormatTemplateService minutesFormatTemplateService,
                                     MeetingTypeService meetingTypeService,
                                     ObjectMapper objectMapper) {
        super(geminiService, promptRepository, minutesFormatTemplateService, objectMapper);
        this.meetingTypeService = meetingTypeService;
    }

    @Override
    public String getTypeCode() {
        return CUSTOM_TYPE_CODE;
    }

    @Override
    protected PromptBundle loadPrompt(String typeCode) {
        return meetingTypeService.findCustomTypeByCode(typeCode)
                .map(this::toBundle)
                .orElseGet(() -> {
                    log.warn("Custom meeting type {} no longer exists, falling back to GENERAL prompt", typeCode);
                    return loadGeneralPrompt();
                });
    }

    private PromptBundle toBundle(CustomMeetingType custom) {
        return new PromptBundle(custom.getSystemPrompt(), MasterSummarySchema.SCHEMA);
    }

    private PromptBundle loadGeneralPrompt() {
        var template = promptRepository.findByTypeCodeAndIsActiveTrue(MeetingType.GENERAL)
                .orElseThrow(() -> new IllegalStateException("Default GENERAL prompt not configured in database. Please seed prompt data."));
        return new PromptBundle(template.getSystemPrompt(), template.getJsonSchema());
    }
}
