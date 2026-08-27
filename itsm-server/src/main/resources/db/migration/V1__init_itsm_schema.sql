CREATE TABLE IF NOT EXISTS tenant (
    tenant_id        VARCHAR(64)  NOT NULL,
    tenant_name      VARCHAR(200) NOT NULL,
    enabled          TINYINT(1)   NOT NULL DEFAULT 1,
    permissions_version VARCHAR(64) NOT NULL DEFAULT 'perm_20260825_01',
    auth_version     BIGINT       NOT NULL DEFAULT 1,
    created_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS app_user (
    user_id          VARCHAR(64)  NOT NULL,
    tenant_id        VARCHAR(64)  NOT NULL,
    display_name     VARCHAR(100) NOT NULL,
    department_name  VARCHAR(200) NULL,
    contact_phone    VARCHAR(50)  NULL,
    contact_email    VARCHAR(200) NULL,
    enabled          TINYINT(1)   NOT NULL DEFAULT 1,
    created_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id),
    KEY idx_app_user_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS user_credential (
    credential_id    VARCHAR(64)  NOT NULL,
    tenant_id        VARCHAR(64)  NOT NULL,
    user_id          VARCHAR(64)  NOT NULL,
    login_name       VARCHAR(100) NOT NULL,
    password_hash    VARCHAR(255) NOT NULL,
    password_algo    VARCHAR(32)  NOT NULL DEFAULT 'bcrypt',
    password_version BIGINT       NOT NULL DEFAULT 1,
    auth_version     BIGINT       NOT NULL DEFAULT 1,
    failed_count     INT          NOT NULL DEFAULT 0,
    locked_until     DATETIME(3)  NULL,
    last_login_at    DATETIME(3)  NULL,
    last_login_ip    VARCHAR(64)  NULL,
    status           VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    created_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (credential_id),
    UNIQUE KEY uk_credential_tenant_login (tenant_id, login_name),
    UNIQUE KEY uk_credential_tenant_user (tenant_id, user_id),
    KEY idx_credential_tenant_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS app_user_role (
    user_id          VARCHAR(64)  NOT NULL,
    role_id          VARCHAR(64)  NOT NULL,
    tenant_id        VARCHAR(64)  NOT NULL,
    created_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id, role_id),
    KEY idx_user_role_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS rbac_role (
    role_id          VARCHAR(64)  NOT NULL,
    tenant_id        VARCHAR(64)  NOT NULL,
    role_code        VARCHAR(64)  NOT NULL,
    role_name        VARCHAR(100) NOT NULL,
    enabled          TINYINT(1)   NOT NULL DEFAULT 1,
    description      VARCHAR(500) NULL,
    created_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (role_id),
    UNIQUE KEY uk_role_tenant_code (tenant_id, role_code),
    KEY idx_role_tenant_enabled (tenant_id, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS rbac_permission (
    permission_id    VARCHAR(64)  NOT NULL,
    tenant_id        VARCHAR(64)  NOT NULL,
    permission_code  VARCHAR(100) NOT NULL,
    permission_name  VARCHAR(100) NOT NULL,
    permission_type  VARCHAR(32)  NOT NULL,
    enabled          TINYINT(1)   NOT NULL DEFAULT 1,
    created_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (permission_id),
    UNIQUE KEY uk_permission_tenant_code (tenant_id, permission_code),
    KEY idx_permission_tenant_enabled (tenant_id, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS rbac_role_permission (
    role_id          VARCHAR(64)  NOT NULL,
    permission_id    VARCHAR(64)  NOT NULL,
    tenant_id        VARCHAR(64)  NOT NULL,
    created_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (role_id, permission_id),
    KEY idx_role_permission_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS conversation_session (
    session_id       VARCHAR(64)  NOT NULL,
    tenant_id        VARCHAR(64)  NOT NULL,
    user_id          VARCHAR(64)  NOT NULL,
    channel          VARCHAR(32)  NOT NULL,
    subject          VARCHAR(100) NULL,
    status           VARCHAR(32)  NOT NULL,
    summary          VARCHAR(2000) NULL,
    ticket_id        VARCHAR(64)  NULL,
    last_message_at  DATETIME(3)  NULL,
    created_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (session_id),
    KEY idx_session_tenant_user (tenant_id, user_id),
    KEY idx_session_tenant_status (tenant_id, status),
    KEY idx_session_ticket (ticket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS conversation_message (
    message_id       VARCHAR(64)  NOT NULL,
    tenant_id        VARCHAR(64)  NOT NULL,
    session_id       VARCHAR(64)  NOT NULL,
    sender_type      VARCHAR(32)  NOT NULL,
    sender_id        VARCHAR(64)  NULL,
    client_message_id VARCHAR(64) NULL,
    content          TEXT         NOT NULL,
    attachments_json JSON         NULL,
    created_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (message_id),
    UNIQUE KEY uk_message_tenant_client (tenant_id, client_message_id),
    KEY idx_message_session_created (session_id, created_at),
    KEY idx_message_tenant_session (tenant_id, session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS agent_decision (
    decision_id      VARCHAR(64)  NOT NULL,
    tenant_id        VARCHAR(64)  NOT NULL,
    session_id       VARCHAR(64)  NOT NULL,
    decision         VARCHAR(32)  NOT NULL,
    confidence       DECIMAL(5,4)  NOT NULL,
    business_line_code VARCHAR(64) NULL,
    summary          VARCHAR(2000) NULL,
    handoff_reason   VARCHAR(2000) NULL,
    suggested_management_unit_id VARCHAR(64) NULL,
    suggested_symptom_id VARCHAR(64) NULL,
    created_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (decision_id),
    KEY idx_agent_decision_session (tenant_id, session_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ticket (
    ticket_id        VARCHAR(64)  NOT NULL,
    tenant_id        VARCHAR(64)  NOT NULL,
    ticket_no        VARCHAR(64)  NOT NULL,
    source           VARCHAR(32)  NOT NULL,
    session_id       VARCHAR(64)  NULL,
    requester_id     VARCHAR(64)  NOT NULL,
    assignee_id      VARCHAR(64)  NULL,
    title            VARCHAR(200) NOT NULL,
    description      TEXT         NOT NULL,
    priority         VARCHAR(32)  NOT NULL,
    business_line_code VARCHAR(64) NULL,
    status           VARCHAR(32)  NOT NULL,
    environment       VARCHAR(500) NULL,
    attachments_json  JSON         NULL,
    resolution_summary TEXT         NULL,
    resolution_type   VARCHAR(64)  NULL,
    resolved_by       VARCHAR(64)  NULL,
    close_reason      VARCHAR(64)  NULL,
    closed_by         VARCHAR(64)  NULL,
    reopen_reason     VARCHAR(1000) NULL,
    reopened_by       VARCHAR(64)  NULL,
    version          BIGINT       NOT NULL DEFAULT 0,
    created_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    accepted_at      DATETIME(3)  NULL,
    resolved_at      DATETIME(3)  NULL,
    closed_at        DATETIME(3)  NULL,
    reopened_at      DATETIME(3)  NULL,
    PRIMARY KEY (ticket_id),
    UNIQUE KEY uk_ticket_tenant_no (tenant_id, ticket_no),
    KEY idx_ticket_tenant_status (tenant_id, status),
    KEY idx_ticket_tenant_requester (tenant_id, requester_id),
    KEY idx_ticket_tenant_assignee (tenant_id, assignee_id),
    KEY idx_ticket_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ticket_classification (
    ticket_id        VARCHAR(64)  NOT NULL,
    tenant_id        VARCHAR(64)  NOT NULL,
    management_unit_id VARCHAR(64) NULL,
    symptom_id       VARCHAR(64)  NULL,
    reason_id        VARCHAR(64)  NULL,
    solution_method_id VARCHAR(64) NULL,
    custom_reason    VARCHAR(1000) NULL,
    custom_solution  VARCHAR(2000) NULL,
    version          BIGINT       NOT NULL DEFAULT 0,
    created_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (ticket_id),
    KEY idx_ticket_classification_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ticket_status_history (
    history_id       VARCHAR(64)  NOT NULL,
    tenant_id        VARCHAR(64)  NOT NULL,
    ticket_id        VARCHAR(64)  NOT NULL,
    from_status      VARCHAR(32)  NULL,
    to_status        VARCHAR(32)  NOT NULL,
    operator_id      VARCHAR(64)  NOT NULL,
    operator_type    VARCHAR(32)  NOT NULL,
    action_type      VARCHAR(64)  NOT NULL,
    action_note      VARCHAR(2000) NULL,
    occurred_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (history_id),
    KEY idx_history_ticket_time (tenant_id, ticket_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ticket_action_log (
    action_log_id    VARCHAR(64)  NOT NULL,
    tenant_id        VARCHAR(64)  NOT NULL,
    ticket_id        VARCHAR(64)  NOT NULL,
    action_type      VARCHAR(64)  NOT NULL,
    operator_id      VARCHAR(64)  NOT NULL,
    operator_type    VARCHAR(32)  NOT NULL,
    action_content   TEXT         NULL,
    created_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (action_log_id),
    KEY idx_action_ticket_time (tenant_id, ticket_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS rating (
    rating_id        VARCHAR(64)  NOT NULL,
    tenant_id        VARCHAR(64)  NOT NULL,
    ticket_id        VARCHAR(64)  NOT NULL,
    requester_id     VARCHAR(64)  NOT NULL,
    score            INT          NOT NULL,
    tags_json        JSON         NULL,
    comment          VARCHAR(1000) NULL,
    created_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (rating_id),
    UNIQUE KEY uk_rating_ticket (tenant_id, ticket_id),
    KEY idx_rating_tenant_requester (tenant_id, requester_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS dictionary_item (
    item_id          VARCHAR(64)  NOT NULL,
    tenant_id        VARCHAR(64)  NOT NULL,
    dict_type        VARCHAR(64)  NOT NULL,
    code             VARCHAR(100) NOT NULL,
    name             VARCHAR(200) NOT NULL,
    parent_id        VARCHAR(64)  NULL,
    description       VARCHAR(500) NULL,
    enabled          TINYINT(1)   NOT NULL DEFAULT 1,
    sort_no          INT          NOT NULL DEFAULT 0,
    version          BIGINT       NOT NULL DEFAULT 1,
    disabled_reason  VARCHAR(1000) NULL,
    disabled_at      DATETIME(3)  NULL,
    disabled_by      VARCHAR(64)  NULL,
    created_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (item_id),
    UNIQUE KEY uk_dict_tenant_type_code (tenant_id, dict_type, code),
    KEY idx_dict_tenant_type_enabled (tenant_id, dict_type, enabled, sort_no),
    KEY idx_dict_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS audit_log (
    audit_id         VARCHAR(64)  NOT NULL,
    tenant_id        VARCHAR(64)  NOT NULL,
    operator_id      VARCHAR(64)  NOT NULL,
    operator_type    VARCHAR(32)  NOT NULL,
    action_type      VARCHAR(100) NOT NULL,
    resource_type    VARCHAR(64)  NOT NULL,
    resource_id      VARCHAR(64)  NOT NULL,
    request_trace_id  VARCHAR(64)  NULL,
    detail_json      JSON         NULL,
    created_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (audit_id),
    KEY idx_audit_tenant_time (tenant_id, created_at),
    KEY idx_audit_resource (resource_type, resource_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS auth_refresh_token (
    token_id         VARCHAR(64)  NOT NULL,
    tenant_id        VARCHAR(64)  NOT NULL,
    user_id          VARCHAR(64)  NOT NULL,
    token_hash       CHAR(64)     NOT NULL,
    expires_at       DATETIME(3)  NOT NULL,
    revoked_at       DATETIME(3)  NULL,
    replaced_by      VARCHAR(64)  NULL,
    auth_version     BIGINT       NOT NULL,
    created_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (token_id),
    UNIQUE KEY uk_refresh_token_hash (token_hash),
    KEY idx_refresh_lookup (tenant_id, user_id, expires_at),
    KEY idx_refresh_revoked (revoked_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS auth_login_audit (
    audit_id         VARCHAR(64)  NOT NULL,
    tenant_id        VARCHAR(64)  NOT NULL,
    user_id          VARCHAR(64)  NULL,
    login_name       VARCHAR(100) NULL,
    result           VARCHAR(32)  NOT NULL,
    reason           VARCHAR(255) NULL,
    client_ip        VARCHAR(64)  NULL,
    user_agent       VARCHAR(500) NULL,
    trace_id         VARCHAR(64)  NULL,
    created_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (audit_id),
    KEY idx_auth_audit_tenant_time (tenant_id, created_at),
    KEY idx_auth_audit_user_time (tenant_id, user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS idempotency_record (
    idempotency_id   VARCHAR(64)  NOT NULL,
    tenant_id        VARCHAR(64)  NOT NULL,
    caller_id        VARCHAR(64)  NOT NULL,
    method           VARCHAR(16)  NOT NULL,
    path             VARCHAR(255) NOT NULL,
    idempotency_key  VARCHAR(128) NOT NULL,
    request_hash     CHAR(64)     NOT NULL,
    response_code    VARCHAR(64)   NULL,
    response_body    JSON         NULL,
    created_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    expires_at       DATETIME(3)  NOT NULL,
    PRIMARY KEY (idempotency_id),
    UNIQUE KEY uk_idem_scope (tenant_id, caller_id, method, path, idempotency_key),
    KEY idx_idem_expire (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS outbox_event (
    event_id         VARCHAR(64)  NOT NULL,
    tenant_id        VARCHAR(64)  NOT NULL,
    event_type       VARCHAR(100) NOT NULL,
    aggregate_type   VARCHAR(64)  NOT NULL,
    aggregate_id     VARCHAR(64)  NOT NULL,
    payload_json     JSON         NOT NULL,
    event_status     VARCHAR(32)  NOT NULL DEFAULT 'NEW',
    occurred_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    published_at     DATETIME(3)  NULL,
    retry_count      INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (event_id),
    KEY idx_outbox_status_time (event_status, occurred_at),
    KEY idx_outbox_tenant_aggregate (tenant_id, aggregate_type, aggregate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO tenant (tenant_id, tenant_name, enabled)
VALUES ('cza', 'cza集团', 1)
ON DUPLICATE KEY UPDATE tenant_name = VALUES(tenant_name), enabled = VALUES(enabled);
