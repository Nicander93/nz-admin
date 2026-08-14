# 文件配置管理

文件配置管理位于“系统管理 / 文件配置”。它维护本地存储和 OSS 存储参数，并在启用配置后立即更新文件服务使用的 FileStorageProperties。已上传文件仍按文件记录中的 storageType 下载和删除，切换配置不会改变历史文件的存储归属。

## 操作规则

- 新建配置默认不启用。
- 同一时间只能有一个生效配置。
- 生效配置不能直接删除，需要先启用另一条配置。
- 编辑生效配置后立即应用。
- 编辑时 AccessKey ID 和 Secret 留空表示保留原值。
- 启用 OSS 前必须填写 endpoint、AccessKey ID、AccessKey Secret 和 bucket。
- 启用本地存储前必须填写 basePath。
- maxFileSizeBytes 必须大于 0。

接口前缀为 /api/system/file-config，使用以下权限：

- system:fileconfig:list
- system:fileconfig:query
- system:fileconfig:add
- system:fileconfig:edit
- system:fileconfig:remove

## 密钥

AccessKey Secret 使用 AES-GCM 加密后写入 sys_file_config。加密主密钥来自环境变量 NZ_FILE_CONFIG_KEY，对应配置 nz.file.config-encryption-key。

没有配置主密钥时，本地存储仍可使用，但保存新的 OSS Secret 会失败。接口不会返回 Secret 原文，只返回 accessKeySecretConfigured。AccessKey ID 在列表和详情中以掩码返回。

生产环境必须把 NZ_FILE_CONFIG_KEY 放入密钥管理系统或部署环境，不要提交到仓库。主密钥丢失后无法解密已有 OSS Secret。轮换主密钥前需要重新保存所有 OSS 配置。

## 数据库

Flyway V6 创建 sys_file_config、唯一生效配置索引和菜单权限。旧部署可执行 db/upgrade-p6-file-config.sql；Flyway 管理的部署不需要手工执行。

## 验证

    cd nz-server
    JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./mvnw -pl nz-framework/nz-starter-file,nz-module/nz-system,nz-app -am test

    cd ../nz-web
    node node_modules/vitest/vitest.mjs run tests/unit/views/system/file-config/hooks.test.ts
    node node_modules/typescript/bin/tsc --noEmit -p tsconfig.app.json
