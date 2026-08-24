package com.example.smartmeetbe.strategy;

import com.example.smartmeetbe.constant.MeetingType;
import com.example.smartmeetbe.constant.MinutesFormat;
import com.example.smartmeetbe.dto.response.MasterMeetingSummaryDto;
import com.example.smartmeetbe.repository.PromptTemplateRepository;
import com.example.smartmeetbe.service.GeminiService;
import com.example.smartmeetbe.service.MinutesFormatTemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

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
                                                   String fullRawTranscript, MinutesFormat minutesFormat,
                                                   LocalDate meetingDate) {
        MinutesFormat format = (minutesFormat != null) ? minutesFormat : MinutesFormat.defaultFor(typeCode);

        // 1. Tải prompt theo loại cuộc họp (loại dựng sẵn và loại tự tạo lấy từ nguồn khác nhau)
        PromptBundle bundle = loadPrompt(typeCode);

        // 2. Ghép ba lớp prompt: chủ đề cần trích xuất + mức độ chi tiết + mốc ngày họp
        String systemPrompt = composeSystemPrompt(bundle.systemPrompt(), format, meetingDate);

        // 3. Gọi Gemini API để sinh tóm tắt dạng JSON dựa theo Schema
        String jsonResult = geminiService.generateSummary(systemPrompt, fullRawTranscript, bundle.jsonSchema());

        // 4. Giải mã và chuyển đổi kết quả sang Master DTO
        try {
            MasterMeetingSummaryDto dto = objectMapper.readValue(jsonResult, MasterMeetingSummaryDto.class);
            groundDeadlines(dto, meetingDate);
            return dto;
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
    private String composeSystemPrompt(String typePrompt, MinutesFormat format, LocalDate meetingDate) {
        StringBuilder prompt = new StringBuilder(typePrompt);
        String detailPrompt = minutesFormatTemplateService.resolveDetailPrompt(format);
        if (detailPrompt != null && !detailPrompt.isBlank()) {
            prompt.append("\n\n---\n").append(detailPrompt);
        }
        if (meetingDate != null) {
            prompt.append("\n\n---\n").append(meetingDateGrounding(meetingDate));
        }
        return prompt.toString();
    }

    /**
     * Without an absolute anchor the model resolves relative deadlines ("thứ Tư", "tuần sau")
     * against its training-data prior and emits a year unrelated to the meeting.
     */
    private String meetingDateGrounding(LocalDate meetingDate) {
        String weekday = meetingDate.getDayOfWeek()
                .getDisplayName(TextStyle.FULL, Locale.forLanguageTag("vi"));
        return """
                Cuộc họp diễn ra vào ngày %s (%s).
                Mọi mốc thời gian tương đối trong transcript ("thứ Tư", "ngày mai", "cuối tuần này",\
                 "tuần sau"...) phải được quy đổi theo ngày này.
                Trường `deadline` bắt buộc dùng năm suy ra từ ngày họp, không được dùng năm khác,\
                 và không bao giờ nằm trước ngày họp."""
                .formatted(meetingDate, weekday);
    }

    /**
     * Safety net for the prompt instruction above: a deadline earlier than the meeting itself is
     * always a model mistake, so pull it forward to the first matching month-day from the meeting on.
     */
    static void groundDeadlines(MasterMeetingSummaryDto dto, LocalDate meetingDate) {
        if (meetingDate == null || dto.getActionItems() == null) {
            return;
        }
        for (MasterMeetingSummaryDto.ActionItemDto item : dto.getActionItems()) {
            item.setDeadline(groundDeadline(item.getDeadline(), meetingDate));
        }
    }

    static String groundDeadline(String deadline, LocalDate meetingDate) {
        if (deadline == null || deadline.isBlank()) {
            return deadline;
        }
        LocalDate parsed;
        try {
            parsed = LocalDate.parse(deadline.trim());
        } catch (DateTimeParseException e) {
            // Prompt requires YYYY-MM-DD; anything else is passed through untouched for the report
            log.warn("Action item deadline '{}' is not YYYY-MM-DD, leaving it as is", deadline);
            return deadline;
        }
        if (!parsed.isBefore(meetingDate)) {
            return deadline;
        }
        LocalDate corrected = parsed.withYear(meetingDate.getYear());
        if (corrected.isBefore(meetingDate)) {
            corrected = corrected.plusYears(1);
        }
        log.info("Corrected action item deadline {} to {} using meeting date {}", deadline, corrected, meetingDate);
        return corrected.toString();
    }
}
