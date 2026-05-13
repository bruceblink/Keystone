# Keylo Token 集成计划

## 背景

Keystone 已接入 Keylo 作为可选身份中心，但 Keystone 仍保留本地授权模型，包括用户、角色、菜单、权限、部门和数据范围。

当前目标不是把 Keystone 的授权体系替换成 Keylo，而是让 Keystone 能识别 Keylo accessToken，并在识别成功后恢复 Keystone 本地用户权限上下文。

## 推荐方案

采用“双 token”模式：

1. Keystone 继续签发自己的 token，用于 Keystone 会话缓存、权限判断和数据范围控制。
2. Keylo 登录成功时，同时返回 Keylo accessToken，用于访问其它 Keylo 保护的服务。
3. Keystone 受保护接口同时接受 Keystone token 和 Keylo accessToken。
4. Keystone token 优先校验。
5. 如果 Keystone token 校验失败，并且 Keylo 已启用，再用 Keylo 校验同一个 bearer token。
6. Keylo 校验成功后，Keystone 将 token 身份映射到本地 `sys_user`，再构建正常的 `SystemLoginUser`。

这样可以保持 Keystone 现有的 `@PreAuthorize`、菜单权限、角色、部门和数据范围逻辑不变。

## 术语

Keystone 中统一使用以下术语：

| 术语 | 含义 |
| --- | --- |
| Keylo subject | Keylo token 的 `sub` claim，例如 `user:alice`、`client:admin`、`service:sync` |
| Keylo user ID | Keylo token 的 `uid` claim，也是 Keylo `users.id` |
| Keystone external subject | `sys_user.external_subject`，用于 Keylo subject 兜底映射，以及服务账号、客户端账号映射 |
| Keystone external user ID | `sys_user.external_user_id`，用于普通 Keylo 用户 token 的主映射 |

## 身份映射

不要混用 Keylo token subject 和 Keylo users 表主键。

推荐映射：

| Keystone 字段 | 含义 | 来源 |
| --- | --- | --- |
| `sys_user.external_user_id` | 普通用户 token 的主映射键 | Keylo accessToken 的 `uid`；Keylo 创建用户响应里的 `id` |
| `sys_user.external_subject` | subject 映射和服务 token 兜底映射 | Keylo accessToken 的 `sub` |

规则：

- 普通用户 token：优先使用 `uid -> sys_user.external_user_id` 映射。
- 服务 token / admin client token：Keylo 不提供 `uid`，使用 `sub -> sys_user.external_subject` 映射。
- `external_user_id` 也用于后续 Keylo 用户管理操作，例如更新、禁用、删除和审计关联。
- 如果 Keylo 的 claim 或创建用户响应字段发生变化，需要通过配置显式调整。

默认配置：

```yaml
keystone:
  auth:
    keylo:
      subject-claim: ${KEYLO_SUBJECT_CLAIM:sub}
      user-id-claim: ${KEYLO_USER_ID_CLAIM:uid}
      provisioning:
        subject-field: ${KEYLO_SUBJECT_FIELD:sub}
        subject-template: ${KEYLO_SUBJECT_TEMPLATE:user:{username}}
        user-id-field: ${KEYLO_USER_ID_FIELD:id}
```

Keylo 用户 token 同时包含 `sub` 和 `uid`。根据 Keylo 项目的集成文档，`uid` 是稳定的 `users.id`，推荐用于用户关联和数据查询。Keystone 对普通用户遵循这个规则，同时保留 `sub` 用于主体语义、审计和服务账号映射。

Keylo 的 `POST /v1/admin/users` 返回用户记录，包含 `id`，但不一定返回 `sub`。因此 Keystone 会把响应里的 `id` 写入 `external_user_id`；如果响应里没有 `sub`，则通过 `subject-template` 推导 `external_subject`。当前默认模板是 `user:{username}`，与 Keylo 当前 token 签发规则一致。

## 服务客户端认证

服务客户端调用 Keystone 时，直接携带 Keylo accessToken：

```http
Authorization: Bearer <keylo_access_token>
```

Keystone 处理流程：

1. 校验 Keylo accessToken。
2. 提取配置指定的 `sub` 和 `uid` claim。
3. 如果存在 `uid`，先按 `uid -> sys_user.external_user_id` 查找本地用户。
4. 如果没有 `uid`，或按 `uid` 没有找到本地用户，再按 `sub -> sys_user.external_subject` 查找。
5. 找到本地用户后，构建 `SystemLoginUser`。
6. 后续权限继续走 Keystone 本地角色和权限体系。

对于机器客户端，需要在 Keystone 中创建一个本地服务账号，并将它的 `external_subject` 设置为 Keylo 服务主体，例如：

```text
service:<service_id>
client:<client_id>
```

服务账号应分配专用角色，不建议设置为超级管理员。

## 当前实现状态

当前 system 模块已完成：

- `TokenService#getTokenFromRequest` 暴露为公共方法，供认证过滤器复用 bearer token 提取逻辑。
- `TokenService#getLoginUserByTokenSilently` 支持 Keystone token 优先校验，并避免 fallback 场景产生无意义错误日志。
- `KeyloTokenIdentity` 统一承载 Keylo token 身份信息。
- `LoginService#buildLoginUserByKeyloIdentity` 负责从 Keylo 身份恢复 Keystone 本地 `SystemLoginUser`。
- `JwtAuthenticationTokenFilter` 支持 Keystone token 优先、Keylo accessToken fallback。
- Keylo provisioning 将 Keylo 用户 ID 和 subject 分开存储：
  - `external_user_id = uid / users.id`
  - `external_subject = sub`
- 已补充 Keylo fallback、本地权限恢复、token claim 解析和 provisioning 身份映射测试。

## 验证清单

1. 本地登录仍只返回 Keystone token，Keylo 字段为空。
2. Keylo 凭证登录返回 Keystone token 和 Keylo accessToken。
3. 使用 Keylo 用户 accessToken 调 Keystone 受保护接口时，可通过 `uid` 映射到本地用户。
4. 使用 Keylo 服务 token / admin client token 调 Keystone 时，可通过 `sub` 映射到本地服务账号。
5. Keystone token 仍是优先路径。
6. Keylo token 既没有可映射 `uid`，也没有可映射 `sub` 时，应拒绝访问。
7. 本地映射用户被禁用时，应拒绝访问。
8. 日志中不输出 Keylo token 明文。
