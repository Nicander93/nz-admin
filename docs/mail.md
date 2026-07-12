# 邮件能力

邮件能力由独立的 `nz-starter-mail` 提供，默认关闭。业务模块只依赖 `MailService` 协议，SMTP 和 JavaMail 细节不进入业务代码。

## 启用配置

```yaml
nz:
  mail:
    enabled: true
spring:
  mail:
    host: smtp.example.com
    port: 587
    username: no-reply@example.com
    password: ${SMTP_PASSWORD}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
```

发件人默认使用 `spring.mail.username`；也可以通过 `spring.mail.properties.from` 覆盖。未启用或缺少 `JavaMailSender` 时，不会注册 `MailService` 和测试接口。

## 管理端测试

- 页面：系统管理 / 邮件测试
- 接口：`POST /api/system/mail/test`
- 权限：`system:mail:test`
- 菜单迁移：`nz-app/src/main/resources/db/upgrade-p5-mail.sql`

生产环境应使用环境变量或密钥服务注入 SMTP 密码，不要提交明文密码。测试页用于验证配置，不承担邮件模板和群发职责。

## 验证

```bash
cd nz-server
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./mvnw -pl nz-framework/nz-starter-mail,nz-module/nz-system -am test

cd ../nz-web
pnpm vitest run tests/unit/views/system/mail/hooks.test.ts
pnpm exec vue-tsc --noEmit -p tsconfig.app.json
```