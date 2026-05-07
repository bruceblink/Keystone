# Keylo token integration plan

## Context

Keystone 已接入 Keylo 作为身份中心，但登录完成后返回给前端的是 Keystone 自己签发的会话 token，而不是 Keylo token。这样会导致一个现实问题：用户虽然是通过 Keylo 认证进来的，但拿 Keystone token 去调用其他只认 Keylo token 的服务会失败。

与此同时，Keystone 自身的权限与数据范围模型又深度依赖本地 `SystemLoginUser`、本地角色/部门/权限装配，以及 `@PreAuthorize` 和 `@dataScope` 这套链路，因此不能贸然直接切成“纯 Keylo token 驱动”。目标是在不破坏 Keystone 现有权限模型的前提下，先解决跨服务 token 互认问题，并为后续扩展保留平滑演进空间。

## Recommended approach

采用“双 token”方案作为当前与中期演进的主路线：

1. Keystone 继续签发自己的 token，专门用于 Keystone 内部接口鉴权、权限判断、数据范围控制。
2. Keylo 登录成功时，把 Keylo access token 一并返回给前端，用于访问其他只接受 Keylo token 的服务。
3. 保持 Keystone 本地授权模型不变，不在这一阶段把 Keystone 改造成直接以 Keylo JWT 驱动整个权限上下文。
4. 后续如果整个系统确定全面统一到 Keylo，再单独规划“Keystone 直接接受并解析 Keylo token”的架构升级，而不是在当前问题修复中顺手重构认证体系。

## Why this is the better extension path

### Why dual-token is better now

- **兼容现状最好**：Keystone 现有的本地权限、菜单、数据范围逻辑都不用推翻。
- **能立刻解决跨服务访问问题**：前端拿到 Keylo token 后，可以直接访问其他 Keylo 保护服务。
- **演进成本最低**：只改登录返回模型和 Keylo 登录链路，不需要重写 Keystone 的安全过滤器与权限恢复流程。
- **中期最稳**：即使后面接更多服务，也能继续沿用“Keystone token 管 Keystone，自带 Keylo token 管外部服务”的边界。

### When pure Keylo token becomes better

只有在以下条件都满足时，统一 Keylo token 才是更好的长期架构：

- Keystone 内外所有服务都统一以 Keylo 为唯一资源服务器；
- Keystone 愿意把每次请求的本地权限上下文从 Keylo subject 动态还原，而不是继续依赖当前 Keystone session token + Redis 登录态；
- 团队能接受一次较大的认证与鉴权重构，以及随之而来的完整回归测试成本。

结论：

- **当前问题修复与近期扩展**：双 token 更好。
- **全域统一身份与资源访问的最终态**：纯 Keylo token 可能更好，但那是后续架构项目，不是当前最优修复。

## Files to modify

- `keystone-admin/src/main/java/app/keystone/admin/customize/service/login/LoginService.java`
- `keystone-admin/src/main/java/app/keystone/admin/customize/service/login/keylo/KeyloCredentialVerifier.java`
- `keystone-admin/src/main/java/app/keystone/admin/customize/service/login/keylo/KeyloPrincipal.java`
- `keystone-admin/src/main/java/app/keystone/admin/controller/common/LoginController.java`
- `keystone-domain/src/main/java/app/keystone/domain/common/dto/TokenDTO.java`
- `keystone-admin/src/test/java/app/keystone/admin/customize/service/login/LoginServiceKeyloLoginTest.java`

## Implementation details

### 1. Extend Keylo login result carrier

当前 `KeyloPrincipal` 只有 `subject`，不够承载双 token 场景。应扩展它，使其能表达：

- `subject`
- `accessToken`
- 如 Keylo 响应中存在，可选承载 `refreshToken`、`expiresIn`、`tokenType`

这样 `KeyloCredentialVerifier` 和 `/login/keylo` 这两条链路都可以统一回传“外部身份认证结果 + 外部 token”。

### 2. Preserve Keylo access token in credential verification

在 `keystone-admin/src/main/java/app/keystone/admin/customize/service/login/keylo/KeyloCredentialVerifier.java` 中：

- 当前已经从响应里取到了 `access_token`，但只拿它去调用 `/me`，没有继续向上返回。
- 修改为返回包含 `subject + accessToken (+ refreshToken/expiresIn/tokenType)` 的对象。
- 注意不要在日志里输出 token 明文。

### 3. Refactor LoginService return model

在 `keystone-admin/src/main/java/app/keystone/admin/customize/service/login/LoginService.java` 中：

- 当前 `login(...)` / `keyloLogin(...)` 返回 `String`，仅表示 Keystone token。
- 改为返回一个内部登录结果对象，至少包含：
  - `keystoneToken`
  - `keyloAccessToken`，仅 Keylo 登录时有值
  - 可选 `keyloRefreshToken` / `expiresIn` / `tokenType`
- 本地登录模式下只填 Keystone token；Keylo 登录模式下同时填 Keystone token 与 Keylo token。
- `buildTokenByKeyloSubject(...)` 仍然负责本地用户映射、构建 `SystemLoginUser`、生成 Keystone token，不改动当前权限装配逻辑。

### 4. Extend TokenDTO for dual-token response

在 `keystone-domain/src/main/java/app/keystone/domain/common/dto/TokenDTO.java` 中：

- 保留现有 `token` 字段作为 Keystone token，避免前端现有 Keystone 调用逻辑被破坏。
- 新增可选字段，例如：
  - `keyloAccessToken`
  - `keyloRefreshToken`
  - `keyloTokenType`
  - `keyloExpiresIn`
- 当前建议保留 `token` 不动，只新增 Keylo 相关字段，保持兼容。

### 5. Update LoginController response assembly

在 `keystone-admin/src/main/java/app/keystone/admin/controller/common/LoginController.java` 中：

- `POST /login` 与 `POST /login/keylo` 读取新的登录结果对象。
- `currentUser` 仍从本地 `AuthenticationUtils.getSystemLoginUser()` 派生，保持现有前端用户信息结构。
- 最终返回 `TokenDTO`，包含 Keystone token、currentUser、以及可选的 Keylo token 字段。

### 6. Update tests

更新 `keystone-admin/src/test/java/app/keystone/admin/customize/service/login/LoginServiceKeyloLoginTest.java`：

- Keylo 登录成功时，断言同时生成 Keystone token 与 Keylo access token 字段。
- 本地登录路径断言仍只返回 Keystone token，Keylo 字段为空。
- 若已有 controller 层测试，也补充返回 JSON 结构断言，确保兼容旧前端字段。

## Existing code/patterns to reuse

- `LoginService.java` 中现有 `buildTokenByKeyloSubject(...)`，继续复用它来构建本地 Keystone 登录态。
- `TokenService.createTokenAndPutUserInCache(...)`，继续作为 Keystone 内部 token 的唯一签发入口。
- `KeyloCredentialVerifier.java` 已经取到 `access_token`，只需要把它向上返回，不需要新建 Keylo 登录基础流程。
- `KeyloTokenVerifier.java` 已经能校验客户端传入的 Keylo token，可在 `/login/keylo` 链路中继续复用。

## Verification

1. 本地登录模式：`/login` 返回 `token + currentUser`，Keylo 相关字段为空；Keystone 原有受保护接口访问正常。
2. Keylo 凭证登录模式：`/login` 返回 `token + currentUser + keyloAccessToken`；Keystone 接口仍使用 `token` 正常访问。
3. 使用返回的 `keyloAccessToken` 调用其他 Keylo 保护服务，验证能够被识别。
4. `/login/keylo` 兼容链路返回同样的双 token 结构。
5. 检查日志与异常输出，确认不会打印 Keylo token 明文。
6. 运行 admin 模块相关测试与编译，确认现有权限和数据范围逻辑未回归。
