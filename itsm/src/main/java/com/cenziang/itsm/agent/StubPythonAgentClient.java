package com.cenziang.itsm.agent;

import com.cenziang.itsm.dto.SubmitQuestionRequest;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

public class StubPythonAgentClient implements PythonAgentClient {
    private final AtomicLong sessionSequence = new AtomicLong(1000);

    @Override
    public AgentAnswer answer(SubmitQuestionRequest request) {
        String text = (request.title() + " " + request.description()).toLowerCase(Locale.ROOT);
        boolean needsHuman = text.contains("转人工")
                || text.contains("人工")
                || text.contains("无法")
                || text.contains("不能")
                || text.contains("权限");
        double confidence = needsHuman ? 0.48 : 0.88;
        String answer = needsHuman
                ? "已记录问题并建议转人工处理。后续客服可查看 Agent 摘要、原始问题和用户环境信息。"
                : "建议先按知识库标准步骤检查网络、账号状态和客户端缓存；如仍未恢复，可一键转人工。";
        return new AgentAnswer(
                "agent-stub-" + sessionSequence.incrementAndGet(),
                answer,
                confidence,
                needsHuman,
                "reserved-python-rag: FAQ/desktop/network/account",
                request.category(),
                request.priority()
        );
    }
}
