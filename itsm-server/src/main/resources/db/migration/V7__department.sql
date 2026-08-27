-- 部门管理表
CREATE TABLE IF NOT EXISTS department (
    department_id   VARCHAR(64)  NOT NULL,
    tenant_id       VARCHAR(64)  NOT NULL,
    name            VARCHAR(200) NOT NULL,
    description     VARCHAR(500) NULL,
    enabled         TINYINT(1)   NOT NULL DEFAULT 1,
    created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (department_id),
    UNIQUE KEY uk_department_tenant_name (tenant_id, name),
    KEY idx_department_tenant_enabled (tenant_id, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
