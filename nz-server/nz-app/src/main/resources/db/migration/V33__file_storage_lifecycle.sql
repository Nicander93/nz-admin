-- 对齐早期基线列名和存储类型，保留原文件位置供管理员确认绑定。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='sys_file' AND column_name='file_suffix')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='sys_file' AND column_name='file_ext') THEN
        ALTER TABLE sys_file RENAME COLUMN file_suffix TO file_ext;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='sys_file' AND column_name='url')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='sys_file' AND column_name='file_url') THEN
        ALTER TABLE sys_file RENAME COLUMN url TO file_url;
    END IF;
END $$;
ALTER TABLE sys_file ALTER COLUMN storage_type DROP DEFAULT;
ALTER TABLE sys_file ALTER COLUMN storage_type TYPE VARCHAR(16) USING
    CASE storage_type::text WHEN '1' THEN 'local' WHEN '2' THEN 'oss' WHEN '3' THEN 's3' ELSE storage_type::text END;
ALTER TABLE sys_file ALTER COLUMN storage_type SET DEFAULT 'local';
ALTER TABLE sys_file ALTER COLUMN file_name TYPE VARCHAR(500);
ALTER TABLE sys_file ALTER COLUMN original_name TYPE VARCHAR(500);
ALTER TABLE sys_file ALTER COLUMN file_path TYPE VARCHAR(2048);
ALTER TABLE sys_file ALTER COLUMN file_url TYPE VARCHAR(2048);
ALTER TABLE sys_file ADD COLUMN IF NOT EXISTS mime_type VARCHAR(200);
ALTER TABLE sys_file ADD COLUMN IF NOT EXISTS uploader_id BIGINT;
ALTER TABLE sys_file ADD COLUMN IF NOT EXISTS uploader_name VARCHAR(200);
ALTER TABLE sys_file ADD COLUMN IF NOT EXISTS storage_config_snapshot TEXT;
ALTER TABLE sys_file ADD COLUMN IF NOT EXISTS lifecycle_status VARCHAR(24) NOT NULL DEFAULT 'READY';
ALTER TABLE sys_file ADD COLUMN IF NOT EXISTS cleanup_after TIMESTAMP;
ALTER TABLE sys_file ADD COLUMN IF NOT EXISTS cleanup_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE sys_file ADD COLUMN IF NOT EXISTS cleanup_error VARCHAR(500);
ALTER TABLE sys_file DROP CONSTRAINT IF EXISTS chk_sys_file_lifecycle;
ALTER TABLE sys_file ADD CONSTRAINT chk_sys_file_lifecycle
    CHECK (lifecycle_status IN ('UPLOADING', 'READY', 'DELETE_PENDING'));
CREATE INDEX IF NOT EXISTS idx_sys_file_cleanup ON sys_file (cleanup_after)
    WHERE lifecycle_status IN ('UPLOADING', 'DELETE_PENDING');
-- 旧记录缺少可信的历史位置，保留空快照，由管理员明确绑定，不能猜测为当前配置。

INSERT INTO sys_menu(parent_id,name,sort,type,perm,visible,status)
SELECT id,'上传',1912,'F','system:file:upload',0,0 FROM sys_menu
WHERE perm='system:file:list' AND NOT EXISTS(SELECT 1 FROM sys_menu WHERE perm='system:file:upload');
INSERT INTO sys_role_menu(tenant_id,role_id,menu_id)
SELECT r.tenant_id,r.id,m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_key='admin' AND m.perm='system:file:upload'
ON CONFLICT(role_id,menu_id) DO NOTHING;
INSERT INTO sys_tenant_package_menu(package_id,menu_id)
SELECT DISTINCT p.package_id,m.id FROM sys_tenant_package_menu p
JOIN sys_menu parent ON parent.id=p.menu_id AND parent.perm='system:file:list'
CROSS JOIN sys_menu m WHERE m.perm='system:file:upload'
ON CONFLICT(package_id,menu_id) DO NOTHING;
