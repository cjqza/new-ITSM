ALTER TABLE app_user
    ADD UNIQUE KEY uk_app_user_tenant_phone (tenant_id, contact_phone);
