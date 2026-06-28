package com.example.smartmeetbe.service;

import com.example.smartmeetbe.constant.MeetingType;
import com.example.smartmeetbe.entity.PromptTemplate;
import java.util.List;

public interface PromptTemplateService {
    List<PromptTemplate> getAllPrompts();
    PromptTemplate getPromptByTypeCode(MeetingType typeCode);
    PromptTemplate createPrompt(PromptTemplate prompt);
    PromptTemplate updatePrompt(Long id, PromptTemplate promptDetails);
    void deletePrompt(Long id);
}
