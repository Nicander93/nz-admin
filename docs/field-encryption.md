# 用户联系方式加密

这一能力保护 `sys_user.email` 和 `sys_user.phone`。数据库写入使用 AES-256-GCM，接口默认只返回脱敏值；拥有独立权限的用户才能临时查看明文。

## 默认行为

- `nz.field-encryption.enabled=false` 时保持兼容，字段按原值读写。
- 启用后，新建和修改用户时自动加密邮箱、手机号。
- 存量明文在 `allow-plaintext-read=true` 时可以继续读取，便于灰度迁移。
- 列表和详情默认不返回 `email`、`phone`，只返回 `emailMasked`、`phoneMasked`。
- `system:user:contact:plain` 控制明文查看，`system:user:contact:encrypt` 控制当前租户批量重加密。
- 明文查看权限不会随 V10 自动授予角色或租户套餐，也不会在初始化数据时自动授予管理员。

## 密文格式

```text
ENC$1$<keyId>$<base64url(iv + ciphertext)>
```

`1` 是格式版本，`keyId` 用于找到历史密钥。每次写入生成 12 字节随机 IV，GCM 校验标签用于识别密文被篡改。相同明文多次写入会得到不同密文，因此邮箱和手机号不能直接做等值、模糊查询。

配置中的密钥材料先经过 SHA-256 派生为 256 位 AES 密钥。密钥材料至少 16 个字符，生产环境应使用密码管理服务生成和保管，不要提交到仓库。

## 首次启用

1. 备份数据库，执行 Flyway V10。未使用 Flyway 的环境执行 `nz-server/nz-app/src/main/resources/db/upgrade-p10-field-encryption.sql`。
2. 在租户套餐和运维角色中只分配 `system:user:contact:encrypt`。确实需要查看明文时，再单独分配 `system:user:contact:plain`。
3. 设置环境变量并重启：

```bash
export NZ_FIELD_ENCRYPTION_ENABLED=true
export NZ_FIELD_ENCRYPTION_ACTIVE_KEY_ID=v1
export NZ_FIELD_ENCRYPTION_KEY='replace-with-a-random-production-secret'
export NZ_FIELD_ENCRYPTION_ALLOW_PLAINTEXT=true
```

4. 切换到每个租户，进入用户管理，执行“重加密联系方式”。接口是 `PUT /api/system/user/contacts/re-encrypt`，只处理当前租户。
5. 检查数据库中非空邮箱、手机号都以 `ENC$1$v1$` 开头，再把 `NZ_FIELD_ENCRYPTION_ALLOW_PLAINTEXT` 改为 `false` 并重启。

启用状态下缺少活动密钥、密钥短于 16 个字符或发现无法验证的密文时，应用会直接报错，不会退回明文写入。

## 密钥轮换

轮换期间必须同时保留旧密钥和新密钥。例如活动密钥从 `v1` 换到 `v2`：

```bash
export NZ_FIELD_ENCRYPTION_ACTIVE_KEY_ID=v2
export NZ_FIELD_ENCRYPTION_KEY='old-v1-secret-kept-during-rotation'
export NZ_FIELD_ENCRYPTION_KEYS_V2='new-v2-production-secret'
```

重启后，旧的 `v1` 密文仍可读取，新写入使用 `v2`。逐租户执行重加密，确认数据库中已没有 `ENC$1$v1$` 后，才能移除旧密钥。不要先删除旧密钥，否则历史数据无法解密。

## 回滚

代码回滚前先保持所有历史密钥可用，并把 `allow-plaintext-read` 保持为 `true`。V10 只扩大列长度和新增权限，不应回退列宽；已经生成的密文不能由不含本 starter 的旧版本读取。需要回到不加密版本时，应先在受控离线任务中解密数据、验证备份，再切换旧应用。

操作日志会递归删除邮箱、手机号、密码、令牌和密钥字段。排查问题时只记录 keyId、租户 ID 和处理条数，不记录明文或完整密文。
