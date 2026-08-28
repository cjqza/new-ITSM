-- 会话参与者表：把转人工后的会话升级成“群”，支持员工 + 一个或多个客服成员
CREATE TABLE IF NOT EXISTS conversation_participant (
    participant_id    VARCHAR(64)  NOT NULL,
    tenant_id         VARCHAR(64)  NOT NULL,
    session_id        VARCHAR(64)  NOT NULL,
    user_id           VARCHAR(64)  NOT NULL,
    participant_type  VARCHAR(32)  NOT NULL COMMENT 'USER=员工, SUPPORT=客服',
    joined_at         DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (participant_id),
    UNIQUE KEY uk_participant_session_user (session_id, user_id),
    KEY idx_participant_tenant_user (tenant_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
