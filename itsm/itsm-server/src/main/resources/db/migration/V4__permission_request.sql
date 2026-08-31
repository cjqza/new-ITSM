CREATE TABLE IF NOT EXISTS permission_request (
    request_id   VARCHAR(64)  NOT NULL,
    tenant_id    VARCHAR(64)  NOT NULL,
    requester_id VARCHAR(64)  NOT NULL,
    request_type VARCHAR(32)  NOT NULL,
    status       VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    reason       VARCHAR(1000) NULL,
    approver_id  VARCHAR(64)  NULL,
    decided_at   DATETIME(3)  NULL,
    created_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (request_id),
    KEY idx_perm_req_tenant_status (tenant_id, status),
    KEY idx_perm_req_requester (requester_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
