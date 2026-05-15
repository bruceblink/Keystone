# Keylo Token 集成计划

## 背景

Keystone 已接入 Keylo 作为可选身份中心，同时保留本地登录、会话 token、用户、角色、菜单、权限、部门和数据范围模型。

当前目标不是把 Keystone 的授权体系替换成 Keylo，而是让 Keystone 能识别 Keylo accessToken，并支持两类使用方式：

1. 通过 Keystone `/login` 完成登录，Keystone 后端调用 Keylo 凭证接口，再映射本地用户并签发 Keystone token。
2. 受保护接口直接携带 Keylo accessToken，Keystone 完成 Keylo JWT 校验后信任该 token，并构建临时 Keystone 安全上下文。

## 推荐方案

采用“双 token + Keylo bearer 兼容”模式：

1. Keystone 继续签发自己的 token，用于 Keystone 会话缓存、权限判断和数据范围控制。
2. Keylo 登录成功时，同时返回 Keylo accessToken，用于访问其它 Keylo 保护的服务。
3. Keystone 受保护接口同时接受 Keystone token 和 Keylo accessToken。
4. Keystone token 优先校验。
5. 如果 Keystone token 校验失败，并且 Keylo 已启用，再用 Keylo 校验同一个 bearer token。
6. Keylo 校验成功后，直接构建一个受信任的临时 `SystemLoginUser` 放入 Spring Security 上下文。

这样可以让 Keylo token 作为服务间调用凭证直接访问 Keystone，同时保留 Keystone 自有 token 和本地用户登录能力。

## Token 校验原理

Keystone 不会因为一个 token 的 payload 看起来像 Keylo token 就信任它。Keylo accessToken 必须通过完整的 JWT 校验链路：

1. **签名校验**：Keylo 使用私钥签发 JWT；Keystone 通过 Keylo 的 JWKS 公钥地址获取公钥并验证签名。伪造者没有 Keylo 私钥，无法生成可通过验签的 token。
2. **issuer 校验**：Keystone 校验 `iss` 是否等于配置的 Keylo issuer，避免接受其它认证中心签发的 token。
3. **audience 校验**：Keystone 校验 token 的 `aud` 是否命中 Keystone 配置的可信 audience 列表，避免其它服务的 Keylo token 被拿来调用 Keystone。
4. **时间校验**：Keystone 校验 `exp`、`nbf`、`iat` 等标准时间声明，过期 token 不能继续使用。
5. **身份 claim 提取**：以上校验全部通过后，Keystone 才会读取 `sub`、`uid` 和 `token_type` 等 claim。

注意：只 decode JWT payload 不是认证；decode 只能读取内容，不能证明 token 由 Keylo 签发。Keystone 当前使用 Spring Security `JwtDecoder` 完成验签和标准 claim 校验。

## 术语

Keystone 中统一使用以下术语：

| 术语 | 含义 |
| --- | --- |
| Keylo subject | Keylo token 的 `sub` claim，例如 `user:alice`、`client:admin`、`service:sync` |
| Keylo user ID | Keylo token 的 `uid` claim，也是 Keylo `users.id` |
| Keystone token | Keystone 自己签发的会话 token，用于本地登录后的后台会话 |
| Keylo bearer token | 直接放在 `Authorization: Bearer` 中调用 Keystone API 的 Keylo accessToken |
| Keystone external subject | `sys_user.external_subject`，用于 `/login` Keylo 用户映射时的 subject 兜底 |
| Keystone external user ID | `sys_user.external_user_id`，用于 `/login` Keylo 用户映射时的主映射 |

## 登录身份映射

`/login` 和 `/login/keylo` 仍然是 Keystone 登录入口。该路径会把 Keylo 身份映射为本地 `sys_user`，再签发 Keystone token。

不要混用 Keylo token subject 和 Keylo users 表主键。推荐映射：

| Keystone 字段 | 含义 | 来源 |
| --- | --- | --- |
| `sys_user.external_user_id` | 普通用户 token 的主映射键 | Keylo accessToken 的 `uid`；Keylo 创建用户响应里的 `id` |
| `sys_user.external_subject` | `/login` 场景下的 subject 兜底映射 | Keylo accessToken 的 `sub` |

规则：

- 普通用户 token：优先使用 `uid -> sys_user.external_user_id` 映射。
- 如果没有 `uid`，或按 `uid` 找不到本地用户，再使用 `sub -> sys_user.external_subject` 兜底映射。
- `external_user_id` 也用于后续 Keylo 用户管理操作，例如更新、禁用、删除和审计关联。
- 如果 Keylo 的 claim 或创建用户响应字段发生变化，需要通过配置显式调整。

默认配置：

```yaml
keystone:
  auth:
    keylo:
      base-url: ${KEYLO_BASE_URL:http://127.0.0.1:2345}
      subject-claim: ${KEYLO_SUBJECT_CLAIM:sub}
      user-id-claim: ${KEYLO_USER_ID_CLAIM:uid}
      audiences: ${KEYLO_AUDIENCES:admin-backend}
      provisioning:
        subject-field: ${KEYLO_SUBJECT_FIELD:sub}
        subject-template: ${KEYLO_SUBJECT_TEMPLATE:user:{username}}
        user-id-field: ${KEYLO_USER_ID_FIELD:id}
```

Keylo 用户 token 同时包含 `sub` 和 `uid`。根据 Keylo 项目的集成文档，`uid` 是稳定的 `users.id`，推荐用于用户关联和数据查询。Keystone 对普通用户遵循这个规则，同时保留 `sub` 用于主体语义和兜底映射。

Keylo 的 `POST /v1/admin/users` 返回用户记录，包含 `id`，但不一定返回 `sub`。因此 Keystone 会把响应里的 `id` 写入 `external_user_id`；如果响应里没有 `sub`，则通过 `subject-template` 推导 `external_subject`。当前默认模板是 `user:{username}`，与 Keylo 当前 token 签发规则一致。

## Audience 配置

Keylo token 的 `aud` 表示这个 token 面向哪个资源服务。Keystone 可能信任多个服务客户端或资源 audience，因此配置使用列表：

```yaml
keystone:
  auth:
    keylo:
      audiences:
        - admin-backend
        - keystone-admin
        - internal-service
```

环境变量也可以使用逗号分隔的列表：

```text
KEYLO_AUDIENCES=admin-backend,keystone-admin,internal-service
```

不再支持旧的单值 `KEYLO_AUDIENCE` 配置，统一使用 `KEYLO_AUDIENCES`，避免单值和列表同时存在时产生歧义。

## Keylo Bearer Token 兼容认证

服务、客户端或其它调用方访问 Keystone 受保护接口时，可以直接携带 Keylo accessToken：

```http
Authorization: Bearer <keylo_access_token>
```

Keystone 处理流程：

1. 先按 Keystone token 解析 bearer token。
2. Keystone token 解析失败时，如果 `keystone.auth.keylo.enabled=true`，再按 Keylo accessToken 校验。
3. `KeyloTokenVerifier` 校验签名、issuer、audience 和时间声明。
4. 校验通过后，读取 `sub` 作为临时主体名称。
5. 构建受信任的临时 `SystemLoginUser`，写入 Spring Security 上下文。
6. 该临时主体当前授予 `*:*:*` 和 ALL 数据范围。

当前实现不会再要求 Keylo bearer token 映射到本地 `sys_user`。也就是说，以下 token 只要完整校验通过，就可以直接调用 Keystone API：

```text
sub = service:<service_id>
aud = <KEYLO_AUDIENCES 中的任意值>
```

这是为了支持服务间调用和启动迁移阶段的 Keylo token 兼容。后续如需收缩权限，可将当前硬编码的 `*:*:*` 替换为配置或数据库驱动的 Keylo permission resolver，例如按 `sub`、`scope`、`role` 映射到 Keystone permission strings。

建议的权限收缩顺序：

1. 按 `sub` 配置服务 token 权限，例如 `service:sys_test -> monitor:server:list`。
2. 按 `scope` 映射权限，例如 `read -> system:user:list`。
3. 按 Keylo `role` 映射权限。
4. 没有任何映射时拒绝访问，而不是回退到 `*:*:*`。

## Keylo 凭证登录错误包装

`/login` 在 mixed 模式下由 Keystone 调用 Keylo 凭证校验接口。虽然底层认证由 Keylo 完成，但对前端和调用方而言，登录入口仍然是 Keystone，因此错误会包装成 Keystone 风格：

- Keylo 返回 `400`、`401` 或 `403`：统一包装为 `Business.LOGIN_WRONG_USER_PASSWORD`。
- Keylo 返回 `5xx`、网络异常或响应缺少 `access_token`：统一包装为 `Business.LOGIN_ERROR`，消息为“认证服务异常”。
- Keylo 原始 HTTP 状态和响应体只写入服务端日志，不直接暴露给前端。
- Keylo 配置缺失仍返回 `Business.LOGIN_KEYLO_CONFIG_MISSING`。

## 当前实现状态

当前 system 模块已完成：

- `TokenService#getTokenFromRequest` 暴露为公共方法，供认证过滤器复用 bearer token 提取逻辑。
- `TokenService#getLoginUserByTokenSilently` 支持 Keystone token 优先校验，并避免 fallback 场景产生无意义错误日志。
- `KeyloTokenIdentity` 统一承载 Keylo token 身份信息。
- `LoginService#buildLoginUserByKeyloIdentity` 负责 `/login` 和 `/login/keylo` 场景下从 Keylo 身份恢复 Keystone 本地 `SystemLoginUser`。
- `JwtAuthenticationTokenFilter` 支持 Keystone token 优先、Keylo accessToken fallback；fallback 成功后直接构建受信任临时主体，不再查询 `sys_user`。
- Keylo bearer token 临时主体使用 token `sub` 作为 username，使用负数 userId 避免和真实 `sys_user.user_id` 撞车，并授予 `*:*:*`。
- Keylo provisioning 将 Keylo 用户 ID 和 subject 分开存储：
  - `external_user_id = uid / users.id`
  - `external_subject = sub`
- Keylo 凭证登录失败会包装为 Keystone 错误码风格。
- 已补充 Keylo fallback、token claim 解析、provisioning 身份映射、Keylo bearer 临时主体和登录错误包装测试。

## 验证清单

1. 本地登录仍只返回 Keystone token，Keylo 字段为空。
2. Keylo 凭证登录返回 Keystone token 和 Keylo accessToken。
3. 使用 Keylo accessToken 直接调 Keystone 受保护接口时，只要签名、issuer、audience 和时间校验通过，即可进入 Keystone 安全上下文。
4. Keylo bearer 临时主体的 username 应为 token `sub`。
5. Keystone token 仍是优先路径。
6. Keylo token 的 `aud` 不在 `KEYLO_AUDIENCES` 中时，应拒绝访问。
7. `/login` Keylo 凭证登录失败时，应返回 Keystone 风格错误，不暴露 Keylo 原始响应。
8. 日志中不输出 Keylo token 明文。
