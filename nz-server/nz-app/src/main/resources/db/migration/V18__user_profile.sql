-- 个人中心：性别和受保护头像文件引用。
ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS gender VARCHAR(1) NOT NULL DEFAULT '2';
ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS avatar_file_id BIGINT;

ALTER TABLE sys_user DROP CONSTRAINT IF EXISTS chk_sys_user_gender;
ALTER TABLE sys_user ADD CONSTRAINT chk_sys_user_gender
    CHECK (gender IN ('0', '1', '2'));

CREATE INDEX IF NOT EXISTS idx_sys_user_tenant_avatar_file
    ON sys_user (tenant_id, avatar_file_id);
