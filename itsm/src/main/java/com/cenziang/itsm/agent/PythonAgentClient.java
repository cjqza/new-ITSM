package com.cenziang.itsm.agent;

import com.cenziang.itsm.dto.SubmitQuestionRequest;

public interface PythonAgentClient {
    AgentAnswer answer(SubmitQuestionRequest request);
}
