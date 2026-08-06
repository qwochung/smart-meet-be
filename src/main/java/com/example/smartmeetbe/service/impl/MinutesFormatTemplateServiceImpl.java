package com.example.smartmeetbe.service.impl;

import com.example.smartmeetbe.constant.MinutesFormat;
import com.example.smartmeetbe.entity.MinutesFormatTemplate;
import com.example.smartmeetbe.repository.MinutesFormatTemplateRepository;
import com.example.smartmeetbe.service.MinutesFormatTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinutesFormatTemplateServiceImpl implements MinutesFormatTemplateService {

    private final MinutesFormatTemplateRepository templateRepository;

    @Override
    public List<MinutesFormatTemplate> getAllTemplates() {
        return templateRepository.findAll();
    }

    @Override
    public MinutesFormatTemplate getTemplateByFormatCode(MinutesFormat formatCode) {
        return templateRepository.findByFormatCodeAndIsActiveTrue(formatCode)
                .orElseThrow(() -> new IllegalArgumentException("No active minutes format template found for: " + formatCode));
    }

    @Override
    @Transactional
    public MinutesFormatTemplate createTemplate(MinutesFormatTemplate template) {
        templateRepository.findByFormatCode(template.getFormatCode()).ifPresent(existing -> {
            throw new IllegalArgumentException("Template with formatCode '" + template.getFormatCode()
                    + "' already exists. Use update API instead.");
        });
        return templateRepository.save(template);
    }

    @Override
    @Transactional
    public MinutesFormatTemplate updateTemplate(Long id, MinutesFormatTemplate details) {
        MinutesFormatTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Minutes format template not found with id: " + id));

        // Ràng buộc UNIQUE trên format_code: chỉ cho đổi sang mã chưa có bản ghi khác chiếm giữ
        if (!template.getFormatCode().equals(details.getFormatCode())) {
            templateRepository.findByFormatCode(details.getFormatCode()).ifPresent(existing -> {
                throw new IllegalArgumentException("Template with formatCode '" + details.getFormatCode() + "' already exists.");
            });
            template.setFormatCode(details.getFormatCode());
        }

        template.setDetailPrompt(details.getDetailPrompt());
        if (details.getIsActive() != null) {
            template.setIsActive(details.getIsActive());
        }
        return templateRepository.save(template);
    }

    @Override
    @Transactional
    public void deleteTemplate(Long id) {
        MinutesFormatTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Minutes format template not found with id: " + id));
        templateRepository.delete(template);
    }

    @Override
    public String resolveDetailPrompt(MinutesFormat formatCode) {
        MinutesFormat format = (formatCode != null) ? formatCode : MinutesFormat.ACTION;
        return templateRepository.findByFormatCodeAndIsActiveTrue(format)
                .map(MinutesFormatTemplate::getDetailPrompt)
                .filter(prompt -> !prompt.isBlank())
                .orElseGet(() -> {
                    log.debug("No active minutes format template for {}, using built-in default prompt", format);
                    return format.getDefaultDetailPrompt();
                });
    }
}
