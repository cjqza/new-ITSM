# ITSM 一期核心接口文档 v1.0

> 文档状态：一期首版执行基线  
> 产品需求唯一基线：`计划书/ITSM桌面工单处理系统计划书.md`  
> 编写范围：用户侧、智能客服 Agent、客服侧、管理员侧核心接口  
> 编写日期：2026-08-25  
> 适用对象：前端 Agent、后端 Agent、测试 Agent、分析师 Agent、file-agent  
> 说明：本文档只定义接口契约，不包含前端代码、后端代码、数据库代码或测试代码。

## 1. 使用规则与决策标记

本文档是一期前后端实现、测试和联调的唯一接口依据。接口路径、HTTP 方法、字段名、错误码、状态值、权限规则、领域事件和副作用均以本文档为准。任何实现方如果发现契约不足或存在冲突，只能反馈给团队老大 Agent，由团队老大 Agent 修改本文档后重新下发。

文档中的决策标记含义如下：

| 标记 | 含义 |
| --- | --- |
| `[一期确定]` | 一期必须按本文档实现和测试的内容 |
| `[待确认]` | 计划书未给出足够细节，本文档给出临时默认值；在确认前不得擅自改成其他契约 |
| `[参考复用]` | 从 Git 中已删除的旧接口资料中吸收的结构或命名，仅在不与计划书冲突时保留 |
| `[不纳入一期]` | 计划书提到但本版不提供公开接口，后续另行立项 |

### 1.1 一期确定范围

一期覆盖以下完整主链路：

1. 企业身份登录和当前用户权限查询。
2. 创建、读取会话。
3. 用户发送消息，获得 Agent 自助回答或人工转接结果。
4. Agent 自助解决、明确转人工、自动创建工单并携带会话摘要。
5. 用户创建工单、查询工单分页、查询工单详情。
6. 客服查询队列、受理工单。
7. 客服更新管理单元、症状、原因、解决方法。
8. 客服提交解决结果，用户确认解决，客服关闭，用户重开。
9. 用户评价已解决或已关闭工单。
10. 管理员查询和维护工单字典项。
11. 管理员查询角色和角色权限。

### 1.2 明确不在本版解决的内容

- 知识库检索、FAQ 检索算法和 RAG/LLM 模型协议。
- SLA 策略配置、超时升级规则配置和运营报表。
- 附件上传服务本身；接口只接收已上传文件的 `fileId`。
- 角色、权限和分流规则的复杂编辑接口；本版先提供权限查询和字典维护。
- 远程控制用户电脑、自动执行修复脚本。
- 直接暴露数据库字段、MQ 消息结构或内部 Agent 模型提示词。

## 2. 总体接口约定

### 2.1 服务划分

| 服务标识 | 服务职责 | 一期接口编号范围 |
| --- | --- | --- |
| `auth-service` | 登录、令牌、当前用户身份 | `AUTH` |
| `conversation-service` | 会话、消息、聊天上下文 | `SESSION`、`CHAT` |
| `agent-orchestrator` | 接收 Agent 决策，触发自助解决或人工转接 | `AGENT` |
| `ticket-service` | 工单事实、分页、详情、状态流转、审计 | `TICKET`、`SUPPORT` |
| `rating-service` | 用户满意度评价 | `RATING` |
| `dictionary-service` | 管理单元、症状、原因、解决方法等字典 | `DICT` |
| `permission-service` | 当前用户、角色、菜单/按钮/数据权限查询 | `PERM` |

一期可以由单体应用或模块化单体承载，但对外接口必须保持上述领域边界。服务拆分是后端实现边界，不要求一期部署为多个进程。

### 2.2 基础路径与媒体类型

- 基础路径：`/api/v1`
- 请求媒体类型：`application/json; charset=utf-8`
- 响应媒体类型：`application/json; charset=utf-8`
- 时间格式：ISO-8601 UTC，例如 `2026-08-25T08:16:38Z`
- 分页页码：从 `1` 开始
- ID 类型：对外使用不可猜测的字符串 ID；工单展示编号可以与内部 `ticketId` 不同
- 枚举值：接口传输使用大写英文枚举，前端显示中文由字典或前端映射负责

### 2.3 统一请求 Header

| Header | 必填 | 适用接口 | 说明 |
| --- | --- | --- | --- |
| `Authorization` | 登录后必填 | 除登录接口外的用户、客服、管理员接口 | `Bearer <accessToken>` |
| `X-Tenant-Id` | 登录后必填 | 全部租户业务接口 | 当前租户。网关必须校验它与令牌 `tenant_id` 一致 |
| `X-Trace-Id` | 否 | 全部接口 | 调用方生成或网关生成；服务端必须透传到响应和日志 |
| `Idempotency-Key` | 命令接口必填 | 创建、发送消息、状态变更、字典写操作、评价 | 同一租户内 24 小时有效；值由调用方生成 |
| `X-Client-Version` | 否 | 前端调用接口 | 前端版本标识，仅用于排查问题，不参与业务判断 |

以下 Header 不属于可信身份来源，服务端不得以其作为最终权限依据：

- `X-User-Id`
- `X-Operator-Id`
- `X-Role`
- `X-User-Type`

操作者、角色、租户和数据范围必须从已验证的令牌、服务凭证和权限服务结果中取得。旧资料中使用的 `X-User-Id`、`X-Operator-Id`、`X-Role` 仅作为 `[参考复用]`，一期不作为可信身份输入。

### 2.4 统一响应结构

所有接口都使用以下结构，不因服务或 HTTP 方法不同而改变：

```json
{
  "code": "SUCCESS",
  "message": "success",
  "data": {},
  "traceId": "trc_20260825_001",
  "details": null
}
```

字段定义：

| 字段 | 类型 | 必返 | 说明 |
| --- | --- | --- | --- |
| `code` | String | 是 | `SUCCESS` 或统一错误码 |
| `message` | String | 是 | 面向调用方的稳定提示，不用于前端判断分支 |
| `data` | Object/Array/Null | 是 | 成功数据；失败时为 `null` |
| `traceId` | String | 是 | 链路追踪 ID |
| `details` | Object/Null | 是 | 成功时通常为 `null`；参数校验、分页、冲突等场景返回结构化详情 |

成功响应示例：

```json
{
  "code": "SUCCESS",
  "message": "success",
  "data": {
    "ticketId": "tkt_01J8V9X6Q7",
    "status": "PENDING_ACCEPTANCE"
  },
  "traceId": "trc_20260825_001",
  "details": null
}
```

失败响应示例：

```json
{
  "code": "ILLEGAL_STATE_TRANSITION",
  "message": "当前工单状态不允许执行该操作",
  "data": null,
  "traceId": "trc_20260825_002",
  "details": {
    "resourceType": "TICKET",
    "resourceId": "tkt_01J8V9X6Q7",
    "currentStatus": "CLOSED",
    "allowedStatuses": ["RESOLVED"]
  }
}
```

分页响应统一结构：

```json
{
  "items": [],
  "page": 1,
  "pageSize": 20,
  "total": 0,
  "hasNext": false
}
```

### 2.5 统一幂等规则

1. 命令接口必须携带 `Idempotency-Key`。
2. 服务端以 `tenantId + callerId + method + path + Idempotency-Key` 作为幂等键。
3. 相同幂等键和相同请求摘要重复请求，返回第一次请求的结果，不重复写入业务数据和领域事件。
4. 相同幂等键但请求体摘要不同，返回 `IDEMPOTENCY_CONFLICT`。
5. 幂等记录至少保留 24 小时；具体存储介质由后端决定，推荐 Redis 加持久化兜底。
6. GET 查询接口天然幂等，不要求 `Idempotency-Key`。

### 2.6 统一错误处理

服务端必须将可预期业务异常转换为统一错误结构，不得把堆栈、SQL、令牌、内部服务地址返回给前端。HTTP 状态和业务码同时表达错误类型，前端分支判断以 `code` 为准。

## 3. 统一错误码

| 错误码 | HTTP | 触发条件 | 前端处理约束 | 后端处理约束 |
| --- | ---: | --- | --- | --- |
| `SUCCESS` | 200/201 | 请求成功 | 按 `data` 渲染 | 记录 trace |
| `VALIDATION_ERROR` | 400 | 字段缺失、格式错误、枚举非法、评分越界 | 标记字段或展示 message | 不写业务数据 |
| `MISSING_HEADER` | 400 | 缺少必要 Header | 记录并提示重新加载 | 在网关或入口层拦截 |
| `INVALID_IDEMPOTENCY_KEY` | 400 | 幂等键格式不合法或过期 | 重新生成幂等键后重试 | 不执行命令 |
| `AUTH_REQUIRED` | 401 | 未携带令牌 | 跳转登录 | 不进入业务服务 |
| `TOKEN_EXPIRED` | 401 | 令牌过期 | 尝试刷新；失败后跳转登录 | 不进入业务逻辑 |
| `TOKEN_INVALID` | 401 | 令牌签名或声明非法 | 清理登录态并跳转登录 | 记录安全日志 |
| `TENANT_REQUIRED` | 400 | 缺少租户 ID | 使用登录态重新初始化 | 不执行查询或命令 |
| `TENANT_FORBIDDEN` | 403 | Header 租户与令牌不一致或无租户权限 | 展示无权访问 | 记录安全审计 |
| `ROLE_FORBIDDEN` | 403 | 角色没有接口权限 | 隐藏或禁用入口，并保留兜底提示 | 拒绝业务执行 |
| `DATA_SCOPE_FORBIDDEN` | 403 | 角色有功能权限但无该数据范围 | 展示无权访问 | 按数据权限过滤并拒绝越权 |
| `RESOURCE_NOT_FOUND` | 404 | 会话、工单、字典项、角色不存在 | 展示资源不存在 | 不泄漏其他租户资源存在性 |
| `IDEMPOTENCY_CONFLICT` | 409 | 同一幂等键对应不同请求体 | 生成新幂等键，不自动重复提交 | 不执行第二次请求 |
| `RESOURCE_CONFLICT` | 409 | 资源已被他人更新或重复创建 | 刷新详情后让用户重新操作 | 使用版本号或更新时间校验 |
| `ILLEGAL_STATE_TRANSITION` | 409 | 工单状态不允许当前操作 | 刷新详情并按新状态更新按钮 | 事务内校验状态机 |
| `DICTIONARY_ITEM_DISABLED` | 409 | 使用已停用字典项 | 提示重新选择有效项 | 禁止新工单引用停用项 |
| `AGENT_UNAVAILABLE` | 503 | Agent 超时、不可用或服务降级 | 展示重试或转人工入口 | 不丢失用户消息；可异步重试 |
| `SERVICE_UNAVAILABLE` | 503 | 依赖服务不可用 | 展示稍后重试 | 告警、重试、熔断 |
| `INTERNAL_ERROR` | 500 | 未分类服务端异常 | 展示通用失败提示并带 traceId | 记录完整异常和告警 |

## 4. 角色、权限和租户隔离

### 4.1 一期角色

| 角色编码 | 角色名称 | 数据范围 | 一期核心权限 |
| --- | --- | --- | --- |
| `USER` | 用户 | 仅本人、本租户 | 登录、会话、发消息、查看本人工单、确认、评价、重开 |
| `SUPPORT_AGENT` | 普通客服 | 本租户内被分配或所属业务线工单 | 查询队列、受理、分类、记录原因和解决方法、解决、关闭 |
| `SUPPORT_ADMIN` | 管理员客服 | 本租户全量客服数据，具体按数据权限 | 普通客服权限、字典维护、角色/权限查询 |
| `SUPERVISOR` | 主管/质检 | 本租户只读或授权范围 | 工单查询、详情、审计查看；抽检和报表为后续扩展 |
| `AGENT_SERVICE` | 智能客服服务 | 仅服务凭证声明的租户和会话 | 提交 Agent 决策、生成转人工摘要，不直接修改工单状态 |

`SUPPORT_ADMIN`、`SUPERVISOR` 是计划书定义的角色；`SUPERVISOR` 的完整统计和抽检接口 `[不纳入一期]`。

### 4.2 租户隔离

1. 所有持久化对象必须带 `tenantId`，包括用户、会话、消息、工单、评价、字典项、角色和审计事件。
2. 业务查询必须默认拼接当前有效租户条件，不能仅依赖前端传入的租户字段。
3. 用户身份从令牌取得，用户不能通过 Body 或 Path 指定另一个用户作为请求人。
4. 客服可以查看的工单集合由 `tenantId + role + dataScope + assignment` 共同决定。
5. Agent 服务调用必须校验服务凭证声明的租户与会话租户一致。
6. 发现跨租户资源 ID 时统一返回 `TENANT_FORBIDDEN` 或 `RESOURCE_NOT_FOUND`，不得通过响应差异泄露资源是否存在。

### 4.3 权限判断顺序

1. 认证令牌有效性。
2. 租户 Header 与令牌租户一致性。
3. 功能权限，例如 `ticket:accept`。
4. 数据权限，例如是否属于当前业务线或被分配客服。
5. 工单当前状态是否允许操作。
6. 参数和字典项校验。

## 5. 会话与工单状态流转

### 5.1 会话状态

| 状态 | 说明 |
| --- | --- |
| `ACTIVE` | 可以继续发送用户消息 |
| `HANDOFF_PENDING` | 用户或 Agent 已请求人工，正在创建或分派工单 |
| `TICKET_CREATED` | 已创建关联工单，聊天上下文继续保留 |
| `ENDED` | 用户结束自助会话，不能继续发送消息；是否允许新建会话由前端重新调用创建接口 |

### 5.2 工单状态

| 状态 | 说明 | 对外可见 |
| --- | --- | --- |
| `NEW` | 工单已写入但尚未完成队列分派的短暂状态 | 是，通常只在异步分派窗口出现 |
| `PENDING_ACCEPTANCE` | 已进入客服队列，等待客服受理 | 是 |
| `IN_PROGRESS` | 客服已受理并正在处理 | 是 |
| `PENDING_USER_CONFIRM` | 客服已提交解决结果，等待用户确认 | 是 |
| `RESOLVED` | 用户确认解决或超时自动确认 | 是 |
| `CLOSED` | 工单生命周期结束 | 是 |
| `REOPENED` | 用户认为未解决，要求继续处理 | 是 |

旧资料中的 `SUBMITTED`、`AGENT_PROCESSING`、`AGENT_ANSWERED`、`ACCEPTED`、`TECH_ANALYSIS`、`IN_SUPPORT` 不作为本版公开工单状态；这些旧值不能被前端或后端新增实现继续使用。

### 5.3 状态流转表

| 当前状态 | 目标状态 | 触发接口/触发方 | 前置条件 |
| --- | --- | --- | --- |
| `NEW` | `PENDING_ACCEPTANCE` | 工单创建事务/系统分派 | 工单字段和租户校验通过 |
| `PENDING_ACCEPTANCE` | `IN_PROGRESS` | 客服受理 | 客服有数据权限且未被其他客服抢占 |
| `REOPENED` | `IN_PROGRESS` | 客服受理 | 客服有数据权限 |
| `IN_PROGRESS` | `IN_PROGRESS` | 分类/原因/解决方法更新 | 仅更新处理字段，不改变状态 |
| `IN_PROGRESS` | `PENDING_USER_CONFIRM` | 客服提交解决 | 分类、原因、解决方法和解决说明满足必填 |
| `PENDING_USER_CONFIRM` | `RESOLVED` | 用户确认或系统超时任务 | 用户是请求人，或系统任务执行 |
| `PENDING_USER_CONFIRM` | `REOPENED` | 用户重开 | 用户明确表示未解决 |
| `RESOLVED` | `CLOSED` | 客服关闭或系统关闭任务 | 解决结果已存在；评价策略满足当前配置 |
| `RESOLVED` | `REOPENED` | 用户重开 | 用户是请求人 |
| `CLOSED` | `REOPENED` | 用户重开 | 用户是请求人；一期暂不限制重开时间窗口 |
| 其他 | 不变 | 任意接口 | 返回 `ILLEGAL_STATE_TRANSITION` |

每次状态变化必须同时写入状态历史和审计日志。状态变更与业务字段变更必须在同一事务中提交；领域事件使用 Outbox 或等价可靠发布机制，不能出现数据库已变更但事件完全丢失的情况。

## 6. 接口清单

| 接口编号 | 接口名称 | 方法 | 路径 | 所属服务 | 主要调用方 |
| --- | --- | --- | --- | --- | --- |
| `ITSM-AUTH-001` | 企业身份登录 | POST | `/api/v1/auth/login` | `auth-service` | 用户、客服、管理员前端 |
| `ITSM-AUTH-002` | 查询当前登录用户 | GET | `/api/v1/auth/me` | `auth-service` | 前端启动、刷新登录态 |
| `ITSM-SESSION-001` | 创建会话 | POST | `/api/v1/conversations/sessions` | `conversation-service` | 用户前端 |
| `ITSM-SESSION-002` | 读取会话 | GET | `/api/v1/conversations/sessions/{sessionId}` | `conversation-service` | 用户前端、客服详情页 |
| `ITSM-CHAT-001` | 发送用户消息 | POST | `/api/v1/conversations/sessions/{sessionId}/messages` | `conversation-service` | 用户前端 |
| `ITSM-AGENT-001` | Agent 自助解决或转人工决策 | POST | `/api/v1/agent/sessions/{sessionId}/decisions` | `agent-orchestrator` | Agent 服务 |
| `ITSM-AGENT-002` | 用户明确请求转人工 | POST | `/api/v1/conversations/sessions/{sessionId}/handoff` | `conversation-service` | 用户前端 |
| `ITSM-TICKET-001` | 创建工单 | POST | `/api/v1/tickets` | `ticket-service` | 用户前端、Agent 编排服务 |
| `ITSM-TICKET-002` | 工单分页查询 | GET | `/api/v1/tickets` | `ticket-service` | 用户、客服、管理员前端 |
| `ITSM-TICKET-003` | 工单详情查询 | GET | `/api/v1/tickets/{ticketId}` | `ticket-service` | 用户、客服、管理员前端 |
| `ITSM-SUPPORT-001` | 客服队列查询 | GET | `/api/v1/support/tickets` | `ticket-service` | 客服前端 |
| `ITSM-SUPPORT-002` | 客服受理工单 | POST | `/api/v1/support/tickets/{ticketId}/accept` | `ticket-service` | 客服前端 |
| `ITSM-SUPPORT-003` | 更新分类/原因/解决方法 | PATCH | `/api/v1/support/tickets/{ticketId}/classification` | `ticket-service` | 客服前端 |
| `ITSM-SUPPORT-004` | 提交工单解决结果 | POST | `/api/v1/support/tickets/{ticketId}/resolve` | `ticket-service` | 客服前端 |
| `ITSM-TICKET-004` | 用户确认解决 | POST | `/api/v1/tickets/{ticketId}/confirm` | `ticket-service` | 用户前端 |
| `ITSM-SUPPORT-005` | 客服关闭工单 | POST | `/api/v1/support/tickets/{ticketId}/close` | `ticket-service` | 客服前端、系统关闭任务 |
| `ITSM-TICKET-005` | 用户重开工单 | POST | `/api/v1/tickets/{ticketId}/reopen` | `ticket-service` | 用户前端 |
| `ITSM-RATING-001` | 用户评价工单 | POST | `/api/v1/tickets/{ticketId}/rating` | `rating-service` | 用户前端 |
| `ITSM-DICT-001` | 查询字典项 | GET | `/api/v1/admin/dictionaries/{dictType}/items` | `dictionary-service` | 客服、管理员前端 |
| `ITSM-DICT-002` | 新增字典项 | POST | `/api/v1/admin/dictionaries/{dictType}/items` | `dictionary-service` | 管理员前端 |
| `ITSM-DICT-003` | 更新字典项 | PATCH | `/api/v1/admin/dictionaries/items/{itemId}` | `dictionary-service` | 管理员前端 |
| `ITSM-DICT-004` | 停用字典项 | POST | `/api/v1/admin/dictionaries/items/{itemId}/disable` | `dictionary-service` | 管理员前端 |
| `ITSM-PERM-001` | 查询当前用户角色和权限 | GET | `/api/v1/permissions/me` | `permission-service` | 所有前端 |
| `ITSM-PERM-002` | 查询角色列表 | GET | `/api/v1/admin/roles` | `permission-service` | 管理员前端 |
| `ITSM-PERM-003` | 查询角色权限 | GET | `/api/v1/admin/roles/{roleId}/permissions` | `permission-service` | 管理员前端 |

## 7. 接口详细定义

### ITSM-AUTH-001 企业身份登录

#### 基本信息

| 项目 | 约定 |
| --- | --- |
| 目标 | 将企业统一身份凭证交换为 ITSM 访问令牌，返回用户、租户、角色和权限摘要 |
| 所属服务 | `auth-service` |
| 调用方 | 登录页或企业工作台跳转后的前端 |
| HTTP/路径 | `POST /api/v1/auth/login` |
| Content-Type | `application/json; charset=utf-8` |
| 鉴权 | 无 Authorization；通过网关或 SSO 回调凭证校验 |
| 租户隔离 | `tenantKey` 由 SSO 结果解析；如果请求携带 `X-Tenant-Id`，必须与解析结果一致 |
| 角色权限 | 未登录用户可调用；登录成功后由令牌携带角色和权限 |
| 幂等 | `Idempotency-Key` 可选；同一 SSO code 重放必须返回 `TOKEN_INVALID` 或已建立会话结果，不得重复创建身份 |

#### 请求

Header：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `X-Tenant-Id` | String | `[待确认]` | 多租户 SSO 场景建议传入；单租户可省略 |
| `X-Trace-Id` | String | 否 | 链路 ID |

Body：

```json
{
  "grantType": "SSO_CODE",
  "ssoCode": "one-time-code",
  "redirectUri": "https://itsm.example.com/login/callback"
}
```

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `grantType` | String | 是 | 一期固定 `SSO_CODE` |
| `ssoCode` | String | 是 | 企业统一身份平台一次性授权码 |
| `redirectUri` | String | 否 | 与 SSO 注册配置匹配 |

#### 成功响应

```json
{
  "code": "SUCCESS",
  "message": "success",
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "refresh_...",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "user": {
      "userId": "usr_10001",
      "displayName": "张三",
      "departmentName": "技术支持部"
    },
    "tenant": {
      "tenantId": "tenant_001",
      "tenantName": "示例企业"
    },
    "roles": ["USER"],
    "permissionsVersion": "perm_20260825_01"
  },
  "traceId": "trc_login_001",
  "details": null
}
```

失败示例：`TOKEN_INVALID`，HTTP 401，`data=null`。

#### 状态、副作用与事件

- 状态：建立登录会话；不改变会话或工单状态。
- 副作用：写登录审计日志；不得记录原始 `ssoCode` 和完整令牌。
- 领域事件：`UserAuthenticated`。
- 通知：无。

#### 前端交互约束

- 登录按钮提交期间只能有一个请求。
- 成功后保存令牌和租户上下文，并调用 `ITSM-PERM-001` 获取最新权限。
- `TOKEN_INVALID`、`AUTH_REQUIRED` 不展示技术细节，回到登录页。
- 不在前端自行解析或修改角色权限。

#### 后端实现边界

- `auth-service` 负责 SSO code 校验、用户映射、租户映射、令牌签发和审计。
- 不在登录接口中创建会话或工单。
- SSO 供应商、字段映射和刷新令牌轮换策略为 `[待确认]`，但不得改变本接口响应结构。

#### 测试与验收

- 必测：有效 code、重复 code、过期 code、错误 redirectUri、租户不匹配、SSO 用户无 ITSM 账号。
- 验收：登录成功返回可用于后续接口的 Bearer token、用户和租户信息；失败均符合统一错误结构。

### ITSM-AUTH-002 查询当前登录用户

| 项目 | 约定 |
| --- | --- |
| 目标 | 前端刷新后获取当前用户、租户、角色和权限版本 |
| 所属服务/调用方 | `auth-service` / 所有前端 |
| HTTP/路径 | `GET /api/v1/auth/me` |
| Content-Type | 响应 `application/json`；无请求 Body |
| 鉴权 | `Authorization` 必填 |
| 租户与权限 | 只能返回当前令牌对应租户；从令牌解析用户，不接受用户 ID |
| 幂等 | 是 |

Header：`Authorization`、`X-Tenant-Id`、`X-Trace-Id`。

成功 `data`：

```json
{
  "userId": "usr_10001",
  "displayName": "张三",
  "departmentName": "技术支持部",
  "tenantId": "tenant_001",
  "roles": ["USER"],
  "permissionsVersion": "perm_20260825_01"
}
```

失败示例：缺少 Authorization 返回 `AUTH_REQUIRED`，HTTP 401。

- 状态/副作用/事件：不改变业务状态；可记录访问审计，不发布领域事件。
- 前端：启动和刷新登录态时调用；权限变化以本接口和 `ITSM-PERM-001` 返回为准。
- 后端：不从 Query、Body 或不可信 Header 覆盖令牌身份。
- 测试/验收：覆盖过期令牌、租户不匹配、权限版本返回和多角色用户。

### ITSM-SESSION-001 创建会话

| 项目 | 约定 |
| --- | --- |
| 目标 | 为用户创建聊天式咨询会话，作为消息和 Agent 上下文容器 |
| 所属服务/调用方 | `conversation-service` / `USER` 前端 |
| HTTP/路径 | `POST /api/v1/conversations/sessions` |
| Content-Type | `application/json; charset=utf-8` |
| 鉴权 | `USER` |
| 租户隔离 | 会话归属当前令牌用户和当前租户 |
| 角色权限 | `USER`；客服不代替用户创建会话 |
| 幂等 | `Idempotency-Key` 必填；同一用户可通过幂等键避免重复会话 |

Body：

```json
{
  "channel": "WORKBENCH",
  "subject": "办公软件使用咨询"
}
```

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `channel` | String | 是 | 一期固定 `WORKBENCH` |
| `subject` | String | 否 | 会话主题，最长 100 字 |

成功 `data`：

```json
{
  "sessionId": "ses_01J8V9X6Q7",
  "status": "ACTIVE",
  "ticketId": null,
  "createdAt": "2026-08-25T08:10:00Z",
  "lastMessageAt": null
}
```

失败示例：请求体缺少 `channel` 返回 `VALIDATION_ERROR`，HTTP 400。

- 状态：无活动会话时创建 `ACTIVE`；同一用户同租户已有可复用活动会话时，服务端可返回原会话。
- 副作用：写会话审计；不创建工单。
- 领域事件：`ConversationCreated`。
- 前端：进入“我的助手”时先获取或创建会话；创建按钮和网络重试必须复用同一幂等键。
- 后端：会话必须保存 `tenantId`、`userId`、`channel`、`status`、摘要和最后消息时间；不得把消息内容只放在缓存。
- 测试/验收：重复提交不产生多个会话；跨租户 token 不可读取或复用会话。

### ITSM-SESSION-002 读取会话

| 项目 | 约定 |
| --- | --- |
| 目标 | 返回会话基本信息、消息列表、关联工单和当前处理摘要 |
| 所属服务/调用方 | `conversation-service` / 用户前端、客服工单详情页 |
| HTTP/路径 | `GET /api/v1/conversations/sessions/{sessionId}` |
| Content-Type | 响应 `application/json` |
| 鉴权 | `USER` 只能读本人；`SUPPORT_AGENT`/`SUPPORT_ADMIN` 只能通过有权限的关联工单读取 |
| 租户隔离 | Path 会话必须属于当前租户 |
| 幂等 | 是 |

Path：`sessionId`，String，必填。

Query：

| 参数 | 类型 | 必填 | 默认 | 说明 |
| --- | --- | --- | --- | --- |
| `messagePage` | Integer | 否 | 1 | 消息页码 |
| `messagePageSize` | Integer | 否 | 50 | 最大 100 |

成功 `data`：

```json
{
  "sessionId": "ses_01J8V9X6Q7",
  "status": "TICKET_CREATED",
  "ticketId": "tkt_01J8V9X8AB",
  "summary": "用户反馈办公软件无法打开",
  "messages": {
    "items": [
      {
        "messageId": "msg_001",
        "senderType": "USER",
        "content": "办公软件打不开",
        "createdAt": "2026-08-25T08:11:00Z"
      }
    ],
    "page": 1,
    "pageSize": 50,
    "total": 1,
    "hasNext": false
  }
}
```

失败示例：资源不属于当前租户返回 `RESOURCE_NOT_FOUND` 或 `TENANT_FORBIDDEN`，不得泄露存在性。

- 状态/副作用/事件：只读，不改变会话和工单状态。
- 前端：加载详情失败时保留当前页面，不清空已展示的旧数据；分页加载消息不能重复追加。
- 后端：消息按创建时间升序返回；敏感字段按角色脱敏。
- 测试/验收：覆盖用户越权、客服无关联工单读取、消息分页边界、空会话。

### ITSM-CHAT-001 发送用户消息

| 项目 | 约定 |
| --- | --- |
| 目标 | 保存用户消息，调用 Agent 首轮接待，并返回 Agent 自助回答或人工转接结果 |
| 所属服务/调用方 | `conversation-service` + `agent-orchestrator` / `USER` 前端 |
| HTTP/路径 | `POST /api/v1/conversations/sessions/{sessionId}/messages` |
| Content-Type | `application/json; charset=utf-8` |
| 鉴权 | `USER`，只能发送到本人 `ACTIVE` 会话 |
| 租户隔离 | 会话租户和令牌租户必须一致 |
| 幂等 | `Idempotency-Key` 必填；重复请求返回首次消息及 Agent 结果 |

Body：

```json
{
  "clientMessageId": "cm_20260825_001",
  "content": "我的 Windows 页面提示无法登录",
  "attachments": [
    {
      "fileId": "file_001",
      "fileName": "error.png"
    }
  ]
}
```

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `clientMessageId` | String | 是 | 前端消息唯一 ID |
| `content` | String | 是 | 1-5000 字 |
| `attachments` | Array | 否 | 只能引用已上传文件 ID |

成功 `data`：

```json
{
  "messageId": "msg_002",
  "sessionId": "ses_01J8V9X6Q7",
  "outcome": "SELF_RESOLVED",
  "agentMessage": {
    "messageId": "msg_003",
    "content": "请先确认账号状态并重新连接企业网络。",
    "confidence": 0.86,
    "sourceSummary": "IT支持/账号登录"
  },
  "ticketId": null,
  "sessionStatus": "ACTIVE"
}
```

`outcome` 取值：

- `SELF_RESOLVED`：Agent 给出可执行答复，暂不创建工单。
- `HANDOFF_PENDING`：Agent 或用户要求人工，正在创建/分派工单。
- `TICKET_CREATED`：本次请求已创建工单。
- `AGENT_PROCESSING`：Agent 异步处理，前端等待后续消息；该值仅表示会话结果，不是工单状态。

失败示例：Agent 超时返回 `AGENT_UNAVAILABLE`，HTTP 503；用户消息必须已经持久化，前端显示转人工或重试入口。

- 状态：`ACTIVE` 会话允许发送；`ENDED` 返回 `ILLEGAL_STATE_TRANSITION`。
- 副作用：写用户消息、Agent 消息或 Agent 处理任务；可能创建工单。
- 领域事件：`UserMessageSent`、`AgentResponseGenerated`、`HandoffRequested`、`TicketCreated`。
- 前端：发送按钮防重复；消息采用客户端临时 ID 去重；`AGENT_PROCESSING` 不允许重复发送同一 `clientMessageId`。
- 后端：先落用户消息再调用 Agent；Agent 失败不得丢消息；不得把 Agent 返回直接当作工单事实状态。
- 测试/验收：覆盖自助回答、低置信度/明确转人工、Agent 超时、附件引用非法、重复消息和结束会话。

### ITSM-AGENT-001 Agent 自助解决或转人工决策

| 项目 | 约定 |
| --- | --- |
| 目标 | 接收 Agent 对会话的结构化决策；自助回答继续会话，转人工时由工单服务创建工单 |
| 所属服务/调用方 | `agent-orchestrator` / 内部 Agent 服务 |
| HTTP/路径 | `POST /api/v1/agent/sessions/{sessionId}/decisions` |
| Content-Type | `application/json; charset=utf-8` |
| 鉴权 | `AGENT_SERVICE` 服务凭证；禁止用户和客服前端调用 |
| 租户隔离 | 服务凭证租户、会话租户必须一致 |
| 角色权限 | 仅 `AGENT_SERVICE`；Agent 不拥有 `ticket:status:update` 权限 |
| 幂等 | `Idempotency-Key` 必填；同一个 Agent 任务只能生效一次 |

Body：

```json
{
  "decision": "HANDOFF",
  "answer": null,
  "confidence": 0.42,
  "businessLineCode": "IT_SUPPORT",
  "summary": "用户无法登录办公系统，已提供错误截图",
  "handoffReason": "置信度不足，需要人工排查账号权限",
  "suggestedManagementUnitId": "dict_unit_account",
  "suggestedSymptomId": "dict_symptom_login_failed"
}
```

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `decision` | String | 是 | `SELF_RESOLVED` 或 `HANDOFF` |
| `answer` | String | `SELF_RESOLVED` 时是 | 返回给用户的答复 |
| `confidence` | Number | 是 | `0-1`；阈值由 Agent 内部策略决定 |
| `businessLineCode` | String | `HANDOFF` 时是 | 分流业务线 |
| `summary` | String | `HANDOFF` 时是 | 交给客服的会话摘要 |
| `handoffReason` | String | `HANDOFF` 时是 | 转人工原因 |
| `suggestedManagementUnitId` | String | 否 | Agent 建议，不是最终分类 |
| `suggestedSymptomId` | String | 否 | Agent 建议，不是最终分类 |

成功 `data`：

```json
{
  "sessionId": "ses_01J8V9X6Q7",
  "decision": "HANDOFF",
  "sessionStatus": "TICKET_CREATED",
  "ticketId": "tkt_01J8V9X8AB",
  "ticketStatus": "PENDING_ACCEPTANCE",
  "acceptedAsFact": false
}
```

失败示例：普通用户调用返回 `ROLE_FORBIDDEN`，HTTP 403。

- 状态：`ACTIVE -> ACTIVE`（自助解决）；`ACTIVE -> HANDOFF_PENDING -> TICKET_CREATED`（转人工）。
- 副作用：保存 Agent 原始摘要、置信度、建议字段；转人工时创建工单并分派队列。
- 领域事件：`AgentDecisionRecorded`、`TicketCreated`、`TicketRouted`。
- 前端：不直接调用本接口；仅根据 `ITSM-CHAT-001` 的结果渲染。
- 后端：Agent 不能直接修改工单状态、分配人或关闭工单；工单服务负责事实校验和状态变更。
- 测试/验收：覆盖自助解决、转人工、重复决策、跨租户服务凭证、Agent 伪造角色、分流业务线不存在。

### ITSM-AGENT-002 用户明确请求转人工

| 项目 | 约定 |
| --- | --- |
| 目标 | 用户无需等待 Agent 判断时，明确请求人工并保留完整会话上下文 |
| 所属服务/调用方 | `conversation-service` / `USER` 前端 |
| HTTP/路径 | `POST /api/v1/conversations/sessions/{sessionId}/handoff` |
| Content-Type | `application/json; charset=utf-8` |
| 鉴权/租户 | `USER`；本人会话、本租户 |
| 幂等 | `Idempotency-Key` 必填 |

Body：

```json
{
  "reason": "我希望人工协助处理",
  "businessLineCode": "IT_SUPPORT"
}
```

成功 `data`：

```json
{
  "sessionId": "ses_01J8V9X6Q7",
  "sessionStatus": "TICKET_CREATED",
  "ticketId": "tkt_01J8V9X8AB",
  "ticketStatus": "PENDING_ACCEPTANCE"
}
```

失败示例：会话已结束返回 `ILLEGAL_STATE_TRANSITION`，HTTP 409。

- 状态：`ACTIVE -> HANDOFF_PENDING -> TICKET_CREATED`。
- 副作用：创建工单，复制会话摘要、消息引用和转人工原因；发布 `HandoffRequested`、`TicketCreated`、`TicketRouted`。
- 前端：点击后显示处理中；成功后展示工单号和当前状态，禁止重复创建。
- 后端：服务端从会话拼装摘要，不能信任前端传入的用户身份；业务线为空时按分流规则进入待分流队列。
- 测试/验收：覆盖明确转人工、已有工单再次转人工、重复请求、业务线非法、会话跨租户。

### ITSM-TICKET-001 创建工单

| 项目 | 约定 |
| --- | --- |
| 目标 | 创建用户工单并进入客服队列；支持用户手动建单和 Agent/会话转人工建单 |
| 所属服务/调用方 | `ticket-service` / `USER` 前端、`AGENT_SERVICE` |
| HTTP/路径 | `POST /api/v1/tickets` |
| Content-Type | `application/json; charset=utf-8` |
| 鉴权 | 用户创建只能是 `USER`；系统创建使用 `AGENT_SERVICE` |
| 租户隔离 | requester 只能取令牌身份；Agent 创建必须使用会话用户 |
| 幂等 | `Idempotency-Key` 必填 |

Body：

```json
{
  "source": "USER_MANUAL",
  "sessionId": "ses_01J8V9X6Q7",
  "title": "办公系统无法登录",
  "description": "进入办公系统后提示账号无权限",
  "businessLineCode": "IT_SUPPORT",
  "priority": "MEDIUM",
  "environment": "Windows 11",
  "attachments": ["file_001"]
}
```

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `source` | String | 是 | `USER_MANUAL` 或 `AGENT_HANDOFF` |
| `sessionId` | String | `AGENT_HANDOFF` 时是 | 关联会话 |
| `title` | String | 是 | 1-200 字 |
| `description` | String | 是 | 1-10000 字 |
| `businessLineCode` | String | 否 | 不填则进入待分流队列 |
| `priority` | String | 是 | `LOW`、`MEDIUM`、`HIGH` |
| `environment` | String | 否 | 环境信息 |
| `attachments` | Array[String] | 否 | 已上传文件 ID |

成功 `data`：

```json
{
  "ticketId": "tkt_01J8V9X8AB",
  "ticketNo": "3053001",
  "status": "PENDING_ACCEPTANCE",
  "businessLineCode": "IT_SUPPORT",
  "requesterId": "usr_10001",
  "sessionId": "ses_01J8V9X6Q7",
  "createdAt": "2026-08-25T08:16:38Z"
}
```

失败示例：会话不属于当前用户或租户返回 `DATA_SCOPE_FORBIDDEN`，HTTP 403。

- 状态：事务内 `NEW -> PENDING_ACCEPTANCE`；若分派事件暂未完成，可短暂返回 `NEW`，后续由事件重试进入队列。
- 副作用：创建工单、初始状态历史、审计日志；关联会话；生成分派任务。
- 领域事件：`TicketCreated`、`TicketRouted`。
- 前端：提交成功后跳转工单详情；不自行生成工单号和状态。
- 后端：工单是事实源；requester、tenant、createdBy 从认证上下文取得；必须保存创建来源和会话引用。
- 测试/验收：覆盖手动建单、Agent 建单、缺标题、优先级非法、重复提交、会话不匹配、队列分派失败重试。

### ITSM-TICKET-002 工单分页查询

| 项目 | 约定 |
| --- | --- |
| 目标 | 用户查询本人工单，客服/管理员查询其数据权限范围内工单 |
| 所属服务/调用方 | `ticket-service` / 用户、客服、管理员前端 |
| HTTP/路径 | `GET /api/v1/tickets` |
| Content-Type | 响应 `application/json` |
| 鉴权 | `USER`、`SUPPORT_AGENT`、`SUPPORT_ADMIN`、`SUPERVISOR` |
| 租户隔离 | 必须按当前租户过滤 |
| 幂等 | 是 |

Query：

| 参数 | 类型 | 必填 | 默认 | 说明 |
| --- | --- | --- | --- | --- |
| `page` | Integer | 否 | 1 | 大于等于 1 |
| `pageSize` | Integer | 否 | 20 | 1-100 |
| `status` | String | 否 | 空 | 可多次传入 |
| `ticketNo` | String | 否 | 空 | 精确或前缀查询 |
| `requesterId` | String | 否 | 空 | 仅客服/管理员可用；用户传入他人 ID 必须拒绝 |
| `businessLineCode` | String | 否 | 空 | 业务线过滤 |
| `keyword` | String | 否 | 空 | 标题和工单号检索 |
| `sort` | String | 否 | `updatedAt:desc` | 只允许白名单字段 |

成功 `data`：

```json
{
  "items": [
    {
      "ticketId": "tkt_01J8V9X8AB",
      "ticketNo": "3053001",
      "title": "办公系统无法登录",
      "status": "PENDING_ACCEPTANCE",
      "priority": "MEDIUM",
      "businessLineCode": "IT_SUPPORT",
      "assigneeId": null,
      "updatedAt": "2026-08-25T08:16:38Z"
    }
  ],
  "page": 1,
  "pageSize": 20,
  "total": 1,
  "hasNext": false
}
```

失败示例：`pageSize=1000` 返回 `VALIDATION_ERROR`，HTTP 400。

- 状态/副作用/事件：只读，不发布事件。
- 前端：分页、筛选和排序变更时重置到第 1 页；列表请求过期结果不得覆盖较新查询。
- 后端：用户默认只能查询本人；客服按数据权限过滤；不能以 `requesterId` 绕过权限。
- 测试/验收：覆盖空数据、分页边界、状态多选、用户越权、跨租户、排序白名单和大页数。

### ITSM-TICKET-003 工单详情查询

| 项目 | 约定 |
| --- | --- |
| 目标 | 返回工单核心字段、会话摘要、消息引用、分类、状态历史、审计摘要和评价 |
| 所属服务/调用方 | `ticket-service` / 用户、客服、管理员前端 |
| HTTP/路径 | `GET /api/v1/tickets/{ticketId}` |
| Content-Type | 响应 `application/json` |
| 鉴权/权限 | 用户本人；客服数据范围；管理员全租户；主管只读 |
| 租户隔离 | Path 工单必须属于当前租户 |
| 幂等 | 是 |

Path：`ticketId`，String，必填。

Query：

| 参数 | 类型 | 默认 | 说明 |
| --- | --- | --- | --- |
| `includeMessages` | Boolean | `true` | 是否返回会话消息摘要 |
| `includeAudit` | Boolean | `false` | 仅客服/管理员/主管可设为 true |

成功 `data`：

```json
{
  "ticketId": "tkt_01J8V9X8AB",
  "ticketNo": "3053001",
  "tenantId": "tenant_001",
  "requester": {
    "userId": "usr_10001",
    "displayName": "张三",
    "departmentName": "技术支持部"
  },
  "title": "办公系统无法登录",
  "description": "进入办公系统后提示账号无权限",
  "source": "AGENT_HANDOFF",
  "status": "IN_PROGRESS",
  "priority": "MEDIUM",
  "businessLineCode": "IT_SUPPORT",
  "classification": {
    "managementUnitId": "dict_unit_account",
    "symptomId": "dict_symptom_login_failed",
    "reasonId": null,
    "solutionMethodId": null,
    "customReason": null,
    "customSolution": null
  },
  "assignee": {
    "userId": "usr_support_01",
    "displayName": "客服一"
  },
  "conversation": {
    "sessionId": "ses_01J8V9X6Q7",
    "summary": "用户无法登录办公系统，已提供错误截图",
    "messageCount": 4
  },
  "statusHistory": [],
  "auditEvents": [],
  "rating": null,
  "createdAt": "2026-08-25T08:16:38Z",
  "updatedAt": "2026-08-25T08:20:10Z"
}
```

失败示例：工单不存在返回 `RESOURCE_NOT_FOUND`，HTTP 404。

- 状态/副作用/事件：只读。
- 前端：详情页以 `status` 决定按钮；不根据接口返回的缺失字段猜测状态。
- 后端：按角色脱敏用户联系方式、内部审计和 Agent 原始信息。
- 测试/验收：覆盖各角色可见字段、跨租户、用户查看他人工单、审计开关和消息摘要。

### ITSM-SUPPORT-001 客服队列查询

| 项目 | 约定 |
| --- | --- |
| 目标 | 提供客服待受理、处理中、待确认和历史工单视图 |
| 所属服务/调用方 | `ticket-service` / `SUPPORT_AGENT`、`SUPPORT_ADMIN` 前端 |
| HTTP/路径 | `GET /api/v1/support/tickets` |
| Content-Type | 响应 `application/json` |
| 鉴权 | `SUPPORT_AGENT`、`SUPPORT_ADMIN`、可选 `SUPERVISOR` 只读 |
| 租户隔离 | 当前租户 + 角色数据范围 |
| 幂等 | 是 |

Query：

| 参数 | 类型 | 默认 | 说明 |
| --- | --- | --- | --- |
| `view` | String | `PENDING` | `PENDING`、`IN_PROGRESS`、`PENDING_CONFIRM`、`HISTORY` |
| `page` | Integer | 1 | 页码 |
| `pageSize` | Integer | 20 | 最大 100 |
| `businessLineCode` | String | 空 | 管理员可按业务线筛选 |
| `assignee` | String | 空 | `ME` 或用户 ID；普通客服只允许 `ME` |
| `keyword` | String | 空 | 工单号、标题、请求人检索 |

成功 `data` 为统一分页结构，列表项至少包括 `ticketId`、`ticketNo`、`title`、`status`、`priority`、`businessLineCode`、`requester`、`assignee`、`updatedAt`。

失败示例：普通用户调用返回 `ROLE_FORBIDDEN`，HTTP 403。

- 状态/副作用/事件：只读。
- 前端：支持截图对应的“待受理/处理中/历史”视图；点击受理前先刷新详情或使用接口冲突处理。
- 后端：普通客服只能看到被分配或所属业务线工单；管理员按数据权限可跨业务线。
- 测试/验收：覆盖非客服、业务线隔离、`ME` 过滤、空队列、并发受理前列表刷新。

### ITSM-SUPPORT-002 客服受理工单

| 项目 | 约定 |
| --- | --- |
| 目标 | 客服抢占或接收待处理工单，成为当前处理人 |
| 所属服务/调用方 | `ticket-service` / `SUPPORT_AGENT`、`SUPPORT_ADMIN` |
| HTTP/路径 | `POST /api/v1/support/tickets/{ticketId}/accept` |
| Content-Type | `application/json; charset=utf-8` |
| 鉴权 | `ticket:accept` |
| 租户/数据权限 | 当前租户；工单属于客服可处理业务线或已分配范围 |
| 幂等 | `Idempotency-Key` 必填 |

Body：

```json
{
  "note": "开始排查用户登录权限"
}
```

成功 `data`：返回工单详情摘要，状态为 `IN_PROGRESS`，`assigneeId` 为令牌当前用户。

失败示例：工单已被其他客服受理返回 `RESOURCE_CONFLICT`，HTTP 409。

- 状态：`PENDING_ACCEPTANCE -> IN_PROGRESS`；`REOPENED -> IN_PROGRESS`。
- 副作用：写当前处理人、状态历史、客服受理动作和审计日志。
- 领域事件：`TicketAccepted`。
- 前端：受理按钮只在 `PENDING_ACCEPTANCE` 或允许的 `REOPENED` 展示；冲突后刷新详情。
- 后端：使用事务锁或版本号防止双人同时受理；操作者从令牌取得。
- 测试/验收：覆盖正常受理、重复受理、并发抢单、无数据权限、非法状态和跨租户。

### ITSM-SUPPORT-003 更新分类/原因/解决方法

| 项目 | 约定 |
| --- | --- |
| 目标 | 客服按标准字典补齐工单管理单元、症状、原因和解决方法 |
| 所属服务/调用方 | `ticket-service` / `SUPPORT_AGENT`、`SUPPORT_ADMIN` |
| HTTP/路径 | `PATCH /api/v1/support/tickets/{ticketId}/classification` |
| Content-Type | `application/json; charset=utf-8` |
| 鉴权 | `ticket:classify` |
| 租户/数据权限 | 当前租户，且客服是处理人或具有管理员覆盖权限 |
| 幂等 | `Idempotency-Key` 必填 |

Body：

```json
{
  "managementUnitId": "dict_unit_account",
  "symptomId": "dict_symptom_login_failed",
  "reasonId": "dict_reason_permission",
  "solutionMethodId": "dict_solution_reset_permission",
  "customReason": null,
  "customSolution": "调整账号权限后重新登录"
}
```

规则：

- `managementUnitId`、`symptomId`、`reasonId` 至少在提交解决前完成。
- `solutionMethodId` 和 `customSolution` 至少填写一个。
- `customReason` 仅在没有合适标准原因时使用。
- 所有字典 ID 必须属于当前租户、类型正确且处于启用状态。

成功 `data`：返回更新后的 `classification`、`updatedAt`、`version`。

失败示例：引用停用字典项返回 `DICTIONARY_ITEM_DISABLED`，HTTP 409。

- 状态：状态不变，仅允许 `IN_PROGRESS` 或 `REOPENED` 更新。
- 副作用：写分类变更历史和审计日志；不改变会话消息。
- 领域事件：`TicketClassificationUpdated`。
- 前端：字段选项必须来自 `ITSM-DICT-001`；保存中禁用重复提交；未完成必填项时不得提交解决。
- 后端：校验字典层级、租户和启用状态；不能把前端显示名作为事实值。
- 测试/验收：覆盖完整更新、部分更新、字典类型错误、停用项、无权限、非法状态、并发版本冲突。

### ITSM-SUPPORT-004 提交工单解决结果

| 项目 | 约定 |
| --- | --- |
| 目标 | 客服记录处理过程和解决方案，将工单交给用户确认 |
| 所属服务/调用方 | `ticket-service` / `SUPPORT_AGENT`、`SUPPORT_ADMIN` |
| HTTP/路径 | `POST /api/v1/support/tickets/{ticketId}/resolve` |
| Content-Type | `application/json; charset=utf-8` |
| 鉴权 | `ticket:resolve` |
| 租户/数据权限 | 当前租户；处理人或管理员 |
| 幂等 | `Idempotency-Key` 必填 |

Body：

```json
{
  "resolution": "已补充账号权限并指导用户重新登录，问题恢复。",
  "resolutionType": "STANDARD_METHOD",
  "solutionMethodId": "dict_solution_reset_permission",
  "customSolution": null
}
```

成功 `data`：状态为 `PENDING_USER_CONFIRM`，返回 `resolvedBy`、`resolvedAt`、`resolution` 和分类摘要。

失败示例：分类字段不完整返回 `VALIDATION_ERROR`，HTTP 400，`details.missingFields` 列出缺失项。

- 状态：`IN_PROGRESS -> PENDING_USER_CONFIRM`。
- 副作用：保存解决说明、处理人和处理时间；写状态历史和审计；向用户发送待确认通知。
- 领域事件：`TicketResolutionSubmitted`、`UserConfirmationRequired`。
- 前端：提交解决前必须展示分类/原因/解决方法缺失提示；成功后切换为待用户确认。
- 后端：在同一事务校验必填字段和状态；不能因为 Agent 建议字段存在而绕过客服确认。
- 测试/验收：覆盖完整人工链路、必填校验、重复提交、非法状态、通知失败重试和审计记录。

### ITSM-TICKET-004 用户确认解决

| 项目 | 约定 |
| --- | --- |
| 目标 | 用户确认客服处理结果，完成工单解决节点 |
| 所属服务/调用方 | `ticket-service` / `USER` |
| HTTP/路径 | `POST /api/v1/tickets/{ticketId}/confirm` |
| Content-Type | `application/json; charset=utf-8`；无 Body |
| 鉴权 | `ticket:confirm` |
| 租户/数据权限 | 当前租户、本人工单请求人 |
| 幂等 | `Idempotency-Key` 必填 |

成功 `data`：工单状态为 `RESOLVED`，返回 `resolvedAt` 和可评价标记 `ratingAllowed=true`。

失败示例：非请求人调用返回 `DATA_SCOPE_FORBIDDEN`，HTTP 403。

- 状态：`PENDING_USER_CONFIRM -> RESOLVED`。
- 副作用：写用户确认动作和审计；向客服发送确认通知。
- 领域事件：`TicketUserConfirmed`、`TicketResolved`。
- 前端：确认前展示解决说明；确认成功后展示评价入口；不得自行把状态改成 RESOLVED。
- 后端：只接受当前请求人；重复确认返回第一次结果或 `ILLEGAL_STATE_TRANSITION`，必须保持幂等。
- 测试/验收：覆盖正常确认、非本人、重复确认、状态不符、通知失败重试。

### ITSM-SUPPORT-005 客服关闭工单

| 项目 | 约定 |
| --- | --- |
| 目标 | 在工单已解决后完成生命周期关闭 |
| 所属服务/调用方 | `ticket-service` / `SUPPORT_AGENT`、`SUPPORT_ADMIN`、系统关闭任务 |
| HTTP/路径 | `POST /api/v1/support/tickets/{ticketId}/close` |
| Content-Type | `application/json; charset=utf-8` |
| 鉴权 | `ticket:close`；系统任务使用内部任务凭证 |
| 租户/数据权限 | 当前租户；处理人、管理员或系统任务 |
| 幂等 | `Idempotency-Key` 必填；系统任务使用任务执行 ID |

Body：

```json
{
  "closeReason": "USER_CONFIRMED",
  "note": "用户已确认问题解决"
}
```

成功 `data`：状态为 `CLOSED`，返回 `closedAt`、`closedBy` 和 `ratingAllowed=true/false`。

失败示例：当前状态为 `IN_PROGRESS` 返回 `ILLEGAL_STATE_TRANSITION`，HTTP 409。

- 状态：`RESOLVED -> CLOSED`；关闭原因取 `USER_CONFIRMED`、`TIMEOUT`、`ADMIN_CLOSED`。
- 副作用：写关闭动作、状态历史、审计；发送关闭通知；保留评价入口。
- 领域事件：`TicketClosed`。
- 前端：客服只在 `RESOLVED` 显示关闭按钮；用户不直接调用客服关闭接口。
- 后端：关闭超时任务的具体时长为 `[待确认]`；一期必须支持任务重试且幂等。
- 测试/验收：覆盖客服关闭、管理员关闭、系统超时关闭、非法状态、重复关闭、权限和审计。

### ITSM-TICKET-005 用户重开工单

| 项目 | 约定 |
| --- | --- |
| 目标 | 用户对结果不满意或问题复发时，将工单重新交给客服处理 |
| 所属服务/调用方 | `ticket-service` / `USER` |
| HTTP/路径 | `POST /api/v1/tickets/{ticketId}/reopen` |
| Content-Type | `application/json; charset=utf-8` |
| 鉴权 | `ticket:reopen` |
| 租户/数据权限 | 当前租户、本人工单请求人 |
| 幂等 | `Idempotency-Key` 必填 |

Body：

```json
{
  "reason": "问题仍然存在，重新打开处理",
  "additionalDescription": "调整权限后仍然无法登录"
}
```

成功 `data`：状态为 `REOPENED`，返回 `reopenedAt`、`reopenedBy`。

失败示例：客服代替用户调用返回 `ROLE_FORBIDDEN` 或 `DATA_SCOPE_FORBIDDEN`，HTTP 403。

- 状态：`PENDING_USER_CONFIRM`、`RESOLVED`、`CLOSED -> REOPENED`。
- 副作用：写重开原因、追加用户消息、状态历史和审计；重新进入客服队列。
- 领域事件：`TicketReopened`、`TicketRouted`。
- 前端：重开表单必须要求原因；成功后展示“等待客服受理”。
- 后端：一期暂不限制重开时间窗口；是否限制重开次数和时间为 `[待确认]`，确认前不能由实现方自行增加限制。
- 测试/验收：覆盖三种可重开状态、空原因、非本人、重复请求、重新分派和历史保留。

### ITSM-RATING-001 用户评价工单

| 项目 | 约定 |
| --- | --- |
| 目标 | 收集用户满意度和补充意见，支持服务闭环和运营追踪 |
| 所属服务/调用方 | `rating-service` / `USER` |
| HTTP/路径 | `POST /api/v1/tickets/{ticketId}/rating` |
| Content-Type | `application/json; charset=utf-8` |
| 鉴权 | `rating:create` |
| 租户/数据权限 | 当前租户、本人工单请求人 |
| 幂等 | `Idempotency-Key` 必填；一期每个工单只允许一条当前评价 |

Body：

```json
{
  "score": 5,
  "tags": ["响应及时", "解决有效"],
  "comment": "处理很快，谢谢"
}
```

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `score` | Integer | 是 | `1-5` |
| `tags` | Array[String] | 否 | 标签编码；允许空 |
| `comment` | String | 否 | 最长 1000 字 |

成功 `data`：

```json
{
  "ticketId": "tkt_01J8V9X8AB",
  "ratingId": "rate_001",
  "score": 5,
  "submittedAt": "2026-08-25T08:40:00Z"
}
```

失败示例：工单状态为 `IN_PROGRESS` 返回 `ILLEGAL_STATE_TRANSITION`，HTTP 409。

- 状态：不改变工单状态；允许 `RESOLVED` 或 `CLOSED` 评价。
- 副作用：写评价、评价审计；触发运营统计事件。
- 领域事件：`TicketRated`。
- 前端：评分必须限制在 1-5；成功后不可重复提交同一幂等键；如需修改评价须另行确认二期接口。
- 后端：校验工单归属和状态；评价内容不得回写解决说明。
- 测试/验收：覆盖 1/5 分、越界分值、非本人、未解决工单、重复评价、标签非法和跨租户。

### ITSM-DICT-001 查询字典项

| 项目 | 约定 |
| --- | --- |
| 目标 | 为客服分类和管理员配置页面提供启用字典项 |
| 所属服务/调用方 | `dictionary-service` / 客服、管理员前端 |
| HTTP/路径 | `GET /api/v1/admin/dictionaries/{dictType}/items` |
| Content-Type | 响应 `application/json` |
| 鉴权 | 客服按读取权限；管理员全量读取 |
| 租户隔离 | 字典项属于当前租户；平台公共字典可由服务端合并 |
| 幂等 | 是 |

Path `dictType` 取值：

- `BUSINESS_LINE`
- `MANAGEMENT_UNIT`
- `SYMPTOM`
- `REASON`
- `SOLUTION_METHOD`
- `RATING_TAG`

Query：`parentId` 可选，`enabledOnly` 默认 `true`，`keyword` 可选，`page/pageSize` 可选。

成功 `data`：

```json
{
  "items": [
    {
      "itemId": "dict_reason_permission",
      "dictType": "REASON",
      "code": "PERMISSION_MISSING",
      "name": "权限缺失",
      "parentId": null,
      "enabled": true,
      "sort": 10,
      "version": 3
    }
  ],
  "page": 1,
  "pageSize": 20,
  "total": 1,
  "hasNext": false
}
```

失败示例：未知字典类型返回 `VALIDATION_ERROR`，HTTP 400。

- 状态/副作用/事件：只读。
- 前端：分类下拉只使用 `enabled=true` 项；空数据展示可配置空态，不自行创建默认项。
- 后端：返回结果必须按租户、父子层级和启用状态过滤；停用项仍可在历史详情中展示。
- 测试/验收：覆盖所有 dictType、树形 parentId、停用过滤、租户隔离和排序。

### ITSM-DICT-002 新增字典项

| 项目 | 约定 |
| --- | --- |
| 目标 | 管理员新增业务线、管理单元、症状、原因、解决方法或评价标签 |
| 所属服务/调用方 | `dictionary-service` / `SUPPORT_ADMIN` |
| HTTP/路径 | `POST /api/v1/admin/dictionaries/{dictType}/items` |
| Content-Type | `application/json; charset=utf-8` |
| 鉴权 | `dictionary:create` |
| 租户隔离 | 只能在当前租户创建 |
| 幂等 | `Idempotency-Key` 必填 |

Body：

```json
{
  "code": "PASSWORD_RESET",
  "name": "重置密码",
  "parentId": null,
  "sort": 20
}
```

成功 `data`：返回 `itemId`、完整字典项、`version=1`。

失败示例：同一租户同一 dictType 下 code 重复返回 `RESOURCE_CONFLICT`，HTTP 409。

- 状态：新字典项 `enabled=true`。
- 副作用：写配置审计。
- 领域事件：`DictionaryItemCreated`。
- 前端：仅管理员显示新增入口；成功后刷新对应字典列表。
- 后端：校验父项类型、编码唯一性、名称长度和租户归属。
- 测试/验收：覆盖权限、重复 code、非法 parentId、空名称、跨租户和审计日志。

### ITSM-DICT-003 更新字典项

| 项目 | 约定 |
| --- | --- |
| 目标 | 修改字典名称、排序、父项或启用说明 |
| 所属服务/调用方 | `dictionary-service` / `SUPPORT_ADMIN` |
| HTTP/路径 | `PATCH /api/v1/admin/dictionaries/items/{itemId}` |
| Content-Type | `application/json; charset=utf-8` |
| 鉴权 | `dictionary:update` |
| 租户隔离 | itemId 必须属于当前租户 |
| 幂等 | `Idempotency-Key` 必填 |

Body：

```json
{
  "name": "密码重置",
  "parentId": null,
  "sort": 20,
  "version": 1
}
```

成功 `data`：返回更新后的字典项和新版本号。

失败示例：版本不匹配返回 `RESOURCE_CONFLICT`，HTTP 409。

- 状态：字典项启用状态不由本接口改变；停用使用 `ITSM-DICT-004`。
- 副作用：写配置审计；不修改历史工单的快照显示。
- 领域事件：`DictionaryItemUpdated`。
- 前端：必须携带详情中的 `version`；冲突后重新加载。
- 后端：采用乐观锁或等价版本校验；不能修改已被历史工单引用的 ID。
- 测试/验收：覆盖并发更新、父项循环、名称和排序校验、越权。

### ITSM-DICT-004 停用字典项

| 项目 | 约定 |
| --- | --- |
| 目标 | 停止新工单选择某个字典项，同时保留历史数据可读性 |
| 所属服务/调用方 | `dictionary-service` / `SUPPORT_ADMIN` |
| HTTP/路径 | `POST /api/v1/admin/dictionaries/items/{itemId}/disable` |
| Content-Type | `application/json; charset=utf-8` |
| 鉴权 | `dictionary:disable` |
| 租户隔离 | itemId 必须属于当前租户 |
| 幂等 | `Idempotency-Key` 必填 |

Body：

```json
{
  "reason": "业务线合并",
  "version": 3
}
```

成功 `data`：返回 `enabled=false`、`disabledAt`、`disabledBy`。

失败示例：已停用项再次停用返回首次结果或 `RESOURCE_CONFLICT`，HTTP 409。

- 状态：`enabled=true -> false`；一期不提供删除，避免历史引用断裂。
- 副作用：写配置审计；影响后续字典查询和工单字段校验。
- 领域事件：`DictionaryItemDisabled`。
- 前端：停用前必须二次确认，显示影响范围；历史工单仍展示名称。
- 后端：禁止停用仍有启用子项的父项，除非先处理子项；禁止物理删除。
- 测试/验收：覆盖已停用项、子项依赖、历史工单展示、权限和租户隔离。

### ITSM-PERM-001 查询当前用户角色和权限

| 项目 | 约定 |
| --- | --- |
| 目标 | 返回当前用户菜单、按钮和数据权限，驱动三类工作台的可见性 |
| 所属服务/调用方 | `permission-service` / 所有前端 |
| HTTP/路径 | `GET /api/v1/permissions/me` |
| Content-Type | 响应 `application/json` |
| 鉴权 | `Authorization` |
| 租户隔离 | 返回当前令牌租户的权限 |
| 幂等 | 是 |

Query：`includeDataScope` Boolean，默认 `true`；仅返回当前用户允许范围。

成功 `data`：

```json
{
  "userId": "usr_support_01",
  "tenantId": "tenant_001",
  "roles": [
    {
      "roleId": "role_support_agent",
      "roleCode": "SUPPORT_AGENT",
      "roleName": "普通客服"
    }
  ],
  "permissions": [
    "ticket:read",
    "ticket:accept",
    "ticket:classify",
    "ticket:resolve",
    "ticket:close"
  ],
  "menus": ["HOME", "TICKET"],
  "dataScope": {
    "scopeType": "BUSINESS_LINE",
    "businessLineCodes": ["IT_SUPPORT"]
  },
  "permissionsVersion": "perm_20260825_01"
}
```

失败示例：令牌有效但权限服务不可用返回 `SERVICE_UNAVAILABLE`，HTTP 503；前端进入最小权限只读态并提示重试。

- 状态/副作用/事件：只读；可记录权限读取审计。
- 前端：按钮可见性只做体验控制，真正权限必须由服务端校验。
- 后端：权限缓存必须以租户、用户和权限版本隔离；权限变更后旧缓存不能无限期生效。
- 测试/验收：覆盖用户、普通客服、管理员、数据范围、权限版本变化和服务不可用。

### ITSM-PERM-002 查询角色列表

| 项目 | 约定 |
| --- | --- |
| 目标 | 管理员查看当前租户可用角色及启用状态 |
| 所属服务/调用方 | `permission-service` / `SUPPORT_ADMIN` |
| HTTP/路径 | `GET /api/v1/admin/roles` |
| Content-Type | 响应 `application/json` |
| 鉴权 | `role:read` |
| 租户隔离 | 返回当前租户角色；平台内置角色按租户授权范围返回 |
| 幂等 | 是 |

Query：`enabledOnly` 默认 `true`，`page` 默认 1，`pageSize` 默认 20，`keyword` 可选。

成功 `data` 为分页结构，列表项至少含 `roleId`、`roleCode`、`roleName`、`enabled`、`description`、`permissionCount`。

失败示例：普通客服调用返回 `ROLE_FORBIDDEN`，HTTP 403。

- 状态/副作用/事件：只读。
- 前端：仅管理员权限页面使用；空列表展示配置空态。
- 后端：不能返回其他租户的自定义角色；角色删除不在一期。
- 测试/验收：覆盖角色分页、关键字、启用过滤、非管理员和跨租户。

### ITSM-PERM-003 查询角色权限

| 项目 | 约定 |
| --- | --- |
| 目标 | 查看角色包含的菜单、按钮和数据权限，支持权限审计和配置页面展示 |
| 所属服务/调用方 | `permission-service` / `SUPPORT_ADMIN` |
| HTTP/路径 | `GET /api/v1/admin/roles/{roleId}/permissions` |
| Content-Type | 响应 `application/json` |
| 鉴权 | `role:permission:read` |
| 租户隔离 | roleId 必须属于当前租户或当前租户可见的平台角色 |
| 幂等 | 是 |

Path：`roleId`，String，必填。Query：`includeDataScope` 默认 `true`。

成功 `data`：

```json
{
  "roleId": "role_support_agent",
  "roleCode": "SUPPORT_AGENT",
  "permissions": [
    {
      "permissionCode": "ticket:accept",
      "permissionName": "受理工单",
      "permissionType": "BUTTON"
    }
  ],
  "menus": ["HOME", "TICKET"],
  "dataScope": {
    "scopeType": "BUSINESS_LINE",
    "businessLineCodes": ["IT_SUPPORT"]
  }
}
```

失败示例：角色不存在或不属于当前租户返回 `RESOURCE_NOT_FOUND`，HTTP 404。

- 状态/副作用/事件：只读。
- 前端：只展示，不在本版直接修改角色权限。
- 后端：权限结果按角色版本返回；角色权限修改接口为 `[不纳入一期]`，避免前后端对写契约产生误解。
- 测试/验收：覆盖管理员权限、普通客服禁止、角色隔离、数据范围和空权限角色。

## 8. 前端统一交互约束

1. 所有请求必须使用统一响应 `code` 判断结果；不能以 HTTP 200 作为唯一成功条件。
2. 所有命令按钮在请求期间进入 loading，成功或失败后恢复；不能因重复点击创建多个会话、工单、消息或评价。
3. `TOKEN_EXPIRED` 只允许自动刷新一次；刷新失败回到登录页，并保留用户未提交输入。
4. `ROLE_FORBIDDEN` 和 `DATA_SCOPE_FORBIDDEN` 不能通过重试解决；隐藏无权入口，但详情页仍要处理服务端拒绝。
5. `ILLEGAL_STATE_TRANSITION`、`RESOURCE_CONFLICT` 必须刷新资源详情，不允许前端强行覆盖服务端状态。
6. 工单详情页面按钮由服务端返回的状态和当前权限共同决定：
   - `PENDING_ACCEPTANCE`：显示受理。
   - `IN_PROGRESS`：显示分类、原因、解决方法、提交解决。
   - `PENDING_USER_CONFIRM`：用户显示确认、重开。
   - `RESOLVED`：用户显示评价和重开；客服显示关闭。
   - `CLOSED`：用户显示评价（未评价时）和重开。
   - `REOPENED`：客服显示重新受理。
7. 列表和详情中的枚举值必须保留原始英文值，显示文本由字典或稳定前端映射提供。
8. Agent 的置信度、来源摘要和建议分类是展示和辅助信息，不得被前端当作最终工单分类或权限依据。

## 9. 后端统一实现边界

1. Controller/Handler 只负责协议解析和基础校验；身份、租户、角色和数据范围校验必须进入统一上下文。
2. Service/Domain 层是状态机唯一执行位置；不得由 Controller 或前端直接拼接目标状态。
3. 工单状态历史、审计日志、处理人、解决时间和关闭时间属于后端事实字段，前端不能提交覆盖。
4. 工单、会话、消息、评价、字典和权限查询都必须附带租户条件。
5. 业务写操作与审计写入必须同事务；领域事件必须可靠投递。
6. Agent 服务只能提交决策和建议，工单服务决定是否创建工单、如何分派和何时改变工单状态。
7. 字典停用采用软停用，不能物理删除被历史工单引用的数据。
8. 所有敏感日志必须脱敏：令牌、SSO code、用户联系方式、内部 Agent 提示词和完整附件地址不得写入普通业务日志。
9. 具体 Controller 类名、数据库表名、缓存 Key、MQ 产品和部署拓扑不属于接口契约；后端可在不改变本文档的前提下选择实现。

## 10. 领域事件约定

一期领域事件至少包括：

| 事件 | 触发接口 | 关键字段 | 要求 |
| --- | --- | --- | --- |
| `UserAuthenticated` | `AUTH-001` | tenantId、userId、traceId、occurredAt | 不含令牌和 SSO code |
| `ConversationCreated` | `SESSION-001` | tenantId、sessionId、userId | 可用于初始化 Agent 上下文 |
| `UserMessageSent` | `CHAT-001` | tenantId、sessionId、messageId、userId | 消息已持久化后发布 |
| `AgentDecisionRecorded` | `AGENT-001` | tenantId、sessionId、decision、confidence | 不允许直接作为工单状态 |
| `HandoffRequested` | `AGENT-001`/`AGENT-002` | tenantId、sessionId、reason、businessLineCode | 用于分流和通知 |
| `TicketCreated` | `TICKET-001`/转人工 | tenantId、ticketId、ticketNo、requesterId、source | 工单事实已提交 |
| `TicketRouted` | 建单/重开 | tenantId、ticketId、businessLineCode、queueId | 分派失败可重试 |
| `TicketAccepted` | `SUPPORT-002` | tenantId、ticketId、assigneeId | 受理成功后发布 |
| `TicketClassificationUpdated` | `SUPPORT-003` | tenantId、ticketId、classification、operatorId | 不包含跨租户字段 |
| `TicketResolutionSubmitted` | `SUPPORT-004` | tenantId、ticketId、resolution、operatorId | 用户通知可订阅 |
| `TicketUserConfirmed` | `TICKET-004` | tenantId、ticketId、userId | 与 `TicketResolved` 同一业务事务 |
| `TicketClosed` | `SUPPORT-005` | tenantId、ticketId、closeReason、closedBy | 关闭可由任务触发 |
| `TicketReopened` | `TICKET-005` | tenantId、ticketId、userId、reason | 重新进入客服队列 |
| `TicketRated` | `RATING-001` | tenantId、ticketId、ratingId、score | 供运营统计使用 |
| `DictionaryItemCreated/Updated/Disabled` | `DICT-002/003/004` | tenantId、itemId、dictType、operatorId | 必须可审计 |

事件名称、字段和可靠投递方式为后端内部实现契约；如果需要对外开放事件，必须新增版本化事件文档，不得直接复用内部事件。

## 11. 测试要求

测试 Agent 必须以本文档为唯一依据，至少覆盖：

### 11.1 接口功能

- 登录、当前用户、会话创建/读取、消息发送。
- Agent 自助解决、Agent 转人工、用户明确转人工。
- 手动建单、工单分页、工单详情。
- 客服队列、受理、分类/原因/解决方法更新、提交解决。
- 用户确认、客服关闭、用户重开、用户评价。
- 字典查询、新增、更新、停用。
- 当前权限、角色列表、角色权限查询。

### 11.2 通用异常

- 缺少 Authorization、X-Tenant-Id、Idempotency-Key。
- 令牌过期、令牌伪造、租户不匹配。
- 参数缺失、长度越界、枚举非法、评分越界。
- 重复幂等键、同键不同 Body、并发版本冲突。
- 资源不存在、跨租户访问、数据范围越权、角色越权。

### 11.3 状态机

- 每一条允许流转均有成功用例。
- 每一条未列出的流转均返回 `ILLEGAL_STATE_TRANSITION`。
- 重复确认、重复关闭、重复重开、重复受理均符合幂等或冲突规则。
- Agent 决策不得绕过客服直接把工单置为 `RESOLVED` 或 `CLOSED`。

### 11.4 前后端联调

- 统一响应字段完整且 `traceId` 可贯穿前后端日志。
- 列表分页字段、详情字段和状态枚举一致。
- 前端按钮状态与服务端错误码一致。
- Agent 不可用时用户消息不丢失，前端可展示重试或转人工。
- 领域事件不会因重复请求重复产生。

## 12. 一期验收标准

1. 用户可以完成登录、打开“我的助手”、创建/读取会话并发送消息。
2. Agent 可以返回自助答复；无法解决或用户要求人工时，系统能创建工单并保留会话上下文。
3. 用户可以查看本人工单分页和详情，看到当前状态、处理进度和历史记录。
4. 客服可以查询本业务线或被分配范围内的工单，完成受理、分类、原因、解决方法和解决结果记录。
5. 工单可以按本文档状态机完成“待受理 -> 处理中 -> 待用户确认 -> 已解决 -> 已关闭”，并支持用户重开。
6. 用户可以对 `RESOLVED` 或 `CLOSED` 工单提交 1-5 分评价，评价可追溯。
7. 管理员可以查询并维护一期字典项，停用项不能被新工单选择，历史工单仍可读取。
8. 前端可以查询当前用户角色、菜单、按钮和数据权限，后端不依赖前端隐藏按钮做安全控制。
9. 所有成功和失败响应均符合统一响应结构；统一错误码、HTTP 状态、`traceId` 和 `details` 可用于联调排错。
10. 所有关键动作均有状态历史、审计日志和对应领域事件，重复请求不会重复写入。

## 13. 一期确定项与待确认项清单

### 13.1 本版已确定

- 统一响应字段为 `code/message/data/traceId/details`。
- 业务接口基础路径为 `/api/v1`。
- 登录后使用 Bearer token；租户使用 `X-Tenant-Id` 并与令牌校验。
- 不信任 `X-User-Id`、`X-Operator-Id`、`X-Role`、`X-User-Type`。
- 命令接口使用 `Idempotency-Key`。
- 工单状态和允许流转以第 5 节为准。
- 用户、客服、管理员和 Agent 的角色边界以第 4 节为准。
- Agent 只提交决策和建议，不直接修改工单事实状态。
- 分类、原因、解决方法在客服提交解决前必须完成。
- 字典项停用而非物理删除。
- 用户只能操作本租户、本人工单；客服按数据范围操作。

### 13.2 必须由产品/架构确认，但不改变本版基础契约

- 企业 SSO 的具体供应商、`ssoCode` 字段映射和刷新令牌策略。
- 多租户场景是否始终要求前端传 `X-Tenant-Id`，以及网关注入方式。
- Agent 置信度阈值、超时时间、重试次数和人工兜底队列。
- 业务线到客服队列的具体分流规则和默认待分流队列编码。
- `PENDING_USER_CONFIRM` 自动确认和 `RESOLVED` 自动关闭的具体时长。
- 用户评价标签的最终编码和是否允许修改已提交评价。
- 重开时间窗口、重开次数限制和是否要求重新生成工单号。
- 附件服务的上传接口、文件大小、类型和病毒扫描要求。
- 角色权限写接口是否在一期追加；本版只交付角色/权限查询。

在上述事项确认前，前后端必须按照本文档的字段、路径、错误码和状态流转实现，不得以“待确认”为理由私自新增接口或修改接口契约。

## 14. 旧资料吸收与不恢复说明

本版吸收 Git 中已删除旧接口资料的以下可复用内容：

- `ApiResponse` 的 `code/message/data/traceId` 统一响应思想。
- 工单详情需要包含状态历史、审计摘要、Agent 摘要和解决信息。
- 用户、客服和管理员存在不同数据范围。
- Agent 作为建议和分流来源，不作为工单事实状态的直接修改者。
- 工单动作需要可审计且状态流转需要显式校验。

以下内容没有恢复或沿用：

- 工作区当前已删除的旧 Java 源码、测试代码和旧接口文档。
- 旧接口中的 `X-User-Id`、`X-Operator-Id`、`X-Role` 可信身份约定。
- 旧接口中的 `SUBMITTED`、`AGENT_ANSWERED`、`TECH_ANALYSIS`、`IN_SUPPORT` 等公开工单状态。
- 旧接口中以 Agent 置信度直接决定工单公开状态的做法。
- 旧实现中把用户身份、操作者身份放入请求 Body 的字段。

本文件是新建的一期接口交付物，不恢复、不覆盖用户当前删除或未提交的旧实现。
