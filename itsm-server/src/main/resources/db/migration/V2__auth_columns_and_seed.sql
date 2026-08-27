INSERT INTO tenant (tenant_id, tenant_name, enabled, permissions_version, auth_version)
VALUES ('cza', 'cza集团', 1, 'perm_20260825_01', 1)
ON DUPLICATE KEY UPDATE tenant_name = VALUES(tenant_name), enabled = VALUES(enabled), permissions_version = VALUES(permissions_version), auth_version = VALUES(auth_version);