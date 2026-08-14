# 第三方账号登录与绑定

## 能力边界

第三方账号能力由两层组成：

- `nz-starter-social` 负责标准 OAuth2/OIDC 协议、PKCE、一次性 state、令牌交换和身份解析。
- `nz-system` 负责租户、登录客户端、系统用户、账号绑定、菜单权限和登录会话。

系统只把第三方稳定用户标识及公开资料保存到 `sys_social`。第三方的 access token、refresh token 和 ID token 只在回调请求内使用，不写入数据库，也不返回浏览器。

## 登录流程

1. 登录页读取 `GET /api/auth/social/providers`，只展示已启用服务商。
2. 浏览器提交租户编码、`nz-web-social` 客户端和服务商到 `POST /api/auth/social/authorize`。
3. 服务端校验租户和客户端，生成 PKCE verifier、challenge 及五分钟有效的一次性 state。
4. 浏览器跳转到服务商授权页，再返回 `/oauth/callback/{provider}`。
5. 回调页把 code、state 和 provider 交给 `POST /api/auth/social/callback`。
6. 服务端读取并删除 state，交换令牌，校验 OIDC ID Token 或读取 OAuth2 userinfo。
7. `sys_social` 中存在绑定时创建 Sa-Token 登录会话；未绑定时明确拒绝登录。

state 在读取时立即删除。令牌交换失败后不能重复提交同一个回调，需要重新发起授权。

## 绑定与解绑

登录后进入“系统管理 / 第三方账号”：

- `GET /api/system/social/list`：查看当前用户绑定。
- `POST /api/system/social/authorize/{provider}`：发起绑定。
- `DELETE /api/system/social/{bindingId}`：解除当前用户自己的绑定。

绑定回调仍使用公开回调地址，但服务端会核对 state 中的用户、租户和当前 Sa-Token 会话。浏览器不能借助绑定 state 给其他用户或其他租户写入关系。

每个租户内，同一服务商身份只能绑定一个系统用户；一个系统用户对同一服务商也只能保留一个绑定。

## GitHub 配置

GitHub 示例默认关闭。先在 GitHub OAuth App 中把回调地址配置为：

```text
http://localhost:5173/oauth/callback/github
```

再设置环境变量：

```bash
export NZ_SOCIAL_GITHUB_ENABLED=true
export NZ_SOCIAL_GITHUB_CLIENT_ID=your-client-id
export NZ_SOCIAL_GITHUB_CLIENT_SECRET=your-client-secret
export NZ_SOCIAL_GITHUB_REDIRECT_URI=http://localhost:5173/oauth/callback/github
```

生产环境必须把回调地址改为前端的 HTTPS 地址，并在服务商控制台使用完全相同的地址。

GitHub 的公开用户接口可能不返回邮箱。当前实现不会额外请求邮箱列表，这不影响身份绑定和登录。

## 通用 OIDC 配置

其他标准 OIDC 服务商可以在 `nz.social.providers` 下增加配置：

```yaml
nz:
  social:
    providers:
      company-oidc:
        enabled: true
        display-name: Company SSO
        protocol: oidc
        client-id: client-id
        client-secret: client-secret
        client-authentication-method: client_secret_basic
        authorization-uri: https://id.example.com/oauth2/authorize
        token-uri: https://id.example.com/oauth2/token
        issuer-uri: https://id.example.com
        user-info-uri: https://id.example.com/oauth2/userinfo
        redirect-uri: https://admin.example.com/oauth/callback/company-oidc
        scopes:
          - openid
          - profile
          - email
```

OIDC 模式必须配置 `issuer-uri` 或 `jwk-set-uri`。服务端会校验签名、有效时间、issuer 和 audience。非标准 OAuth 服务商需要实现适配器，不能通过关闭 ID Token 校验来绕过协议差异。

## 多节点部署

默认的 `InMemorySocialAuthorizationStateStore` 适合单节点和本地开发。多实例部署时，同一次授权的开始请求与回调可能落到不同节点，必须提供共享的 `SocialAuthorizationStateStore` Bean，例如 Redis 实现，并保持“读取即删除”和 TTL 语义。

在共享 state 存储落地前，不应把默认实现用于无会话粘滞的多节点生产环境。

## 数据库升级

Flyway 会自动执行 `V16__social_login.sql`。关闭 Flyway 的既有环境使用：

```text
nz-server/nz-app/src/main/resources/db/upgrade-p16-social-login.sql
```

迁移会创建 `sys_social`、`nz-web-social` 客户端以及查看、绑定、解绑权限。

## 验证

后端定向测试：

```bash
cd nz-server
./mvnw -pl nz-framework/nz-starter-social,nz-module/nz-system,nz-app -am test
```

前端验证：

```bash
cd nz-web
pnpm test
pnpm build
```

真实服务商联调还需要有效客户端凭据和外网回调地址，自动化测试不会调用外部 OAuth 服务。
