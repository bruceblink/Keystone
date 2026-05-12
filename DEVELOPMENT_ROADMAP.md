# Keystone 开发计划

> 基于 2026-05-12 代码审查更新  
> 版本：3.0

---

## 一、当前状态

### 1.1 模块结构

```text
keystone-admin           # 管理后台启动模块、Controller、安全登录、Docker 运行入口
keystone-domain          # 业务应用服务、领域模型、MyBatis-Plus 数据服务
keystone-infrastructure  # 基础设施配置、缓存、过滤器、限流、Flyway、日志
keystone-common          # 通用 DTO、枚举、异常、工具类、文件工具、配置入口
```

`keystone-api` 模块已移除，当前系统入口集中在 `keystone-admin`。

### 1.2 技术与工程状态

| 项目 | 当前状态 |
|------|----------|
| Spring Boot | 3.5.13 |
| JDK | 开发/容器使用 JDK 25，编译目标 17 |
| 构建工具 | Gradle Wrapper，启用 configuration cache / build cache |
| 数据库 | MySQL 8.4，H2 测试库 |
| 缓存 | Redis + 本地缓存 |
| 认证 | Spring Security + JWT，支持 local / mixed / keylo-only |
| 数据迁移 | Flyway 依赖与迁移 Runner 已接入，默认 `spring.flyway.enabled=false` |
| 质量工具 | Checkstyle / SpotBugs / JaCoCo 已接入，当前以报告为主，不阻断构建 |
| 构建验证 | `./gradlew clean build -x test` 已通过 |

### 1.3 已实现能力

| 能力 | 状态 | 说明 |
|------|------|------|
| RBAC 管理 | 已完成 | 用户、角色、部门、岗位、菜单等 CRUD |
| 登录认证 | 已完成 | 本地账号、Keylo 凭证登录、Keylo token 兼容登录 |
| 用户同步注册 | 已完成 | 新增 Keystone 用户时可选同步注册 Keylo |
| 数据权限 | 已完成 | 多种数据范围检查器 |
| 操作日志 / 登录日志 | 已完成 | AOP + 异步任务 |
| 字典管理 | 已完成 | 字典类型与字典数据管理 |
| 文件上传下载 | 可用 | Controller 仍直接操作工具类和文件系统 |
| 限流 / 防重复提交 | 已完成 | 注解式能力 |
| Docker 本地环境 | 已完成 | MySQL、Redis、后端服务 |
| Swagger / OpenAPI | 已完成 | 可按环境关闭 |

### 1.4 测试状态

当前共有 42 个测试类，分布如下：

| 模块 | 测试类数量 | 覆盖重点 |
|------|------------|----------|
| keystone-admin | 9 | Keylo 登录、Swagger 关闭、数据权限检查器 |
| keystone-common | 12 | 工具类、异常、枚举、查询基类 |
| keystone-domain | 16 | 领域模型、用户新增、Keylo 用户注册、H2 集成测试 |
| keystone-infrastructure | 5 | 限流类型、缓存模板、MySQL 函数、鉴权工具 |

主要空白仍在：Controller MockMvc 覆盖、Role/Menu/Notice 等 ApplicationService 的业务分支覆盖、文件上传下载流程覆盖。

---

## 二、代码审查结论

### 2.1 优先风险

| 风险 | 位置 | 影响 | 建议 |
|------|------|------|------|
| 多处中文注释与 YAML 注释乱码 | `build.gradle`、`application-basic.yml`、`SecurityConfig`、`FileController`、`TokenService` 等 | 降低维护效率，也容易误读关键安全配置 | 统一恢复为 UTF-8 可读中文或精简英文注释 |
| `FileController` 直接处理文件读写和 DTO 组装 | `keystone-admin/.../FileController.java` | Controller 过重，难测，文件策略难扩展 | 新增 `FileApplicationService`，Controller 只负责 HTTP 适配 |
| 质量工具不阻断构建 | 根 `build.gradle` | Checkstyle / SpotBugs 问题可能长期堆积 | 先限定 production 代码逐步阻断，再扩展到 test |
| Flyway 默认关闭 | `application-basic.yml` | 部署依赖外部初始化，环境一致性风险仍在 | 设计迁移启用策略，先在 dev/test 打开并验证 |
| Token 与 RSA 默认密钥仍有兜底值 | `application-basic.yml` / Docker env | 误部署时可能使用弱默认密钥 | 生产 profile 启动时校验必填强密钥 |
| Keylo 兼容登录仍保留 | `LoginController#keyloLogin` | 历史接口增加认证面 | 设定废弃窗口和迁移完成后的删除版本 |

### 2.2 架构观察

- 当前模块依赖链为 `admin -> domain -> infrastructure -> common`，整体清晰。
- `common` 中仍有 `KeystoneConfig` 这类 Spring 配置入口，历史上迁移到 infrastructure 会引入循环依赖；短期不强迁，后续可以通过“配置属性接口 + infrastructure 绑定实现”的方式逐步解耦。
- `domain` 层已有较多应用服务，但部分服务仍偏事务脚本风格；先补测试，再谈重构。
- `ScheduleJobManager` 目前是孤立基础设施能力，若短期不做可视化定时任务，应避免对外承诺已可用。

---

## 三、近期开发计划（未来 4 周）

### Week 1：可维护性与安全基线

1. 修复核心配置与安全链路中的乱码注释。
   - 范围：`build.gradle`、`application-basic.yml`、`SecurityConfig`、`TokenService`、`LoginService`、`FileController`、`docker-compose.yml`
   - 验收：关键注释可读；`./gradlew clean build -x test` 通过。

2. 增加生产环境密钥校验。
   - 对 `TOKEN_SECRET`、RSA 私钥、Keylo 管理密钥等敏感配置增加启动期校验。
   - 生产环境不允许使用 `changeme`、空值、示例密钥。
   - 验收：prod profile 弱配置启动失败，dev/test 不被误伤。

3. 明确 Keylo token 兼容接口退役计划。
   - 在文档中标注 `/login/keylo` 保留窗口。
   - 增加配置开关或日志告警，统计是否仍被调用。

### Week 2：文件服务下沉

1. 新增 `FileApplicationService`。
   - 上传单文件、批量上传、下载路径校验与响应数据组装下沉到 domain 或 common-domain 协作层。
   - Controller 保持薄层：参数校验、ResponseDTO / ResponseEntity 返回。

2. 补充文件流程测试。
   - `FileApplicationService` 单元测试覆盖空文件、非法下载文件名、正常上传、批量上传。
   - `FileController` MockMvc 覆盖核心 HTTP 行为。

3. 梳理上传目录配置。
   - 确认 `KEYSTONE_FILE_BASE_DIR` 在本地、Docker、生产环境的一致行为。
   - 验收：路径穿越、非法扩展名、空文件都有明确错误码。

### Week 3：业务服务测试补强

1. 补 `RoleApplicationService` 测试。
   - 新增、更新、删除、角色状态、数据权限范围等关键分支。

2. 补 `MenuApplicationService` 测试。
   - 菜单树、路由生成、权限标识、可见状态。

3. 扩展 `UserApplicationService` 测试。
   - 当前已有新增用户相关测试，继续覆盖资料更新、密码重置、状态变更、删除保护、缓存失效。

4. 验收目标。
   - ApplicationService 关键路径具备单元测试。
   - `./gradlew test` 通过。

### Week 4：质量门禁与迁移策略

1. 开启分阶段质量门禁。
   - 第一阶段：`spotbugsMain` 对 production 代码阻断，test 仍 report-only。
   - 第二阶段：新增代码的 Checkstyle 问题不得增加。

2. Flyway 启用演练。
   - 在 dev/test 环境打开 `spring.flyway.enabled=true`。
   - 整理迁移脚本命名、baseline 与 H2/MySQL 的对应关系。
   - 验收：空库可通过 Flyway 初始化；已有库可 baseline 后迁移。

3. 更新 CI 建议。
   - PR 必跑：`./gradlew clean build -x test`、`./gradlew test`。
   - 后续加入覆盖率报告归档。

---

## 四、季度计划（1-3 个月）

### 4.1 通知中心与实时推送

目标：把现有 `sys_notice` 从 CRUD 提升为可用通知中心。

- 增加未读/已读状态、接收人、推送渠道字段。
- 新增未读数、批量已读、按用户查询接口。
- 引入 WebSocket/STOMP，JWT 握手鉴权。
- 多节点通过 Redis Pub/Sub 做广播。
- 验收：公告发布后在线用户可实时收到，离线用户登录后可拉取未读。

### 4.2 定时任务管理

目标：从孤立的 `ScheduleJobManager` 演进为可管理能力。

- 选型优先 Spring Quartz。
- 新增 `sys_job`、`sys_job_log` 表及 Flyway 迁移。
- API 覆盖列表、新增、修改、删除、启停、立即执行、日志查询。
- 验收：任务执行有审计日志，失败有错误信息，服务重启后任务状态可恢复。

### 4.3 可观测性

目标：达到生产基础监控可用。

- 引入 Spring Boot Actuator。
- 暴露 health、metrics、prometheus。
- 增加应用维度 tags：应用名、环境、版本。
- 日志增加 traceId/requestId 贯穿。
- 验收：Prometheus 可抓取指标，Grafana 可展示 JVM、HTTP、DB、Redis 基础面板。

### 4.4 权限与审计增强

- 为敏感操作增加二次确认或操作原因字段。
- 登录失败、Keylo 调用失败、权限拒绝增加结构化审计字段。
- 梳理 Druid、Swagger、Actuator 在 prod profile 下的暴露面。

---

## 五、长期规划（3-12 个月）

### 5.1 多租户能力

适用于 SaaS 化场景，建议在明确业务需求后启动。

- 基于 MyBatis-Plus TenantLine 做列隔离。
- 新增租户、套餐、租户管理员初始化流程。
- 全局表白名单：配置、字典、系统级菜单等。
- 风险：缓存 key、登录态、数据权限都需要带租户维度。

### 5.2 工作流引擎

适用于审批类业务明确后引入。

- 候选方案：Flowable。
- 先做独立 POC：流程定义、任务列表、审批、撤回、历史记录。
- 避免在没有业务流程前过早集成。

### 5.3 插件化扩展点

- 通知渠道插件：邮件、企业微信、钉钉、Webhook。
- 登录认证插件：本地、Keylo、OIDC。
- 文件存储插件：本地、S3/MinIO。

---

## 六、优先级总览

| 优先级 | 任务 | 难度 | 价值 | 建议时间 |
|--------|------|------|------|----------|
| P0 | 修复关键文件乱码注释 | 低 | 维护效率 | Week 1 |
| P0 | 生产弱密钥启动校验 | 中 | 安全基线 | Week 1 |
| P0 | FileController 下沉与测试 | 中 | 架构与可测性 | Week 2 |
| P1 | ApplicationService 测试补强 | 中 | 回归保障 | Week 3 |
| P1 | production SpotBugs 门禁 | 中 | 缺陷前移 | Week 4 |
| P1 | Flyway dev/test 启用演练 | 中 | 部署一致性 | Week 4 |
| P2 | WebSocket 通知中心 | 中 | 用户体验 | 本季度 |
| P2 | Quartz 定时任务管理 | 中 | 运维能力 | 本季度 |
| P2 | Actuator + Prometheus | 中 | 生产可观测 | 本季度 |
| P3 | 多租户 | 高 | SaaS 化 | 长期 |
| P3 | Flowable 工作流 | 高 | 审批业务 | 长期 |

---

## 七、执行准则

- 每个功能先补关键测试，再做较大重构。
- 不再新增独立对外 API 模块，除非有明确边界与部署需求。
- 生产 profile 默认安全：Swagger、Druid、Actuator 暴露面必须显式收敛。
- 配置优先环境变量注入，示例默认值只服务本地开发。
- 数据库结构变化必须通过 Flyway 脚本进入版本管理。
- 新增跨模块能力时保持依赖方向：`admin -> domain -> infrastructure -> common`。
