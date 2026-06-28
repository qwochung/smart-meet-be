package com.example.smartmeetbe.strategy.impl;

import com.example.smartmeetbe.constant.MeetingType;
import com.example.smartmeetbe.repository.PromptTemplateRepository;
import com.example.smartmeetbe.service.GeminiService;
import com.example.smartmeetbe.strategy.AbstractSummaryStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class ClientSalesSummaryStrategy extends AbstractSummaryStrategy {

    public ClientSalesSummaryStrategy(GeminiService geminiService,
                                      PromptTemplateRepository promptRepository,
                                      ObjectMapper objectMapper) {
        super(geminiService, promptRepository, objectMapper);
    }

    @Override
    public MeetingType getTypeCode() {
        return MeetingType.CLIENT_SALES;
    }
}
