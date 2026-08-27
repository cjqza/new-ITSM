CREATE TABLE IF NOT EXISTS colleague_message (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id     VARCHAR(64)  NOT NULL,
    from_user_id  VARCHAR(64)  NOT NULL,
    to_user_id    VARCHAR(64)  NOT NULL,
    content       VARCHAR(2000) NOT NULL,
    created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_colleague_tenant_peer (tenant_id, from_user_id, to_user_id),
    KEY idx_colleague_tenant_to (tenant_id, to_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
