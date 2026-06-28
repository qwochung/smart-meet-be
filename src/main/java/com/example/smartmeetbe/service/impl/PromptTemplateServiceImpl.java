package com.example.smartmeetbe.service.impl;

import com.example.smartmeetbe.constant.MeetingType;
import com.example.smartmeetbe.entity.PromptTemplate;
import com.example.smartmeetbe.repository.PromptTemplateRepository;
import com.example.smartmeetbe.service.PromptTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromptTemplateServiceImpl implements PromptTemplateService {

    private final PromptTemplateRepository promptRepository;

    @Override
    public List<PromptTemplate> getAllPrompts() {
        return promptRepository.findAll();
    }

    @Override
    public PromptTemplate getPromptByTypeCode(MeetingType typeCode) {
        return promptRepository.findByTypeCodeAndIsActiveTrue(typeCode)
                .orElseThrow(() -> new IllegalArgumentException("No active prompt found for type: " + typeCode));
    }

    @Override
    @Transactional
    public PromptTemplate createPrompt(PromptTemplate prompt) {
        // Kiểm tra xem typeCode đã tồn tại chưa (do ràng buộc UNIQUE)
        promptRepository.findByTypeCode(prompt.getTypeCode()).ifPresent(existing -> {
            throw new IllegalArgumentException("Prompt with typeCode '" + prompt.getTypeCode() + "' already exists. Use update API instead.");
        });

        // Nếu prompt mới được set active, vô hiệu hoá các active prompt cũ của cùng typeCode (nếu có, đề phòng sau này bỏ unique)
        if (Boolean.TRUE.equals(prompt.getIsActive())) {
            deactivateOldPrompts(prompt.getTypeCode());
        }

        return promptRepository.save(prompt);
    }

    @Override
    @Transactional
    public PromptTemplate updatePrompt(Long id, PromptTemplate promptDetails) {
        PromptTemplate prompt = promptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Prompt not found with id: " + id));

        // Kiểm tra xem thay đổi typeCode có bị trùng lặp với bản ghi khác không
        if (!prompt.getTypeCode().equals(promptDetails.getTypeCode())) {
            promptRepository.findByTypeCode(promptDetails.getTypeCode()).ifPresent(existing -> {
                throw new IllegalArgumentException("Prompt with typeCode '" + promptDetails.getTypeCode() + "' already exists.");
            });
        }

        prompt.setTypeCode(promptDetails.getTypeCode());
        prompt.setSystemPrompt(promptDetails.getSystemPrompt());
        prompt.setJsonSchema(promptDetails.getJsonSchema());

        // Nếu thay đổi trạng thái thành active
        if (Boolean.TRUE.equals(promptDetails.getIsActive()) && !Boolean.TRUE.equals(prompt.getIsActive())) {
            deactivateOldPrompts(prompt.getTypeCode());
        }
        prompt.setIsActive(promptDetails.getIsActive());

        return promptRepository.save(prompt);
    }

    @Override
    @Transactional
    public void deletePrompt(Long id) {
        PromptTemplate prompt = promptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Prompt not found with id: " + id));
        promptRepository.delete(prompt);
    }

    private void deactivateOldPrompts(MeetingType typeCode) {
        // Trong trường hợp bỏ unique sau này để quản lý versioning, hàm này sẽ set isActive = false cho tất cả prompt cũ
        promptRepository.findByTypeCodeAndIsActiveTrue(typeCode).ifPresent(oldPrompt -> {
            oldPrompt.setIsActive(false);
            promptRepository.save(oldPrompt);
        });
    }
}
