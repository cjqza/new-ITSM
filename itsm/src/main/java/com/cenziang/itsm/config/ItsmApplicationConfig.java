package com.cenziang.itsm.config;

import com.cenziang.itsm.agent.PythonAgentClient;
import com.cenziang.itsm.agent.StubPythonAgentClient;
import com.cenziang.itsm.application.TicketWorkflowService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ItsmApplicationConfig {
    @Bean
    public PythonAgentClient pythonAgentClient() {
        return new StubPythonAgentClient();
    }

    @Bean
    public TicketWorkflowService ticketWorkflowService(PythonAgentClient pythonAgentClient) {
        return new TicketWorkflowService(pythonAgentClient);
    }
}
