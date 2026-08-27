# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

ITSM (IT Service Management) backend — a multi-module Maven project on **Java 17** and **Spring Boot 4.1.1** (jakarta namespace). Source comments/Javadoc are written in Chinese.

Stack: Spring Web / Security / JDBC / Data Redis / AMQP, MyBatis-Plus 3.5.17, Flyway, MySQL, jjwt 0.12.6, springdoc-openapi + Knife4j, Hutool, Lombok.

Three modules (declared in the root aggregator `pom.xml`):

- `itsm-common` — shared API contract only: `ApiResponse`, `PageResponse`, `BusinessException`, `ErrorCode`. No web dependency.
- `itsm-pojo` — MyBatis-Plus entities (`@TableName`, Lombok) and DTOs (Java records nested in per-domain classes like `TicketDtos`). Depends on `itsm-common`.
- `itsm-server` — the runnable Spring Boot app: controllers, services, config, mappers, JdbcTemplate repositories, Flyway migrations, `application.yaml`.

## Build / Run / Test

Use the system `mvn` (no Maven wrapper is checked in).

```bash
# Build all modules (runs tests)
mvn clean verify

# Build, skipping tests
mvn clean package -DskipTests

# Run the server (port 8080; needs MySQL database `itsm`)
mvn -pl itsm-server spring-boot:run

# Run a single test class / method
mvn -pl itsm-server -Dtest=ItsmServerApplicationTests test
mvn -pl itsm-server -Dtest=ItsmServerApplicationTests#contextLoads test
```

There is currently only one test — a `@SpringBootTest` context-load test. It disables seed data via `@TestPropertySource(properties = "itsm.auth.seed.enabled=false")`.

Runtime defaults live in `itsm-server/src/main/resources/application.yaml`: MySQL `jdbc:mysql://localhost:3306/itsm` (root/1234), Flyway enabled (`classpath:db/migration`, `baseline-on-migrate` + `validate-on-migrate`), Swagger UI at `/swagger-ui.html`, api-docs at `/v3/api-docs`. A seeded dev login is `tenant_001` / `zhangsan` / `P@ssw0rd123` (configurable under `itsm.auth.seed.*`).

## Architecture

### Request path and tenant model

Every business table carries `tenant_id`; the tenant is selected by the `X-Tenant-Id` header, but identity is never trusted from client headers.

1. `SecurityConfig` registers a JWT `OncePerRequestFilter` that parses the `Authorization: Bearer <access-token>` and writes `auth.userId`, `auth.tenantId`, `auth.roles`, `auth.permissionsVersion`, `auth.authVersion` as request attributes (invalid tokens are silently ignored here).
2. Controllers extend `ControllerSupport` and call `context(request, tenantId)` → `RequestContextHolder.resolve(...)`, which reads those attributes, verifies the header tenant matches the token tenant, and returns a `RequestContext` record (or throws `AUTH_REQUIRED`).
3. Services take `RequestContext` and enforce role/data-scope checks inline. Roles are plain strings: `USER`, `SUPPORT_AGENT`, `SUPPORT_ADMIN`, `SUPERVISOR` (see `TicketService` for the canonical pattern).

### Two persistence styles coexist

- **Auth subsystem** uses plain JDBC: immutable records in `itsm-server/.../domain/` + `@Repository` classes under `itsm-server/.../repository/` using `JdbcTemplate` and text-block SQL (no MyBatis).
- **Business subsystem** uses MyBatis-Plus: entities in `itsm-pojo/.../entity/` + mapper interfaces in `itsm-server/.../infrastructure/persistence/mapper/` extending `BaseMapper<T>`. There are no MyBatis XML mappers; queries are built with `LambdaQueryWrapper` in services.

### Layering and response envelope

- Layers: `api` (controllers) → `service` → `infrastructure/persistence/mapper` (or `repository`) / `domain` / `infrastructure` (`JsonSupport`, `AuditService`).
- All endpoints return `ApiResponse<T>` (`code`, `message`, `data`, `traceId`, `details`). Expected domain errors are raised as `BusinessException` with an `ErrorCode` (HTTP status + business code); `GlobalExceptionHandler` renders them, plus validation and generic errors. `X-Trace-Id` is read from the request and echoed back.

### Entity / DTO conventions

- Entity base hierarchy (in `itsm-pojo/.../entity/base/`): `TenantScopedEntity` → `TenantCreatedEntity` → `TenantCreatedUpdatedEntity` → `TenantCreatedUpdatedVersionEntity` (adds `@Version` optimistic lock). All use chainable generic setters.
- Entities use Lombok `@Getter @Setter @Accessors(chain = true)`, `@TableName`, and `@TableId` string PKs (e.g. `tkt_<uuid>`).
- DTOs are one `final` class per domain (e.g. `TicketDtos`, `AuthDtos`) containing nested `record`s with Swagger `@Schema` annotations. `auth` DTOs instead live in `itsm-server/.../dto/` as standalone records.

### Auth flow

JWT HS256 access token (claims include `tenant_id`, `roles`, `perm_ver`, `auth_ver`, `token_type=access`) plus a refresh token that is a random UUID string stored only as its SHA-256 hash and rotated on every refresh. `auth_version` on `user_credential` invalidates outstanding tokens. Passwords are BCrypt; lockout is driven by `failed_count` / `locked_until` and `itsm.auth.login-*` settings. `AuthSeedInitializer` (conditional `CommandLineRunner`) seeds tenant/user/credential/roles.

### Database schema

Flyway migrations in `itsm-server/src/main/resources/db/migration/` define the schema (`V1__init_itsm_schema.sql`, `V2__auth_columns_and_seed.sql`). Tables cover: tenant, app_user, user_credential, RBAC (`app_user_role`, `rbac_role`, `rbac_permission`, `rbac_role_permission`), conversation (`conversation_session`, `conversation_message`, `agent_decision`), ticket (`ticket`, `ticket_classification`, `ticket_status_history`, `ticket_action_log`, `rating`), `dictionary_item`, `audit_log`, `auth_refresh_token`, `auth_login_audit`, `idempotency_record`, `outbox_event`.

The ticket lifecycle is a state machine implemented directly in `TicketService`: `PENDING_ACCEPTANCE → IN_PROGRESS → PENDING_USER_CONFIRM → RESOLVED → CLOSED`, with `REOPENED` as a branch, each transition guarded by role + current status.

## Gotchas

- The root `pom.xml` is only an **aggregator** (`packaging=pom`); the three child modules do **not** inherit from it. Each child declares `spring-boot-starter-parent` 4.1.1 as its own parent and version `0.0.1-SNAPSHOT` (root is `1.0-SNAPSHOT`). Inter-module dependencies reference `com.cenziang:itsm-common:0.0.1-SNAPSHOT` / `itsm-pojo:0.0.1-SNAPSHOT` — keep those versions aligned when adding modules.
- Flyway `validate-on-migrate` is on: do not edit an already-applied migration; add a new `Vn__...sql` file instead.
- Spring Boot 4 uses the **jakarta** namespace (`jakarta.servlet.*`, `jakarta.validation.*`), not `javax.*`.
- MyBatis-Plus mappers are picked up via `@MapperScan("com.cenziang.itsmserver.infrastructure.persistence.mapper")` on `ItsmServerApplication`; place new mappers there.
- `application.yaml` contains dev-only credentials and a dev JWT secret — don't copy them into prod configs.
