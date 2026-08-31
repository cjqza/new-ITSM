# ITSM 桌面工单处理系统

基于 Spring Boot 4 的 IT 服务管理（ITSM）系统，支持用户自助提单、客服受理处理、工单状态流转、评价闭环等核心功能。

## 技术栈

- **后端框架**: Spring Boot 4.1.1 + Java 17
- **持久层**: MyBatis-Plus 3.5.17 + Flyway 数据库版本管理
- **数据库**: MySQL 8
- **缓存**: Redis
- **消息队列**: RabbitMQ (Spring AMQP)
- **安全认证**: Spring Security + JWT (jjwt 0.12.6)
- **实时通信**: WebSocket
- **API 文档**: Knife4j + SpringDoc OpenAPI 3
- **工具库**: Hutool、Lombok、Pinyin4j

## 项目结构

```
itsm/
├── itsm-common/          # 通用基础设施：统一响应、错误码、异常处理、分页封装
├── itsm-pojo/            # 领域模型：实体、DTO、枚举
├── itsm-server/          # 主应用：Controller、Service、Mapper、配置、数据库迁移
│   ├── src/main/java/com/cenziang/itsmserver/
│   │   ├── api/          # REST 控制器
│   │   ├── config/       # Spring 配置（安全、MyBatis、WebSocket、Flyway 等）
│   │   ├── domain/       # 领域对象（Auth、Tenant、Role 等）
│   │   ├── dto/          # 请求/响应 DTO
│   │   ├── service/      # 业务逻辑层
│   │   ├── repository/   # 数据访问层
│   │   ├── infrastructure/ # 基础设施（审计、持久化 Mapper）
│   │   └── websocket/    # WebSocket 处理
│   └── src/main/resources/
│       ├── db/migration/ # Flyway SQL 迁移脚本（V1~V9）
│       └── static/       # 前端静态页面
└── pom.xml               # 父 POM
```

## 核心功能

### 用户端
- 手机号 + 验证码注册登录
- 在线提单（选择业务线、描述问题）
- 查看工单状态和处理进度
- 工单评价与满意度反馈

### 客服端
- 工单队列管理与受理
- 工单分类（管理单元、症状、原因、解决方法）
- 提交解决结果、关闭工单
- 同事消息与内部沟通

### 通用能力
- 多租户隔离
- RBAC 权限控制（角色、菜单、按钮、数据范围）
- 工单状态机流转（新建 -> 待受理 -> 处理中 -> 待确认 -> 已解决 -> 已关闭 / 重开）
- 字典管理（业务线、管理单元、症状、原因等可配置项）
- 操作审计日志
- 幂等写入保障

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
- RabbitMQ 3.8+（可选，异步事件用）

### 数据库配置

默认连接配置在 `itsm/itsm-server/src/main/resources/application.yaml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/itsm?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: 1234
```

数据库表通过 Flyway 自动迁移，首次启动时会自动创建。

### 启动应用

```bash
cd itsm
mvn clean install
cd itsm-server
mvn spring-boot:run
```

应用启动后访问：
- 前端页面: http://localhost:8080
- API 文档: http://localhost:8080/swagger-ui.html
- Knife4j 文档: http://localhost:8080/doc.html

### 默认账号

系统首次启动会自动创建种子用户（可通过配置关闭）：
- 登录名: `zhangsan`
- 密码: `P@ssw0rd123`
- 租户: `cza集团`

## 数据库迁移

项目使用 Flyway 管理数据库版本，迁移脚本位于 `itsm/itsm-server/src/main/resources/db/migration/`：

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

## API 概览

| 模块 | 路径前缀 | 说明 |
|------|----------|------|
| 认证 | `/api/auth` | 登录、注册、令牌刷新、验证码 |
| 工单 | `/api/tickets` | 建单、查询、受理、解决、关闭、重开 |
| 会话 | `/api/conversations` | 创建会话、发消息、查询历史 |
| 评价 | `/api/ratings` | 工单评价提交与查询 |
| 字典 | `/api/dictionaries` | 业务线、症状、原因等字典管理 |
| 员工 | `/api/employees` | 员工信息查询 |
| 部门 | `/api/departments` | 部门信息管理 |
| 权限 | `/api/permissions` | 角色权限管理 |
| 通讯录 | `/api/contacts` | 联系人查询 |
| 同事消息 | `/api/colleague-messages` | 内部消息通信 |

## 项目文档

- [后端架构设计](./后端架构设计_v1.0.md)
- [登录方案](./登录方案_v1.0.md)
- [核心接口文档](./接口文档/ITSM一期核心接口文档_v1.0.md)
- [接口产品分析](./接口文档/ITSM一期核心接口产品分析与功能分类_v1.0.md)
- [项目计划书](./计划书/ITSM桌面工单处理系统计划书.md)
- [UI 设计说明](./UI设计/UI信息架构与原型说明_v0.1.md)

## License

MIT
