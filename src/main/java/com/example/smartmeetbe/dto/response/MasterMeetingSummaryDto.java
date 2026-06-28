package com.example.smartmeetbe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasterMeetingSummaryDto {
    private String executiveSummary;
    private List<String> discussionTopics;
    private List<String> decisionsMade;
    private List<ActionItemDto> actionItems;
    private List<QaPairDto> qaPairs;
    private List<String> painPoints;
    private List<ProConDto> prosAndCons;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionItemDto {
        private String task;
        private String assignee;
        private String deadline; // Định dạng chuỗi rỗng "" hoặc YYYY-MM-DD từ AI
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QaPairDto {
        private String question;
        private String answer;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProConDto {
        private String idea;
        private String pros;
        private String cons;
    }
}
