-- 同事消息增加「已读时间」，让未读通知在刷新/跨端后依然持久化
ALTER TABLE colleague_message
    ADD COLUMN read_at DATETIME(3) NULL DEFAULT NULL COMMENT '已读时间，NULL 表示未读';

-- 历史消息一律视为已读，避免升级后一次性刷出大量「未读」角标
UPDATE colleague_message SET read_at = created_at;

CREATE INDEX idx_colleague_tenant_to_unread
    ON colleague_message (tenant_id, to_user_id, read_at);
