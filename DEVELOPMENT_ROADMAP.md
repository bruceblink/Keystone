# Keystone 开发路线图

> 基于 2026-04-19 完整代码深度审查制定  
> 版本：2.0

---

## 一、项目现状总结

### 1.1 技术栈（实测版本）

| 组件 | 版本 |
|------|------|
| Spring Boot | 3.5.13 |
| Spring Security | 6.x |
| Java 编译目标 | 17（开发环境 JDK 25）|
| MyBatis-Plus | 3.5.5 |
| Druid | 1.2.x |
| Hutool | 5.8.40 |
| JJWT | 0.12.6 |
| SpringDoc OpenAPI | 2.x |
| JUnit Jupiter | 5.12.2 |
| MySQL | 8.4（Docker）|
| Redis | 8.6.2（Docker）|

### 1.2 模块结构

```txt
keystone-admin          # 管理后台接口（11个Controller）
keystone-api            # 对外开放API（3个Controller，几乎为空）
keystone-domain         # DDD领域模型（10个业务域）
keystone-infrastructure # 基础设施（Security/Cache/Filter/Schedule等）
keystone-common         # 通用工具与常量
```

### 1.3 已实现功能

| 功能模块 | 状态 | 说明 |
|---------|------|------|
| RBAC 用户/角色/权限/部门/岗位/菜单 | ✅ 完整 | 11个Controller，完整CRUD |
| JWT 无状态认证 + Token 自动刷新 | ✅ 完整 | Redis 存储，30分钟过期 |
| 方法级权限（@PreAuthorize） | ✅ 完整 | 数据权限+功能权限 |
| 操作日志 / 登录日志 / AOP 切面 | ✅ 完整 | MethodLogAspect + 异步写入 |
| 双层缓存（Guava 本地 + Redis 分布式） | ✅ 完整 | CacheCenter 统一管理 |
| 动态多数据源 + Druid 监控 | ✅ 完整 | MySQL + PostgreSQL 双驱动 |
| Excel 导入导出（EasyExcel） | ✅ 完整 | 用户/角色/部门 |
| 文件上传下载 | ✅ 完整 | 路径安全校验 |
| 数据字典管理（sys_dict_type/data） | ✅ 完整 | v3.2.0 新增，双层缓存 |
| 验证码（数学题） | ✅ 完整 | |
| 限流注解（@RateLimit） | ✅ 完整 | Redis/Local 两种策略 |
| 防重复提交（@Unrepeatable） | ✅ 完整 | CheckType 策略枚举 |
| XSS 防护 | ✅ 完整 | JsonHtmlXssTrimSerializer |
| 敏感配置环境变量注入 | ✅ 完整 | JWT/Druid/DB密码全覆盖 |
| Docker 容器化（MySQL 8.4 + Redis 8.6） | ✅ 完整 | health check + 持久化 |
| 定时任务框架（@Scheduled） | ✅ 基础框架 | ScheduleJobManager，默认注释掉 |
| 通知管理（sys_notice） | ✅ 基础框架 | CRUD 完整，无推送机制 |
| 站内消息推送 | ❌ 缺失 | 无 WebSocket |
| 消息队列 | ❌ 缺失 | |
| 定时任务可视化管理 | ❌ 缺失 | 无 Quartz/xxl-job |
| 多租户 | ❌ 缺失 | |
| 工作流 | ❌ 缺失 | |
| 可观测性（APM/Prometheus） | ❌ 缺失 | |

### 1.4 测试覆盖现状（实测）

| 模块 | 测试类数量 | 测试类型 |
|------|-----------|--------|
| keystone-common | 9 | 工具类单元测试 |
| keystone-domain | 15 | 领域模型单元测试 + H2 集成测试 |
| keystone-infrastructure | 4 | 注解/工具测试 |
| keystone-admin | 5 | 权限检查器单元测试 |
| **合计** | **33** | — |

**空白区域**：UserApplicationService、RoleApplicationService、MenuApplicationService、FileController 均无测试。

---

## 二、短期计划（1-2 个月）— 消除技术债，提升工程质量

### 2.1 代码问题修复

#### 🔴 高优先级

**[1] SecurityConfig.java — `/api/**` 通配符过宽（安全风险）**

```txt
文件：keystone-admin/.../customize/config/SecurityConfig.java
现状：.requestMatchers("/api/**").anonymous()  // 过于宽松
```

`/api/**` 通配符会将所有 api 子路径暴露为匿名访问，存在安全风险。应改为明确枚举允许匿名的端点：

```java
// 改为明确路径，避免误放行内部 API
.requestMatchers(
    "/login", "/register", "/getConfig", "/health", "/captchaImage",
    "/api/v1/app/login", "/api/v1/app/list"
).anonymous()
```

**[2] FileController.java — 文件服务未下沉到 Domain 层（架构问题）**

```txt
文件：keystone-admin/.../controller/common/FileController.java
现状：Controller 直接调用 FileUploadUtils，违反 DDD 分层
```

应在 keystone-domain 创建 `FileApplicationService`，Controller 只负责参数校验与响应封装。

**[3] keystone-infrastructure/filter/TestFilter.java — 遗留空模板文件**

```txt
文件：keystone-infrastructure/.../filter/TestFilter.java
现状：空实现的模板文件遗留在生产代码目录中，不参与任何 Bean 注册
```

删除该文件。

#### 🟡 中优先级

**[4] DeptQuery.java — status/deptName 过滤条件被注释**

```java
// 现状：以下条件被注释，查询功能不完整
// .eq(status != null, "status", status)
// .like(StrUtil.isNotEmpty(deptName), "dept_name", deptName)
```

补全部门查询条件，支持按名称搜索和状态过滤，并在 DeptQuery 中补充对应字段。

**[5] KeystoneConfig.java — 配置类位置不当**

```txt
现状：keystone-common/.../config/KeystoneConfig.java
应改：keystone-infrastructure/.../config/KeystoneConfig.java
原因：common 层不应依赖 @ConfigurationProperties（框架耦合），该类属于基础设施职责
```

**[6] ScheduleJobManager.java — 定时任务体系孤立**

```java
// 现状：@Component 被注释，整个定时任务体系无法运行
//@Component
public class ScheduleJobManager { ... }
```

按实际需求决定：若近期不需要定时任务，删除该类；若需要，进入中期定时任务可视化管理方案。

### 2.2 测试体系补强

优先补充以下测试（当前完全缺失）：

| 目标类 | 测试类型 | 预估工作量 |
|--------|---------|---------|
| UserApplicationService | 单元测试（Mock） | 1天 |
| RoleApplicationService | 单元测试（Mock） | 1天 |
| MenuApplicationService | 单元测试（Mock） | 0.5天 |
| FileController | MockMvc 集成测试 | 0.5天 |

**目标覆盖率**：核心业务服务 ≥ 60%，工具类 ≥ 80%

### 2.3 keystone-api 模块定位明确化

```txt
keystone-api 现状：
├── LoginController（登录）    ← 已完成
├── AppController（应用列表）  ← 已完成
└── OrderController（订单示例）← 空示例，无实际意义
```

- 删除 `OrderController` 空示例
- 明确该模块定位（见长期计划 4.4）

---

## 三、中期计划（3-4 个月）— 补充核心缺失功能

### 3.1 定时任务可视化管理

**现状**：仅有 `ScheduleJobManager` 骨架，无持久化、无管理界面。

**方案（Spring Quartz + 可视化）**：

```sql
CREATE TABLE sys_job (
    job_id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_name        VARCHAR(64)  NOT NULL,
    job_group       VARCHAR(64)  DEFAULT 'DEFAULT',
    invoke_target   VARCHAR(500) NOT NULL,
    cron_expression VARCHAR(255),
    misfire_policy  TINYINT DEFAULT 3,  -- 1立即 2执行一次 3放弃
    concurrent      TINYINT DEFAULT 1,
    status          TINYINT DEFAULT 0,
    remark          VARCHAR(500)
);

CREATE TABLE sys_job_log (
    job_log_id    BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_name      VARCHAR(64) NOT NULL,
    job_group     VARCHAR(64),
    invoke_target VARCHAR(500),
    job_message   VARCHAR(500),
    status        TINYINT DEFAULT 0,
    exception_info TEXT,
    create_time   DATETIME
);
```

**新增 API**：

```txt
GET    /monitor/jobs                 # 任务列表
POST   /monitor/jobs                 # 新建任务
PUT    /monitor/jobs/{jobId}         # 更新任务
DELETE /monitor/jobs/{jobId}         # 删除任务
PUT    /monitor/jobs/{jobId}/run     # 立即执行一次
PUT    /monitor/jobs/{jobId}/status  # 启用/暂停
GET    /monitor/jobLogs              # 执行日志
```

### 3.2 实时消息推送（WebSocket）

**现状**：`sys_notice` 表和 CRUD 接口已就绪，但无推送机制，前端只能轮询。

**技术方案**：

```txt
Spring WebSocket（STOMP 协议）
    ← 连接鉴权：握手阶段验证 JWT Token
    ← 多节点广播：Redis Pub/Sub
    → 前端：SockJS + StompJS

端点设计：
  /ws/connect              # WebSocket 握手端点
  /topic/notification      # 广播通知（所有在线用户订阅）
  /queue/user/{userId}     # 用户私信（点对点）
```

**触发场景**：
- 管理员发送系统公告 → 广播全部在线用户
- 审批/任务操作完成 → 推送给相关用户
- 在线用户数统计实时刷新（MonitorController 现有功能增强）

### 3.3 消息通知中心

在 3.2 基础上，扩展 `sys_notice` 为多渠道通知分发：

```txt
Spring ApplicationEvent 触发
    → NotificationDispatcher
        ├── 站内信（WebSocket 推送 + DB 持久化）
        ├── 邮件（Spring Mail + Thymeleaf 模板）
        └── 企业微信 / 钉钉 Webhook（可插拔）
```

**sys_notice 扩展字段**：

```sql
ALTER TABLE sys_notice
    ADD COLUMN push_channel TINYINT DEFAULT 1,  -- 位标志：1站内 2邮件 4企微
    ADD COLUMN receiver_id  BIGINT,              -- NULL 表示广播
    ADD COLUMN is_read      TINYINT DEFAULT 0,
    ADD COLUMN read_time    DATETIME;
```

**新增 API**：

```txt
GET  /system/notices/unread/count   # 未读通知数
PUT  /system/notices/read/batch     # 批量标记已读
```

### 3.4 API 版本管理

**现状**：`keystone-api` 模块无版本控制，接口升级无法向后兼容。

```yaml
# application-basic.yml 添加
keystone:
  api-version: v1
  api-prefix: /api/${keystone.api-version}
```

```java
// Controller 使用配置化路径
@RequestMapping("${keystone.api-prefix}/users")  // → /api/v1/users
```

---

## 四、长期计划（5-12 个月）— 平台化扩展

### 4.1 可观测性全栈

**推荐作为长期计划中优先级最高的项目**（对生产运维影响最大）：

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```yaml
# application-basic.yml 添加
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus,info,loggers
  metrics:
    tags:
      application: ${spring.application.name}
```

完整链路：

```txt
Spring Boot Actuator + Micrometer → Prometheus → Grafana
链路追踪：Micrometer Tracing + Zipkin（轻量）或 SkyWalking（无侵入）
日志聚合：Logback JSON → Loki → Grafana
```

### 4.2 Flyway 数据库版本管理

**现状**：初始化依赖手动执行 SQL 文件，多人协作/多环境部署容易版本混乱。

```txt
db/migration/
├── V1__init_schema.sql         # 建表 DDL
├── V2__init_data.sql           # 基础数据
├── V3__add_dict_tables.sql     # 数据字典（v3.2.0 对应）
└── V4__add_job_tables.sql      # 定时任务（中期计划添加后）
```

### 4.3 多租户 SaaS（列隔离方案）

利用现有 MyBatis-Plus 插件体系，最小侵入实现：

```java
@Bean
public TenantLineInnerInterceptor tenantLineInnerInterceptor() {
    return new TenantLineInnerInterceptor(new TenantLineHandler() {
        @Override
        public Expression getTenantId() {
            return new LongValue(TenantContext.getCurrentTenantId());
        }

        @Override
        public List<String> ignoreTable() {
            // 全局表不过滤租户
            return List.of("sys_config", "sys_dict_type", "sys_dict_data");
        }
    });
}
```

新增模块 `keystone-tenant`：租户注册/开通/暂停/注销 + 套餐授权管理。

### 4.4 开放 API 平台（keystone-api 深化）

```txt
keystone-api（独立端口 8081，与 admin 8080 隔离）
├── API Key 认证（非 JWT，适合服务间调用）
├── 调用方管理（ClientApp：注册/授权/限流配额）
├── 接口白名单（每个 App 只能访问授权端点）
├── 调用日志与统计
└── Webhook 回调（事件触发通知第三方）
```

### 4.5 工作流引擎（Flowable）

适用于请假、采购、费用报销等多级审批业务。集成成本高（约 3-4 周），建议只在有明确审批类业务需求时引入。

---

## 五、优先级总览

| 阶段 | 任务 | 难度 | 价值 | 推荐优先级 |
|------|------|------|------|----------|
| **立即** | 修复 SecurityConfig `/api/**` 通配符 | 🟢 低 | 安全风险消除 | ⭐⭐⭐⭐⭐ |
| **立即** | 删除 TestFilter.java 遗留空文件 | 🟢 低 | 代码整洁 | ⭐⭐⭐ |
| **立即** | 补全 DeptQuery 被注释的过滤条件 | 🟢 低 | 功能完整 | ⭐⭐⭐ |
| **短期** | FileController DDD 化重构 | 🟡 中 | 架构一致性 | ⭐⭐⭐⭐ |
| **短期** | 迁移 KeystoneConfig 到 infrastructure | 🟢 低 | 架构规范 | ⭐⭐⭐ |
| **短期** | 补充 ApplicationService 单元测试 | 🟡 中 | 长期质量保障 | ⭐⭐⭐⭐ |
| **短期** | 清理 keystone-api 空示例，明确定位 | 🟢 低 | 方向清晰 | ⭐⭐⭐ |
| **中期** | 定时任务可视化管理（Quartz） | 🟡 中 | 高频运维需求 | ⭐⭐⭐⭐⭐ |
| **中期** | WebSocket 实时推送 | 🟡 中 | 提升用户体验 | ⭐⭐⭐⭐ |
| **中期** | 消息通知中心（多渠道） | 🟡 中 | 高频业务需求 | ⭐⭐⭐⭐ |
| **中期** | API 版本管理 | 🟢 低 | 向后兼容支持 | ⭐⭐⭐ |
| **长期** | Prometheus + Grafana 可观测性 | 🟡 中 | 生产运维必备 | ⭐⭐⭐⭐ |
| **长期** | Flyway 数据库版本管理 | 🟢 低 | 工程规范 | ⭐⭐⭐⭐ |
| **长期** | 多租户 SaaS | 🔴 高 | 平台化价值 | ⭐⭐⭐ |
| **长期** | 开放 API 平台（keystone-api 深化） | 🟡 中 | 生态扩展 | ⭐⭐⭐ |
| **长期** | 工作流引擎（Flowable） | 🔴 高 | 审批类场景 | ⭐⭐ |

---

## 六、近期行动清单

### 本周内（代码质量修复）

- [x] 修复 `SecurityConfig.java`：将 `/api/**` 改为明确的端点白名单（commit 8607304）
- [x] 删除 `keystone-infrastructure/filter/TestFilter.java`（commit 8607304）
- [x] 取消注释 `DeptQuery.java` 中的 `status` 和 `deptName` 过滤条件，并补充对应字段（commit 8607304）
- ~~迁移 `KeystoneConfig.java` 到 `keystone-infrastructure` 模块~~ ⛔ 不可行：`FileUploadUtils` 及测试直接使用静态方法，迁移会造成 common ← infrastructure 循环依赖，保持现状

### 本月内（测试与重构）

- [ ] 为 `UserApplicationService` 编写单元测试
- [ ] 为 `RoleApplicationService` 编写单元测试
- [ ] 重构 `FileController`，将文件逻辑下沉到 `FileApplicationService`
- [ ] 删除 `OrderController` 空示例，明确 `keystone-api` 定位

### 下季度启动

- [ ] 实现定时任务可视化管理（Quartz + 管理界面）
- [ ] 集成 WebSocket，为 `sys_notice` 添加实时推送能力
- [ ] 引入 Flyway 管理数据库版本迁移
