-- 工单挂起字段
ALTER TABLE ticket ADD COLUMN is_suspended TINYINT(1) NOT NULL DEFAULT 0 AFTER resolution_type;
ALTER TABLE ticket ADD COLUMN suspended_reason VARCHAR(500) NULL AFTER is_suspended;
ALTER TABLE ticket ADD COLUMN suspended_at DATETIME NULL AFTER suspended_reason;
