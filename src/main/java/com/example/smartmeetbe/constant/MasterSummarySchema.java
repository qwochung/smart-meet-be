package com.example.smartmeetbe.constant;

/**
 * JSON Schema dùng chung cho các loại cuộc họp do người dùng tự tạo.
 * Giống schema đã seed cho 6 loại dựng sẵn ở {@code prompt_seed.sql}: luôn khai báo đủ
 * mọi trường của {@code MasterMeetingSummaryDto}, còn việc trường nào được điền
 * do system prompt quyết định.
 */
public final class MasterSummarySchema {

    public static final String SCHEMA = """
            {
              "type": "object",
              "properties": {
                "executiveSummary": { "type": "string" },
                "discussionTopics": { "type": "array", "items": { "type": "string" } },
                "decisionsMade": { "type": "array", "items": { "type": "string" } },
                "actionItems": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "task": { "type": "string" },
                      "assignee": { "type": "string" },
                      "deadline": { "type": "string" }
                    },
                    "required": ["task", "assignee", "deadline"]
                  }
                },
                "qaPairs": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "question": { "type": "string" },
                      "answer": { "type": "string" }
                    },
                    "required": ["question", "answer"]
                  }
                },
                "painPoints": { "type": "array", "items": { "type": "string" } },
                "prosAndCons": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "idea": { "type": "string" },
                      "pros": { "type": "string" },
                      "cons": { "type": "string" }
                    },
                    "required": ["idea", "pros", "cons"]
                  }
                }
              },
              "required": ["executiveSummary"]
            }""";

    private MasterSummarySchema() {
    }
}
