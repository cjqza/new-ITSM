# 第1轮接口文档 v1.0：用户提问与 Agent 预处理

## 1. 团队老大结论

本轮先完成 Java 后端的用户提问入口、Agent 预处理占位、工单详情查询和 Python Agent 能力预留接口。Python 智能 Agent 暂不实现真实 RAG/LLM，只通过 `PythonAgentClient` 接口和 `StubPythonAgentClient` 返回预留结果。

## 2. 接口清单

| 接口编号 | 接口名称 | 方法 | 路径 | 所属服务 |
|---|---|---|---|---|
| ITSM-R1-001 | 用户提交问题 | POST | `/api/v1/user/tickets/questions` | itsm Java 主服务 |
| ITSM-R1-002 | 用户查看工单详情 | GET | `/api/v1/user/tickets/{ticketId}` | itsm Java 主服务 |
| ITSM-R1-003 | 查询 Python Agent 预留能力 | GET | `/api/v1/agent/capabilities` | itsm Java 主服务 |

## 3. 通用请求头

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| X-Tenant-Id | String | 是 | 租户 ID，所有接口必须校验 |
| X-User-Id | String | 用户侧接口必填 | 当前用户 ID |
| X-User-Type | String | 否 | `INTERNAL` 或 `EXTERNAL`，默认 `INTERNAL` |
| X-Trace-Id | String | 否 | 链路追踪 ID，错误响应中透传 |

## 4. ITSM-R1-001 用户提交问题

### 4.1 业务目标

用户提交桌面、办公软件、账号权限、网络/VPN、终端故障等问题后，Java 主服务创建工单，调用预留 Python Agent 客户端生成回答、置信度、来源摘要和是否转人工建议。

### 4.2 请求体

```json
{
  "title": "VPN 无法连接",
  "description": "用户连接企业 VPN 时提示网络异常",
  "category": "NETWORK",
  "priority": "MEDIUM",
  "environment": "Windows 11 / VPN Client 5.0",
  "attachments": ["screenshot-001.png"],
  "agentReservation": {
    "pythonAgentEnabled": true,
    "agentProfile": "desktop-support",
    "knowledgeBaseScope": "network"
  }
}
```

### 4.3 成功响应

```json
{
  "code": "SUCCESS",
  "message": "success",
  "data": {
    "ticketId": "TCK-1001",
    "status": "AGENT_ANSWERED",
    "agentSessionId": "agent-uuid",
    "agentAnswer": "建议先按知识库标准步骤检查网络、账号状态和客户端缓存；如仍未恢复，可一键转人工。",
    "agentConfidence": 0.88,
    "agentSuggestedHandoff": false,
    "agentSourceSummary": "reserved-python-rag: FAQ/desktop/network/account"
  }
}
```

## 5. 状态机规则

- 创建后初始状态为 `SUBMITTED`。
- 提交后立即进入 `AGENT_PROCESSING`。
- Agent 置信度大于等于 `0.65` 且未建议转人工时进入 `AGENT_ANSWERED`。
- Agent 置信度低于 `0.65` 或建议转人工时进入 `PENDING_HUMAN`。

## 6. 权限与租户隔离

- 用户只能查看本租户内本人提交的工单。
- 客服角色暂不通过第 1 轮接口处理工单。
- 跨租户访问必须返回 `FORBIDDEN`。

## 7. 前端 Agent 任务

- 只根据本接口文档设计提交问题表单、Agent 回答展示、转人工入口占位和工单详情展示。
- 不允许新增接口字段，不允许与后端私下约定接口变化。

## 8. 后端 Agent 任务

- 实现用户提交、详情查询、Agent 能力预留三个 Java 接口。
- 实现 `PythonAgentClient` 预留接口，不接真实 Python 服务。
- 实现租户校验、用户校验、状态机和审计事件。

## 9. 测试 Agent 必测场景

- 正常提交后返回 Agent 回答。
- Agent 低置信度时自动进入 `PENDING_HUMAN`。
- 工单详情查询返回状态历史和审计记录。
- 跨租户访问返回 `FORBIDDEN`。
- 缺少必填参数返回 `VALIDATION_ERROR`。

## 10. 验收标准

- Java 代码中存在用户提交接口、工单详情接口和 Agent 预留能力接口。
- 服务层能完成提交、Agent 预处理、状态流转和审计。
- 测试用例覆盖高置信度回答、低置信度转人工、跨租户隔离。

