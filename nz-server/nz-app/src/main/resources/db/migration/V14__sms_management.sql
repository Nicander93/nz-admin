-- 短信渠道、模板、发送记录与管理权限。
CREATE TABLE IF NOT EXISTS sys_sms_channel (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    channel_code VARCHAR(64) NOT NULL,
    channel_name VARCHAR(100) NOT NULL,
    provider_code VARCHAR(64) NOT NULL,
    endpoint VARCHAR(500),
    access_key_id VARCHAR(256),
    access_key_secret VARCHAR(2000),
    signature VARCHAR(128),
    status SMALLINT NOT NULL DEFAULT 0,
    remark VARCHAR(500),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_sms_channel_status CHECK (status IN (0, 1))
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sms_channel_tenant_code
    ON sys_sms_channel (tenant_id, channel_code);

CREATE TABLE IF NOT EXISTS sys_sms_template (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    channel_id BIGINT NOT NULL REFERENCES sys_sms_channel(id),
    template_code VARCHAR(100) NOT NULL,
    template_name VARCHAR(100) NOT NULL,
    provider_template_id VARCHAR(200),
    content VARCHAR(1000) NOT NULL,
    status SMALLINT NOT NULL DEFAULT 0,
    remark VARCHAR(500),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_sms_template_status CHECK (status IN (0, 1))
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sms_template_tenant_channel_code
    ON sys_sms_template (tenant_id, channel_id, template_code);

CREATE TABLE IF NOT EXISTS sys_sms_send_log (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    channel_id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    phone_number VARCHAR(1024) NOT NULL,
    template_code VARCHAR(100) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    request_params TEXT,
    send_status VARCHAR(16) NOT NULL,
    provider_message_id VARCHAR(256),
    error_message VARCHAR(500),
    send_time TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_sms_send_status CHECK (send_status IN ('PENDING', 'SUCCESS', 'FAILED'))
);
CREATE INDEX IF NOT EXISTS idx_sms_log_tenant_status_time
    ON sys_sms_send_log (tenant_id, send_status, create_time DESC);

INSERT INTO sys_sms_channel (
    tenant_id, channel_code, channel_name, provider_code, status, remark
)
SELECT 1, 'local-log', '本地日志渠道', 'log', 0, '开发环境默认渠道，不会调用外部短信服务'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_sms_channel WHERE tenant_id = 1 AND channel_code = 'local-log'
);

INSERT INTO sys_sms_template (
    tenant_id, channel_id, template_code, template_name, content, status, remark
)
SELECT 1, channel.id, 'verification-code', '验证码',
       '您的验证码是 {{code}}，5 分钟内有效。', 0, '系统内置测试模板'
FROM sys_sms_channel channel
WHERE channel.tenant_id = 1 AND channel.channel_code = 'local-log'
  AND NOT EXISTS (
    SELECT 1 FROM sys_sms_template template
    WHERE template.tenant_id = 1
      AND template.channel_id = channel.id
      AND template.template_code = 'verification-code'
  );

UPDATE sys_menu SET sort = 14 WHERE perm = 'system:tenantpackage:list';
UPDATE sys_menu SET sort = 15 WHERE perm = 'system:tenant:list';

INSERT INTO sys_menu (parent_id, name, path, component, icon, sort, type, perm, visible, status)
SELECT parent.id, '短信管理', 'sms', 'system/sms/index', 'ChatDotRound', 13,
       'C', 'system:sms:list', 0, 0
FROM sys_menu parent
WHERE parent.path = '/system'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perm = 'system:sms:list');

INSERT INTO sys_menu (parent_id, name, sort, type, perm, visible, status)
SELECT menu.id, button.name, button.sort, 'F', button.perm, 0, 0
FROM sys_menu menu
CROSS JOIN (
    VALUES
        ('查询', 1, 'system:sms:query'),
        ('新增', 2, 'system:sms:add'),
        ('修改', 3, 'system:sms:edit'),
        ('删除', 4, 'system:sms:remove'),
        ('测试发送', 5, 'system:sms:send')
) AS button(name, sort, perm)
WHERE menu.perm = 'system:sms:list'
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu child
    WHERE child.parent_id = menu.id AND child.perm = button.perm
  );

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT role.id, menu.id
FROM sys_role role
JOIN sys_menu menu ON menu.perm IN (
    'system:sms:list', 'system:sms:query', 'system:sms:add',
    'system:sms:edit', 'system:sms:remove', 'system:sms:send'
)
WHERE role.role_key = 'admin'
ON CONFLICT DO NOTHING;
