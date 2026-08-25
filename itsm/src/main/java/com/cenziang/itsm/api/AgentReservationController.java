package com.cenziang.itsm.api;

import com.cenziang.itsm.dto.AgentCapabilityResponse;
import com.cenziang.itsm.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/agent")
public class AgentReservationController {
    @GetMapping("/capabilities")
    public ApiResponse<AgentCapabilityResponse> capabilities() {
        return ApiResponse.success(new AgentCapabilityResponse(
                "python-agent-reserved",
                "RESERVED_STUB",
                List.of("知识库检索", "LLM 回答", "置信度评估", "来源引用", "转人工摘要", "建议分类", "建议优先级"),
                List.of("直接修改工单状态", "远程控制用户电脑", "自动执行修复脚本")
        ));
    }
}
