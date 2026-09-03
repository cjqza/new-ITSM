# ITSM 桌面工单处理系统

基于 Spring Boot + LangGraph AI 的智能 IT 服务管理平台，支持 AI 自动接待、工单全生命周期管理、多角色协作和满意度评价闭环。
## 具体说明书请移步https://ccn9ofeiasjk.feishu.cn/wiki/D5ndw3IELiRY1ukodUWcM3uJnVf

## 技术栈

### Java 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.4.5 | 应用框架 |
| Java | 17 | 开发语言 |
| MyBatis-Plus | 3.5.17 | 持久层框架 |
| Flyway | - | 数据库版本管理 |
| MySQL | 8.0 | 主数据库 |
| Redis | 6+ | 缓存、幂等键、短期状态 |
| RabbitMQ | 3.8+ | 消息队列（可选） |
| Spring Security + JWT | jjwt 0.12.6 | 安全认证 |
| WebSocket | - | 实时消息推送 |
| Knife4j + SpringDoc | - | API 文档 |
| Hutool / Lombok / Pinyin4j | - | 工具库 |

### Python AI Agent

| 技术 | 版本 | 用途 |
|------|------|------|
| Python | 3.12+ | 开发语言 |
| FastAPI | 0.115+ | Web 框架 |
| LangGraph | 0.2+ | AI 工作流编排 |
| LangChain | 0.3+ | LLM 应用框架 |
| DashScope (通义千问) | - | 大模型 API |

## 项目结构

```
ITSM/
├── pom.xml                   # Maven 父 POM
├── itsm-common/              # 通用基础设施：统一响应、错误码、异常处理、分页封装
├── itsm-pojo/                # 领域模型：实体、DTO、枚举、租户隔离基类
├── itsm-server/              # 主应用：Controller、Service、Mapper、配置、数据库迁移
│   └── src/main/
│       ├── java/com/cenziang/itsmserver/
│       │   ├── api/          # REST 控制器（17 个）
│       │   ├── config/       # Spring 配置（安全、MyBatis、WebSocket、Flyway 等）
│       │   ├── domain/       # 领域对象（Auth、Tenant、Role 等）
│       │   ├── dto/          # 请求/响应 DTO
│       │   ├── service/      # 业务逻辑层（20+ 个 Service）
│       │   ├── repository/   # 数据访问层
│       │   ├── infrastructure/ # 基础设施（审计、持久化 Mapper）
│       │   └── websocket/    # WebSocket 处理
│       └── resources/
│           ├── db/migration/ # Flyway SQL 迁移脚本（V1~V9）
│           └── static/       # 前端静态页面
├── ai-agent/                 # Python AI Agent 服务
│   ├── agent.py              # LangGraph 工作流定义（分类→诊断→决策）
│   ├── server.py             # FastAPI 服务入口
│   ├── requirements.txt      # Python 依赖
│   └── start.ps1             # 启动脚本
├── UI设计/                   # UI 信息架构与原型
├── 接口文档/                 # 核心接口文档
├── 计划书/                   # 项目计划书
└── 产品说明书/               # 最终产品说明书
```

## 核心功能

### AI 智能接待

- AI Agent 基于 LangGraph 三节点工作流：分类（classify）→ 诊断（diagnose）→ 决策（decide）
- 自动识别 6 类问题（系统/软件/账号/网络/外设/其他），生成结构化诊断报告
- 置信度低于 0.5 或涉及硬件故障等场景自动转人工
- 通过 HTTP 集成 Java 后端，AI 不可用时自动降级

### 用户端

- 手机号注册/登录，与 AI 智能客服实时聊天
- 主动或 AI 触发转人工，自动创建工单
- 查看工单状态和处理进度，确认解决结果或重开
- 服务评价与满意度反馈（1~5 分）

### 客服端

- 工单台式工作面板（待处理 / 即将超时 / 草稿箱 / 待本岗位处理）
- 受理工单、补齐分类（管理单元、症状、原因、解决方法）
- 提交解决结果，用户确认后关闭工单

### 管理员端

- 字典管理（业务线、管理单元、症状、原因、解决方法、评价标签）
- 权限查询、员工管理、配置审计

### 通用能力

- 多租户数据隔离
- RBAC 权限控制（功能权限 / 菜单权限 / 数据权限）
- 工单状态机流转（7 个状态）
- 操作审计日志、幂等写入保障

## 工单状态流转

```
新建(NEW) → 待受理(PENDING_ACCEPTANCE) → 处理中(IN_PROGRESS)
    → 待用户确认(PENDING_USER_CONFIRM) → 已解决(RESOLVED) → 已关闭(CLOSED)
                                                          ↘ 重开(REOPENED) → 处理中
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.9+
- MySQL 8.0+
- Redis 6+
- Python 3.12+（AI Agent）

### 启动 Java 后端

```bash
# 构建项目
mvn clean install

# 启动应用（数据库表通过 Flyway 自动迁移）
cd itsm-server
mvn spring-boot:run
```

数据库默认连接配置在 `itsm-server/src/main/resources/application.yaml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/itsm?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: 1234
```

### 启动 Python AI Agent

```bash
cd ai-agent
pip install -r requirements.txt

# 配置环境变量
export DASHSCOPE_API_KEY="your-api-key"

# 启动服务
python server.py
```

或使用 PowerShell：
```powershell
.\start.ps1
```

### 访问系统

| 服务 | 地址 |
|------|------|
| 前端页面 | http://localhost:8080 |
| 用户端聊天 | http://localhost:8080/user-portal.html |
| API 文档 (Swagger) | http://localhost:8080/swagger-ui.html |
| API 文档 (Knife4j) | http://localhost:8080/doc.html |
| AI Agent 健康检查 | http://localhost:8090/api/v1/ai/health |

### 默认账号

- 登录名：`zhangsan`
- 密码：`P@ssw0rd123`
- 租户：`cza集团`

## API 概览

| 模块 | 路径前缀 | 说明 |
|------|----------|------|
| 认证 | `/api/v1/auth` | 登录、注册、令牌刷新、验证码 |
| 工单 | `/api/v1/tickets` | 建单、查询、确认、重开 |
| 客服工单 | `/api/v1/support/tickets` | 队列、受理、分类、解决、关闭 |
| 会话 | `/api/v1/conversations` | 创建会话、发消息、转人工 |
| Agent | `/api/v1/agent` | Agent 决策编排 |
| 评价 | `/api/v1/ratings` | 工单评价提交与查询 |
| 字典 | `/api/v1/admin/dictionaries` | 字典项查询、新增、更新、停用 |
| 权限 | `/api/v1/permissions` | 角色权限管理 |
| 员工 | `/api/v1/admin/employees` | 员工信息查询 |
| 部门 | `/api/v1/admin/departments` | 部门信息管理 |
| 通讯录 | `/api/v1/contacts` | 联系人查询 |
| 同事消息 | `/api/v1/colleagues/messages` | 内部消息通信 |
| AI Agent | `/api/v1/ai` | 智能对话、健康检查 |

## 数据库迁移

使用 Flyway 管理数据库版本，迁移脚本位于 `itsm-server/src/main/resources/db/migration/`：

| 版本 | 说明 |
|------|------|
| V1 | 初始化核心表结构 |
| V2 | 认证字段与种子数据 |
| V3 | 用户手机号唯一约束 |
| V4 | 权限申请表 |
| V5 | 同事消息表 |
| V6 | 消息已读状态 |
| V7 | 部门表 |
| V8 | 会话参与者表 |
| V9 | 工单挂起功能 |

## 项目文档

- [产品说明书](./产品说明书/ITSM桌面工单处理系统最终产品说明书_V1.0.md)
- [后端架构设计](./后端架构设计_v1.0.md)
- [登录方案](./登录方案_v1.0.md)
- [核心接口文档](./接口文档/ITSM一期核心接口文档_v1.0.md)
- [接口产品分析](./接口文档/ITSM一期核心接口产品分析与功能分类_v1.0.md)
- [项目计划书](./计划书/ITSM桌面工单处理系统计划书.md)
- [UI 设计说明](./UI设计/UI信息架构与原型说明_v0.1.md)

## License

MIT
