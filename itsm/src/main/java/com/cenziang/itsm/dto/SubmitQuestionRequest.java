package com.cenziang.itsm.dto;

import java.util.List;

public record SubmitQuestionRequest(
        String title,
        String description,
        String category,
        String priority,
        String environment,
        List<String> attachments,
        AgentReservation agentReservation
) {
    public record AgentReservation(
            boolean pythonAgentEnabled,
            String agentProfile,
            String knowledgeBaseScope
    ) {
    }
}
