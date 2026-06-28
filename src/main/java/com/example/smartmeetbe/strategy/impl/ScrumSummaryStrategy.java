package com.example.smartmeetbe.strategy.impl;

import com.example.smartmeetbe.constant.MeetingType;
import com.example.smartmeetbe.repository.PromptTemplateRepository;
import com.example.smartmeetbe.service.GeminiService;
import com.example.smartmeetbe.strategy.AbstractSummaryStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class ScrumSummaryStrategy extends AbstractSummaryStrategy {

    public ScrumSummaryStrategy(GeminiService geminiService,
                                PromptTemplateRepository promptRepository,
                                ObjectMapper objectMapper) {
        super(geminiService, promptRepository, objectMapper);
    }

    @Override
    public MeetingType getTypeCode() {
        return MeetingType.SCRUM_SYNC;
    }
}
