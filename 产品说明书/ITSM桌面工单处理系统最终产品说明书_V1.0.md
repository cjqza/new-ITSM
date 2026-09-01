# ITSM 桌面工单处理系统 — 最终产品说明书

> **版本**：V1.0
> **日期**：2026-09-01
> **编写团队**：cza 集团 ITSM 项目组
> **文档性质**：最终产品说明书，覆盖 Java 后端 + Python AI Agent 全部已交付能力

---

## 一、产品概述

### 1.1 产品定位

ITSM 桌面工单处理系统是一套面向企业内部桌面支持与业务咨询的智能服务管理平台。系统将"聊天式智能接待"与"工单式闭环处理"融为一体，用户通过对话即可完成问题咨询、转人工、进度追踪和服务评价，客服通过工单台高效受理、分类和处理问题，管理员通过配置中心维护字典、权限和分流规则。

### 1.2 产品目标

1. 让用户在一个统一入口中完成咨询、提交问题、追踪处理进度和评价服务体验。
2. 让 AI 智能客服优先处理可自动解决的问题，无法解决时自动识别业务线并创建工单转人工。
3. 让客服在高密度工单台中快速受理、分类和处理问题，形成标准化工单数据。
4. 让管理员能够维护工单可选项、权限体系和业务分流规则，保证系统可持续迭代。
5. 让全流程具备审计、历史追踪和运营分析能力。

### 1.3 核心价值主张

- **用户侧**：像聊天一样提问题，不用填复杂表单。
- **客服侧**：工单台高效处理，AI 已帮你做好首轮接待和信息收集。
- **管理侧**：字典和权限可配置，系统可持续运营。

---

## 二、系统总体架构

### 2.1 技术全景

系统由两大部分组成：

- **Java 后端服务**（主服务）：基于 Spring Boot 4 的单体应用，承载全部核心业务逻辑、API 接口、数据库持久化、安全认证和实时通信。
- **Python AI Agent 服务**（智能客服引擎）：基于 LangGraph 的智能客服编排服务，负责用户意图识别、问题诊断和转人工决策。

### 2.2 架构图

> 📌 [此处插入架构总览图]

建议画面内容：左侧三类前端（用户/客服/管理员）→ 中间 API 网关 → 右侧 Java 后端各服务模块 + Python AI Agent → 底层 MySQL / Redis / RabbitMQ

### 2.3 部署架构

| 组件 | 技术方案 | 部署方式 |
| --- | --- | --- |
| Java 后端 | Spring Boot 4.1.1 + JDK 17 | 模块化单体，单一进程部署 |
| Python AI Agent | FastAPI + LangGraph | 独立进程，默认端口 8090 |
| 数据库 | MySQL 8.0 | 主存储，Flyway 自动迁移 |
| 缓存 | Redis 6+ | 幂等键、会话缓存、短期状态 |
| 消息队列 | RabbitMQ 3.8+ | 领域事件可靠投递（可选） |
| 前端 | HTML/CSS/JS 静态页面 | 随 Java 后端静态资源部署 |

---

## 三、角色定义与权限模型

### 3.1 系统角色

| 角色 | 角色编码 | 定位 | 核心职责 |
| --- | --- | --- | --- |
| 用户 | USER | 提交问题的一方 | 登录、发起咨询、接收答复、确认解决、完成评价 |
| 智能客服 Agent | AGENT_SERVICE | 首轮接待与分流 | 理解用户意图、自动答复、创建工单、转人工、生成摘要 |
| 普通客服 | SUPPORT_AGENT | 工单处理人员 | 受理工单、分类、排查、填写处理过程、置为已解决 |
| 管理员客服 | SUPPORT_ADMIN | 配置与权限维护人员 | 维护字典项、业务线、分流规则、客服权限 |
| 主管/质检 | SUPERVISOR | 运营监督人员 | 查看统计、抽检工单（一期为只读查询） |

### 3.2 权限模型

系统采用 RBAC（基于角色的访问控制）权限模型：

- **功能权限**：控制用户能执行哪些操作（如受理、分类、关闭工单）。
- **菜单权限**：控制用户能看到哪些页面入口。
- **数据权限**：控制用户能看到哪些范围的数据（如本人工单、本业务线工单）。
- **租户隔离**：所有数据按租户隔离，用户只能访问自己租户的数据。

> 📌 [此处插入权限矩阵图]

---

## 四、用户端功能说明

### 4.1 功能总览

用户端提供"聊天式智能助手"入口，用户通过对话即可完成问题咨询和工单追踪。

| 功能 | 说明 |
| --- | --- |
| 手机号注册/登录 | 支持手机号 + 验证码注册，账号密码登录 |
| 智能客服对话 | 与 AI 智能客服实时聊天，自动获得问题诊断和解决方案 |
| 转人工服务 | 用户主动要求或 AI 判断需要人工时，自动创建工单并转接 |
| 工单状态查看 | 查看本人所有工单的当前状态和处理进度 |
| 工单确认与重开 | 确认客服处理结果，不满意时可重开工单 |
| 服务评价 | 对已解决/已关闭工单提交满意度评分和评价 |

### 4.2 用户端页面结构

> 📌 [此处插入用户端聊天界面截图]

建议画面：左侧会话列表 + 中间聊天主区 + 右侧工单摘要

### 4.3 核心交互流程

**流程一：自助咨询**

1. 用户进入"我的助手"，看到欢迎语和常用问题建议。
2. 用户输入问题并发送，AI 智能客服自动诊断并返回结构化回答。
3. 用户可以继续追问、点击建议或重新描述。
4. 问题解决后，用户可结束对话。

**流程二：转人工处理**

1. 用户点击"转人工"或 AI 判断需要人工介入。
2. 系统自动创建工单，携带聊天上下文和 AI 诊断摘要。
3. 工单自动分派到对应业务线的客服队列。
4. 用户可在聊天区继续补充信息，客服处理后通知用户。

**流程三：确认与评价**

1. 客服提交解决结果后，用户收到通知。
2. 用户确认解决结果，或选择重开工单。
3. 确认后用户可提交满意度评分（1-5 分）和评价。

> 📌 [此处插入用户端核心流程图]

---

## 五、客服端功能说明

### 5.1 功能总览

客服端提供"工单台"式处理界面，客服通过列表和详情页高效处理工单。

| 功能 | 说明 |
| --- | --- |
| 工作台首页 | 展示待处理、即将超时、草稿箱等核心面板 |
| 工单队列 | 查看待受理、处理中、待确认、已解决、历史工单 |
| 受理工单 | 从队列中受理工单，成为当前处理人 |
| 工单分类 | 补齐管理单元、症状、原因、解决方法等标准分类 |
| 提交解决 | 记录处理过程和解决说明，提交给用户确认 |
| 关闭工单 | 用户确认后或超时后关闭工单 |
| 历史查询 | 按工单号、用户、业务线、状态等条件检索历史工单 |

### 5.2 客服端页面结构

> 📌 [此处插入客服工作台首页截图]

建议画面：左侧导航 + 右上四象限面板（草稿箱 / 等待我处理 / 即将超时 / 待本岗位处理）

### 5.3 工单处理流程

1. **受理**：客服在队列中看到待处理工单，点击"受理"成为处理人。
2. **查看详情**：查看用户聊天记录、AI 诊断摘要和问题背景。
3. **分类补齐**：选择或补充管理单元、症状、原因、解决方法。
4. **提交解决**：填写处理过程和解决说明，提交给用户确认。
5. **关闭工单**：用户确认后关闭，或超时自动关闭。

> 📌 [此处插入客服工单处理流程图]

---

## 六、管理员端功能说明

### 6.1 功能总览

管理员端提供"配置中心"，维护系统运行所需的字典、权限和分流规则。

| 功能 | 说明 |
| --- | --- |
| 字典管理 | 维护业务线、管理单元、症状、原因、解决方法等可配置项 |
| 权限查询 | 查看当前租户的角色列表和角色权限 |
| 员工管理 | 查询员工信息和部门结构 |
| 配置审计 | 查看字典和权限的变更记录 |

### 6.2 字典体系

| 字典维度 | 用途 | 示例 |
| --- | --- | --- |
| 业务线 | 决定工单派给谁 | IT 支持、订单问题、售后服务 |
| 管理单元 | 决定是什么场景 | 网络问题、操作系统、浏览器、账号 |
| 症状 | 描述问题表现 | 无法登录、页面报错、收不到通知 |
| 原因 | 描述为什么发生 | 权限不足、网络异常、配置错误 |
| 解决方法 | 描述怎么处理 | 重置密码、清缓存、调整权限 |

> 📌 [此处插入管理员配置中心截图]

建议画面：左侧导航 + 页签切换不同字典类型 + 表格列表 + 新增/编辑/停用操作

---

## 七、工单生命周期

### 7.1 工单状态定义

| 状态 | 状态码 | 说明 |
| --- | --- | --- |
| 新建 | NEW | 工单已创建，等待系统分派 |
| 待受理 | PENDING_ACCEPTANCE | 已进入客服队列，等待客服受理 |
| 处理中 | IN_PROGRESS | 客服已接手并正在处理 |
| 待用户确认 | PENDING_USER_CONFIRM | 客服已提交解决结果，等待用户确认 |
| 已解决 | RESOLVED | 用户确认解决或超时自动确认 |
| 已关闭 | CLOSED | 工单生命周期结束 |
| 重开 | REOPENED | 用户对结果不满意，要求继续处理 |

### 7.2 状态流转图

> 📌 [此处插入工单状态流转图]

建议画面：状态节点连线图，标注每个流转的触发条件

`
新建(NEW) → 待受理(PENDING_ACCEPTANCE) → 处理中(IN_PROGRESS)
    → 待用户确认(PENDING_USER_CONFIRM) → 已解决(RESOLVED) → 已关闭(CLOSED)
                                                          ↘ 重开(REOPENED) → 处理中
`

### 7.3 状态流转规则

| 当前状态 | 目标状态 | 触发方 | 前置条件 |
| --- | --- | --- | --- |
| NEW | PENDING_ACCEPTANCE | 系统 | 工单创建成功 |
| PENDING_ACCEPTANCE | IN_PROGRESS | 客服 | 客服有权限且未被抢占 |
| REOPENED | IN_PROGRESS | 客服 | 客服有权限 |
| IN_PROGRESS | PENDING_USER_CONFIRM | 客服 | 分类、原因、解决方法已填写 |
| PENDING_USER_CONFIRM | RESOLVED | 用户 | 用户确认解决 |
| PENDING_USER_CONFIRM | REOPENED | 用户 | 用户认为未解决 |
| RESOLVED | CLOSED | 客服/系统 | 解决结果已存在 |
| RESOLVED | REOPENED | 用户 | 用户是请求人 |
| CLOSED | REOPENED | 用户 | 用户是请求人 |

---

## 八、Java 后端技术实现

### 8.1 技术栈

| 技术 | 版本 | 用途 |
| --- | --- | --- |
| Spring Boot | 4.1.1 | 应用框架 |
| Java | 17 | 开发语言 |
| MyBatis-Plus | 3.5.17 | 持久层框架 |
| Flyway | 最新 | 数据库版本管理 |
| MySQL | 8.0 | 主数据库 |
| Redis | 6+ | 缓存、幂等键、短期状态 |
| RabbitMQ | 3.8+ | 消息队列（可选） |
| Spring Security | 最新 | 安全认证框架 |
| JWT (jjwt) | 0.12.6 | 令牌签发与验证 |
| WebSocket | - | 实时消息推送 |
| Knife4j + SpringDoc | - | API 文档 |
| Hutool | - | 工具库 |
| Lombok | - | 代码简化 |
| Pinyin4j | - | 拼音转换 |

### 8.2 项目结构

`
itsm/
├── itsm-common/          # 通用基础设施
│   ├── ApiResponse       # 统一响应结构
│   ├── BusinessException # 业务异常
│   ├── ErrorCode         # 统一错误码
│   └── PageResponse      # 分页响应封装
│
├── itsm-pojo/            # 领域模型
│   ├── entity/           # 数据库实体（25+ 个实体类）
│   ├── dto/              # 请求/响应 DTO
│   └── entity/base/      # 租户隔离基类
│
├── itsm-server/          # 主应用
│   ├── api/              # REST 控制器（17 个 Controller）
│   ├── config/           # Spring 配置
│   ├── domain/           # 领域对象
│   ├── dto/              # 服务层 DTO
│   ├── service/          # 业务逻辑层（20+ 个 Service）
│   ├── repository/       # 数据访问层
│   ├── infrastructure/   # 基础设施（审计、持久化 Mapper）
│   ├── websocket/        # WebSocket 处理
│   └── resources/db/     # Flyway 迁移脚本（V1~V9）
│
└── pom.xml               # 父 POM
`

### 8.3 核心服务模块

| 服务模块 | 职责 | 对应接口 |
| --- | --- | --- |
| AuthService | 登录、令牌签发、当前用户 | /api/v1/auth/* |
| PermissionService | 角色、菜单、按钮、数据权限 | /api/v1/permissions/* |
| ConversationService | 会话创建、消息存储、聊天上下文 | /api/v1/conversations/* |
| AgentOrchestrationService | Agent 决策接收、自助回复、转人工编排 | /api/v1/agent/* |
| AiAgentService | 调用 Python AI Agent 服务 | 内部 HTTP 调用 |
| TicketService | 工单事实、状态机、分页、详情、流转 | /api/v1/tickets/* |
| SupportService | 客服队列、受理、分类、解决、关闭 | /api/v1/support/* |
| RatingService | 用户满意度评价 | /api/v1/ratings/* |
| DictionaryService | 字典项查询、新增、更新、停用 | /api/v1/admin/dictionaries/* |
| ColleagueMessageService | 内部消息通信 | /api/v1/colleagues/* |
| ContactService | 联系人查询 | /api/v1/contacts/* |
| DepartmentService | 部门信息管理 | /api/v1/admin/departments/* |
| EmployeeService | 员工信息查询 | /api/v1/admin/employees/* |

### 8.4 数据库设计

#### 核心数据表

| 数据表 | 说明 | 关键字段 |
| --- | --- | --- |
| app_user | 用户信息 | user_id, tenant_id, display_name, department_name |
| user_credential | 登录凭证 | login_name, password_hash, password_algo, auth_version |
| tenant | 租户信息 | tenant_id, tenant_name, enabled |
| conversation_session | 会话 | session_id, user_id, status, summary, ticket_id |
| conversation_message | 消息 | message_id, session_id, sender_type, content |
| agent_decision | Agent 决策 | decision, confidence, business_line_code |
| ticket | 工单 | ticket_id, ticket_no, status, priority, business_line_code |
| ticket_status_history | 状态历史 | from_status, to_status, operator_id |
| ticket_classification | 分类 | management_unit_id, symptom_id, reason_id, solution_method_id |
| ticket_action_log | 动作日志 | action_type, operator_id, action_content |
| rating | 评价 | ticket_id, score, tags, comment |
| dictionary_item | 字典项 | dict_type, code, name, parent_id, enabled, version |
| rbac_role | 角色 | role_code, role_name, enabled |
| rbac_permission | 权限 | permission_code, permission_name, permission_type |
| rbac_role_permission | 角色权限关联 | role_id, permission_id |
| audit_log | 审计日志 | operator_id, action_type, resource_type, resource_id |
| idempotency_record | 幂等记录 | tenant_id, caller_id, method, path, request_hash |
| outbox_event | 领域事件 | event_type, payload, status, published_at |

#### 数据库版本管理

项目使用 Flyway 管理数据库版本，迁移脚本自动执行：

| 版本 | 说明 |
| --- | --- |
| V1 | 初始化核心表结构 |
| V2 | 认证字段与种子数据 |
| V3 | 用户手机号唯一约束 |
| V4 | 权限申请表 |
| V5 | 同事消息表 |
| V6 | 消息已读状态 |
| V7 | 部门表 |
| V8 | 会话参与者表 |
| V9 | 工单挂起功能 |

### 8.5 安全设计

#### 认证方案

- **令牌类型**：JWT（RS256 签名算法）
- **访问令牌**：有效期 7200 秒，携带用户 ID、租户 ID、角色列表和权限版本
- **刷新令牌**：有效期 7-14 天，支持轮换，服务端可撤销
- **密码存储**：BCrypt 哈希，不保存明文

#### 请求头规范

| Header | 必填 | 说明 |
| --- | --- | --- |
| Authorization | 登录后必填 | Bearer {accessToken} |
| X-Tenant-Id | 登录后必填 | 当前租户，必须与令牌一致 |
| Idempotency-Key | 命令接口必填 | 幂等键，24 小时有效 |
| X-Trace-Id | 可选 | 链路追踪 ID |

#### 权限校验顺序

1. 验证 JWT 签名和过期时间
2. 校验 X-Tenant-Id 与令牌租户一致
3. 校验功能权限（如 ticket:accept）
4. 校验数据权限（如本业务线工单）
5. 校验工单状态是否允许操作
6. 校验参数和字典项有效性

### 8.6 统一接口规范

#### 响应结构

`json
{
  "code": "SUCCESS",
  "message": "success",
  "data": {},
  "traceId": "trc_20260901_001",
  "details": null
}
`

#### 分页响应

`json
{
  "items": [],
  "page": 1,
  "pageSize": 20,
  "total": 0,
  "hasNext": false
}
`

#### 错误码体系

| 错误码 | HTTP | 说明 |
| --- | --- | --- |
| SUCCESS | 200/201 | 请求成功 |
| VALIDATION_ERROR | 400 | 字段校验失败 |
| AUTH_REQUIRED | 401 | 未登录 |
| TOKEN_EXPIRED | 401 | 令牌过期 |
| TENANT_FORBIDDEN | 403 | 租户无权限 |
| ROLE_FORBIDDEN | 403 | 角色无权限 |
| RESOURCE_NOT_FOUND | 404 | 资源不存在 |
| ILLEGAL_STATE_TRANSITION | 409 | 状态不允许操作 |
| IDEMPOTENCY_CONFLICT | 409 | 幂等键冲突 |
| AGENT_UNAVAILABLE | 503 | AI 服务不可用 |

### 8.7 接口清单

| 模块 | 路径前缀 | 接口数量 | 说明 |
| --- | --- | --- | --- |
| 认证 | /api/v1/auth | 4 | 登录、当前用户、刷新令牌、退出 |
| 工单 | /api/v1/tickets | 5 | 建单、查询、详情、确认、重开 |
| 客服工单 | /api/v1/support/tickets | 5 | 队列、受理、分类、解决、关闭 |
| 会话 | /api/v1/conversations | 3 | 创建会话、读取会话、发送消息、转人工 |
| Agent | /api/v1/agent | 1 | Agent 决策编排 |
| 评价 | /api/v1/ratings | 1 | 工单评价提交 |
| 字典 | /api/v1/admin/dictionaries | 4 | 查询、新增、更新、停用 |
| 权限 | /api/v1/permissions | 3 | 当前权限、角色列表、角色权限 |
| 员工 | /api/v1/admin/employees | 1 | 员工信息查询 |
| 部门 | /api/v1/admin/departments | 1 | 部门信息管理 |
| 通讯录 | /api/v1/contacts | 1 | 联系人查询 |
| 同事消息 | /api/v1/colleagues/messages | 1 | 内部消息通信 |

---

## 九、Python AI Agent 技术实现

### 9.1 技术栈

| 技术 | 版本 | 用途 |
| --- | --- | --- |
| Python | 3.12+ | 开发语言 |
| FastAPI | 0.115+ | Web 框架 |
| LangGraph | 0.2+ | AI 工作流编排 |
| LangChain | 0.3+ | LLM 应用框架 |
| LangChain-OpenAI | 0.2+ | OpenAI 兼容接口 |
| Pydantic | 2.0+ | 数据验证 |
| Uvicorn | 0.30+ | ASGI 服务器 |
| DashScope | - | 阿里云通义千问 API |

### 9.2 项目结构

`
ai-agent/
├── agent.py              # LangGraph 工作流定义
├── server.py             # FastAPI 服务入口
├── requirements.txt      # Python 依赖
└── start.ps1             # 启动脚本
`

### 9.3 AI 工作流架构

AI Agent 采用 LangGraph 实现三节点工作流：

`
用户消息 → classify(分类) → diagnose(诊断) → decide(决策) → 返回结果
`

#### 节点一：classify（问题分类）

- **输入**：用户消息文本
- **处理**：调用 LLM 将问题分类为 6 个类别之一
- **输出**：分类结果（SYSTEM/SOFTWARE/ACCOUNT/NETWORK/PERIPHERAL/OTHER）+ 优先级（high/medium/low）

#### 节点二：diagnose（问题诊断）

- **输入**：用户消息、聊天历史、分类结果、优先级
- **处理**：调用 LLM 生成结构化诊断报告
- **输出**：
  - 问题摘要
  - 排查步骤（3-5 步）
  - 解决方案
  - 是否建议转人工
  - 置信度评分（0.0-1.0）
  - 预防建议

#### 节点三：decide（最终决策）

- **输入**：诊断结果、置信度
- **处理**：验证结果完整性，置信度低于 0.5 时强制转人工
- **输出**：最终决策（自助解决或转人工）

### 9.4 问题分类体系

| 分类 | 分类码 | 典型问题 |
| --- | --- | --- |
| 系统问题 | SYSTEM | 无法开机、系统崩溃、蓝屏、卡顿 |
| 软件问题 | SOFTWARE | 软件报错、功能异常、版本兼容 |
| 账号问题 | ACCOUNT | 无法登录、账号锁定、权限不足 |
| 网络问题 | NETWORK | 无法上网、VPN 断连、网络延迟 |
| 外设问题 | PERIPHERAL | 打印机异常、显示器故障、键盘鼠标问题 |
| 其他 | OTHER | 无法明确分类的问题 |

### 9.5 强制转人工条件

出现以下情况时，AI Agent 必须建议转人工：

1. 涉及硬件物理损坏、硬件更换、拆机操作
2. 需要管理员权限才能执行的操作（域账号解锁、权限变更）
3. 连续给出 3 套解决方案仍无法解决
4. 系统崩溃、无法开机、反复蓝屏等严重故障
5. 涉及公司安全策略、存在违规操作风险
6. 用户明确要求上门处理

### 9.6 API 接口

#### 健康检查

`
GET /api/v1/ai/health
Response: { "status": "ok" }
`

#### 智能对话

`
POST /api/v1/ai/chat
Request:
{
  "message": "我的电脑无法开机了",
  "history": [
    {"role": "user", "content": "之前电脑有点卡"},
    {"role": "assistant", "content": "建议清理磁盘空间"}
  ]
}

Response:
{
  "response": "【问题摘要】电脑无法开机，可能为电源或硬件故障...\n【问题分类】SYSTEM\n【优先级判定】高\n...",
  "classification": "SYSTEM",
  "priority": "high",
  "confidence": 0.85,
  "shouldHandoff": true,
  "handoffReason": "涉及硬件故障，建议现场排查"
}
`

### 9.7 系统提示词设计

AI Agent 的系统提示词定义了资深桌面运维工程师的角色，包括：

- **核心任务**：基于工单信息完成问题诊断、排查指引、方案输出
- **输出规范**：严格按照结构化格式输出（问题摘要、分类、优先级、排查步骤、解决方案、转人工建议、置信度、预防建议）
- **处理原则**：从简到繁、从软到硬，先无风险快速排查，再深度解决
- **禁忌规则**：严禁绕过安全策略、猜测不确定原因、未提示备份就给出数据风险操作

### 9.8 配置参数

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| DASHSCOPE_API_KEY | - | 阿里云 DashScope API 密钥（必填） |
| DASHSCOPE_BASE_URL | https://dashscope.aliyuncs.com/compatible-mode/v1 | API 端点 |
| DASHSCOPE_MODEL | qwen-plus | 使用的模型 |
| AI_AGENT_PORT | 8090 | 服务监听端口 |

---

## 十、Java 与 Python 服务集成

### 10.1 集成架构

Java 后端通过 HTTP 调用 Python AI Agent 服务：

`
用户消息 → Java ConversationService → Java AiAgentService → HTTP POST → Python /api/v1/ai/chat
                                                                    ↓
Python AI Agent 返回决策结果 ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ←
                                                                    ↓
Java AgentOrchestrationService 根据决策结果：
  - shouldHandoff=false → 返回 AI 回复给用户
  - shouldHandoff=true  → 创建工单 + 转人工
`

### 10.2 集成流程

1. 用户在聊天界面发送消息。
2. Java 后端将消息持久化到数据库。
3. Java 后端异步调用 Python AI Agent 的 /api/v1/ai/chat 接口。
4. Python AI Agent 返回结构化决策结果。
5. Java 后端根据 shouldHandoff 字段决定：
   - 如果为 alse：将 AI 回复作为助手消息返回给用户。
   - 如果为 	rue：自动创建工单，携带聊天上下文和 AI 摘要，分派到客服队列。

### 10.3 降级策略

当 Python AI Agent 不可用时（超时、服务异常），Java 后端的降级策略：

1. 返回 AGENT_UNAVAILABLE 错误给前端。
2. 用户消息不会丢失，已持久化到数据库。
3. 前端展示重试或转人工入口。
4. 用户可以选择直接转人工服务。

---

## 十一、前端实现

### 11.1 前端技术方案

前端采用纯 HTML/CSS/JavaScript 实现，随 Java 后端静态资源部署，无需独立前端构建工具。

| 文件 | 说明 |
| --- | --- |
| index.html | 管理员端和客服端主页面 |
| app.js | 管理员端和客服端交互逻辑 |
| styles.css | 全局样式 |
| user-portal.html | 用户端聊天工作台 |
| user-portal.js | 用户端交互逻辑 |
| user-portal.css | 用户端样式 |
| mock-data.js | 模拟数据 |

### 11.2 三端页面

#### 用户端（user-portal.html）

- 聊天式智能助手界面
- 左侧会话列表 + 中间聊天主区 + 右侧工单摘要
- 支持消息发送、转人工、工单状态查看、评价

#### 客服端（index.html）

- 工单台式处理界面
- 左侧导航 + 右侧工作面板
- 支持工单队列、受理、分类、解决、关闭

#### 管理员端（index.html）

- 配置中心式管理界面
- 左侧导航 + 页签切换 + 表格列表
- 支持字典管理、权限查询、员工管理

---

## 十二、API 文档

### 12.1 文档入口

项目提供两套 API 文档：

- **Swagger UI**：http://localhost:8080/swagger-ui.html
- **Knife4j 文档**：http://localhost:8080/doc.html

### 12.2 接口规范文档

完整的接口契约定义在：

- [ITSM 一期核心接口文档 V1.0](../接口文档/ITSM一期核心接口文档_v1.0.md)
- [ITSM 一期核心接口产品分析与功能分类 V1.0](../接口文档/ITSM一期核心接口产品分析与功能分类_v1.0.md)

---

## 十三、快速开始

### 13.1 环境要求

| 环境 | 版本要求 |
| --- | --- |
| JDK | 17+ |
| Maven | 3.9+ |
| MySQL | 8.0+ |
| Redis | 6+ |
| RabbitMQ | 3.8+（可选） |
| Python | 3.12+（AI Agent） |
| Node.js | 不需要（纯静态前端） |

### 13.2 启动 Java 后端

`ash
# 1. 克隆项目
git clone <repository-url>
cd itsm

# 2. 构建项目
mvn clean install

# 3. 配置数据库
# 编辑 itsm-server/src/main/resources/application.yaml
# 配置 MySQL 连接信息

# 4. 启动应用
cd itsm-server
mvn spring-boot:run
`

### 13.3 启动 Python AI Agent

`ash
# 1. 进入 ai-agent 目录
cd ai-agent

# 2. 安装依赖
pip install -r requirements.txt

# 3. 配置环境变量
export DASHSCOPE_API_KEY="your-api-key"
export DASHSCOPE_BASE_URL="https://dashscope.aliyuncs.com/compatible-mode/v1"
export DASHSCOPE_MODEL="qwen-plus"
export AI_AGENT_PORT=8090

# 4. 启动服务
python server.py
`

或使用 PowerShell 启动脚本：

`powershell
sk-86124f0fe26c4ffcbd9b239ed6a8ea53="your-api-key"
.\start.ps1
`

### 13.4 访问系统

启动完成后：

- **前端页面**：http://localhost:8080
- **用户端**：http://localhost:8080/user-portal.html
- **API 文档**：http://localhost:8080/swagger-ui.html
- **Knife4j 文档**：http://localhost:8080/doc.html
- **AI Agent 健康检查**：http://localhost:8090/api/v1/ai/health

### 13.5 默认账号

系统首次启动会自动创建种子用户：

- **登录名**：zhangsan
- **密码**：P@ssw0rd123
- **租户**：cza 集团

---

## 十四、项目文档索引

| 文档 | 位置 | 说明 |
| --- | --- | --- |
| 后端架构设计 | 后端架构设计_v1.0.md | 整体架构、分层设计、状态机 |
| 登录方案 | 登录方案_v1.0.md | JWT 认证、密码哈希、令牌策略 |
| 核心接口文档 | 接口文档/ITSM一期核心接口文档_v1.0.md | 完整接口契约 |
| 接口产品分析 | 接口文档/ITSM一期核心接口产品分析与功能分类_v1.0.md | 功能域分类和优先级 |
| 项目计划书 | 计划书/ITSM桌面工单处理系统计划书.md | 项目背景、目标、范围 |
| UI 设计说明 | UI设计/UI信息架构与原型说明_v0.1.md | 页面结构、交互规范 |

---

## 十五、一期验收标准

### 15.1 功能验收

- [ ] 用户可以完成登录、打开"我的助手"、创建/读取会话并发送消息
- [ ] AI Agent 可以返回自助答复；无法解决时系统能创建工单并保留会话上下文
- [ ] 用户可以查看本人工单分页和详情，看到当前状态、处理进度和历史记录
- [ ] 客服可以查询本业务线或被分配范围内的工单，完成受理、分类、原因、解决方法和解决结果记录
- [ ] 工单可以按状态机完成"待受理 → 处理中 → 待用户确认 → 已解决 → 已关闭"，并支持用户重开
- [ ] 用户可以对 RESOLVED 或 CLOSED 工单提交 1-5 分评价，评价可追溯
- [ ] 管理员可以查询并维护一期字典项，停用项不能被新工单选择，历史工单仍可读取
- [ ] 前端可以查询当前用户角色、菜单、按钮和数据权限

### 15.2 技术验收

- [ ] 所有成功和失败响应均符合统一响应结构
- [ ] 统一错误码、HTTP 状态、traceId 和 details 可用于联调排错
- [ ] 所有关键动作均有状态历史、审计日志和对应领域事件
- [ ] 重复请求不会重复写入（幂等性保障）
- [ ] 多租户数据隔离，无跨租户数据泄露
- [ ] Agent 服务不可用时用户消息不丢失，前端可展示重试或转人工

---

## 十六、二期规划

### 16.1 知识库与 FAQ

- 构建企业 IT 知识库
- 支持 FAQ 检索和智能推荐
- AI Agent 集成 RAG 能力

### 16.2 SLA 管理

- 配置 SLA 时效规则
- 超时自动升级和告警
- SLA 达成率统计

### 16.3 统计报表

- 工单处理量统计
- 客服绩效分析
- 用户满意度趋势
- 运营看板

### 16.4 高级智能能力

- 更精细的智能分流策略
- 推荐处理方法
- 自动执行修复脚本（受控环境）

---

## 十七、附录

### 附录 A：错误码完整列表

| 错误码 | HTTP | 触发条件 |
| --- | --- | --- |
| SUCCESS | 200/201 | 请求成功 |
| VALIDATION_ERROR | 400 | 字段缺失、格式错误 |
| MISSING_HEADER | 400 | 缺少必要 Header |
| AUTH_REQUIRED | 401 | 未携带令牌 |
| TOKEN_EXPIRED | 401 | 令牌过期 |
| TOKEN_INVALID | 401 | 令牌签名或声明非法 |
| TENANT_REQUIRED | 400 | 缺少租户 ID |
| TENANT_FORBIDDEN | 403 | Header 租户与令牌不一致 |
| ROLE_FORBIDDEN | 403 | 角色没有接口权限 |
| DATA_SCOPE_FORBIDDEN | 403 | 无数据范围权限 |
| RESOURCE_NOT_FOUND | 404 | 资源不存在 |
| IDEMPOTENCY_CONFLICT | 409 | 幂等键冲突 |
| RESOURCE_CONFLICT | 409 | 资源已被他人更新 |
| ILLEGAL_STATE_TRANSITION | 409 | 状态不允许操作 |
| DICTIONARY_ITEM_DISABLED | 409 | 使用已停用字典项 |
| AGENT_UNAVAILABLE | 503 | AI 服务不可用 |
| SERVICE_UNAVAILABLE | 503 | 依赖服务不可用 |
| INTERNAL_ERROR | 500 | 未分类服务端异常 |

### 附录 B：字典类型枚举

| 字典类型 | 类型码 | 说明 |
| --- | --- | --- |
| 业务线 | BUSINESS_LINE | 工单分派目标 |
| 管理单元 | MANAGEMENT_UNIT | 问题场景分类 |
| 症状 | SYMPTOM | 问题表现 |
| 原因 | REASON | 问题根因 |
| 解决方法 | SOLUTION_METHOD | 处理方案 |
| 评价标签 | RATING_TAG | 满意度标签 |

### 附录 C：AI Agent 分类枚举

| 分类 | 分类码 | 说明 |
| --- | --- | --- |
| 系统问题 | SYSTEM | 操作系统、硬件相关 |
| 软件问题 | SOFTWARE | 应用软件相关 |
| 账号问题 | ACCOUNT | 登录、权限相关 |
| 网络问题 | NETWORK | 网络连接相关 |
| 外设问题 | PERIPHERAL | 外接设备相关 |
| 其他 | OTHER | 无法明确分类 |

---

> **文档说明**
>
> 本产品说明书覆盖 ITSM 桌面工单处理系统一期已交付的全部功能，包括 Java 后端服务和 Python AI Agent 服务。文档中的截图占位符（📌）需要替换为实际的产品截图。
>
> 如有疑问，请联系项目负责人。
