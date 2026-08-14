-- 第三方 OAuth2/OIDC 登录、账号绑定与菜单权限。
CREATE TABLE IF NOT EXISTS sys_social (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    user_id BIGINT NOT NULL REFERENCES sys_user(id) ON DELETE CASCADE,
    provider VARCHAR(64) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    username VARCHAR(200),
    nickname VARCHAR(200),
    email VARCHAR(320),
    avatar VARCHAR(1000),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_social_tenant_identity
    ON sys_social (tenant_id, provider, provider_user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_social_tenant_user_provider
    ON sys_social (tenant_id, user_id, provider);
CREATE INDEX IF NOT EXISTS idx_sys_social_tenant_user
    ON sys_social (tenant_id, user_id);

INSERT INTO sys_client (
    client_id, client_name, login_type, token_timeout, status, remark
) VALUES (
    'nz-web-social', '管理端第三方账号登录', 'social', 7200, 0,
    '管理端默认 OAuth2/OIDC 登录客户端'
)
ON CONFLICT (client_id) DO NOTHING;

INSERT INTO sys_menu (parent_id, name, path, component, icon, sort, type, perm, visible, status)
SELECT parent.id, '第三方账号', 'social', 'system/social/index', 'Link', 16,
       'C', 'system:social:list', 0, 0
FROM sys_menu parent
WHERE parent.path = '/system'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perm = 'system:social:list');

INSERT INTO sys_menu (parent_id, name, sort, type, perm, visible, status)
SELECT menu.id, button.name, button.sort, 'F', button.perm, 0, 0
FROM sys_menu menu
CROSS JOIN (
    VALUES
        ('绑定账号', 1, 'system:social:bind'),
        ('解除绑定', 2, 'system:social:remove')
) AS button(name, sort, perm)
WHERE menu.perm = 'system:social:list'
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu child
    WHERE child.parent_id = menu.id AND child.perm = button.perm
  );

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT role.id, menu.id
FROM sys_role role
JOIN sys_menu menu ON menu.perm IN (
    'system:social:list', 'system:social:bind', 'system:social:remove'
)
WHERE role.role_key = 'admin'
ON CONFLICT DO NOTHING;
