-- 与 V15__sms_login.sql 保持同步，供未接入 Flyway 的旧部署人工升级。
ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS phone_hash VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_sys_user_tenant_phone_hash
    ON sys_user (tenant_id, phone_hash);

INSERT INTO sys_client (
    client_id, client_name, login_type, token_timeout, status, remark
) VALUES
    ('nz-web-account', '管理端账号登录', 'account', 7200, 0, '管理端默认账号密码客户端'),
    ('nz-web-sms', '管理端短信登录', 'sms', 7200, 0, '管理端默认短信验证码客户端')
ON CONFLICT (client_id) DO NOTHING;

ALTER TABLE sys_client DROP CONSTRAINT IF EXISTS chk_sys_client_login_type;
ALTER TABLE sys_client ADD CONSTRAINT chk_sys_client_login_type
    CHECK (login_type IN ('account', 'sms', 'social'));
