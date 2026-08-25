package com.cenziang.itsm.agent;

public record AgentAnswer(
        String sessionId,
        String answer,
        double confidence,
        boolean suggestedHandoff,
        String sourceSummary,
        String suggestedCategory,
        String suggestedPriority
) {
}
