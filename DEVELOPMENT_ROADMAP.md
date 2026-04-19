# AgileBoot 未来开发路线图

> 基于代码深度审查，结合项目现状与技术趋势制定  
> 版本：1.0 | 日期：2026-04-19

---

## 一、项目现状总结

AgileBoot 目前是一套功能较为完善的**快速开发脚手架**，具备以下核心能力：

| 能力 | 实现情况 |
|------|--------|
| 用户/角色/权限/部门/岗位/菜单 RBAC 管理 | ✅ 完整 |
| JWT 无状态认证 + 方法级权限 | ✅ 完整 |
| 操作日志 / 登录日志 / AOP 切面 | ✅ 完整 |
| 双层缓存（Guava + Redis） | ✅ 完整 |
| 动态多数据源 + Druid 监控 | ✅ 完整 |
| Excel 导入导出 | ✅ 完整 |
| 文件上传 / 服务器监控 | ✅ 完整 |
| 验证码 / 限流 / 防重复提交注解 | ✅ 完整 |
| 数据库密码加密（ENC 机制） | ✅ 完整 |
| Docker 容器化部署 | ✅ 完整 |
| 单元测试框架 + JaCoCo 覆盖率 | ✅ 基础具备 |
| 事件驱动 / 消息队列 | ❌ 缺失 |
| 多租户 SaaS | ❌ 缺失 |
| 工作流引擎 | ❌ 缺失 |
| 可观测性（APM / 链路追踪） | ❌ 缺失 |
| API 版本管理 | ❌ 缺失 |

---

## 二、短期计划（1-2 个月）— 技术债与代码质量

> **目标**：消除已知缺陷，提升代码质量与工程规范，为后续扩展打好基础。

### 2.1 修复代码中的 TODO 与技术债

| 优先级 | 文件 | 问题 | 修复方案 |
|--------|------|------|---------|
| 🔴 高 | `SecurityConfig.java:138` | `TODO this is danger` | 明确安全边界，完善注释或修复 |
| 🔴 高 | `FileController.java:33` | `TODO 需要重构` | 按 DDD 规范将文件服务下沉到 domain 层 |
| 🟡 中 | `FilterConfig.java:21` | URL 白名单硬编码 | 抽取到 `application-basic.yml` 统一配置 |
| 🟡 中 | `MenuApplicationService.java:63` | 按钮只允许在页面类型下创建的校验 | 在 MenuModel 中添加 validateParent() |
| 🟡 中 | `DeptQuery.java:25` | parentId 字段未使用 | 实现按 parentId 筛选部门树 |
| 🟡 中 | `Unrepeatable.java:32` | 防重复提交注解缺少类型选项 | 参考 `@RateLimit` 设计补充策略选项 |
| 🟡 中 | `AgileBootConfig.java:11` | 配置类位置不当 | 迁移至 infrastructure 层合适包下 |
| 🟡 中 | `LoginStatusEnum.java:11` | 枚举命名与数据库表名不一致 | 统一命名规范 |

### 2.2 配置安全加固

```yaml
# 目标：消除生产环境中的硬编码敏感配置

# 1. Druid 监控密码改为环境变量
druid.statViewServlet:
  login-username: ${DRUID_USERNAME:agileboot}
  login-password: ${DRUID_PASSWORD}    # 无默认值，强制注入

# 2. JWT Secret 从环境变量读取
agileboot.jwt.secret: ${JWT_SECRET}    # 强制注入，不设默认值

# 3. 生产环境关闭 Swagger UI
springdoc.api-docs.enabled: ${SWAGGER_ENABLED:false}
springdoc.swagger-ui.enabled: ${SWAGGER_ENABLED:false}

# 4. 生产环境关闭 Druid 监控
druid.statViewServlet.enabled: ${DRUID_MONITOR_ENABLED:false}
```

### 2.3 测试体系完善

- **目标覆盖率**：核心业务代码 ≥ 60%，工具类 ≥ 80%
- **补充单元测试**：UserApplicationService、RoleApplicationService、MenuApplicationService
- **添加集成测试**：使用 Testcontainers 提供 MySQL + Redis 真实容器测试
- **API 测试**：使用 Spring Boot Test + MockMvc 为主要 Controller 编写测试

### 2.4 API 版本控制

在所有路由中添加版本前缀，平滑支持多版本并行：

```java
// 方案：URL 路径版本
@RequestMapping("/api/v1/system/users")    // v1 接口
@RequestMapping("/api/v2/system/users")    // v2 接口（向后兼容）

// application-basic.yml
agileboot:
  api-version: v1
  api-prefix: /api/${agileboot.api-version}
```

---

## 三、中期计划（3-4 个月）— 核心功能扩展

> **目标**：在稳固基础上，补充主流企业应用缺少的关键能力。

### 3.1 消息通知中心

**场景**：系统消息推送、待办提醒、审批通知、告警推送

**技术方案**：
```
用户触发事件
    → EventPublisher（Spring ApplicationEvent）
    → NotificationService（分发路由）
        ├── 站内信（数据库存储 + WebSocket 推送）
        ├── 邮件通知（Spring Mail + 模板）
        └── 短信通知（阿里云 / 腾讯云 SMS SDK，可插拔）
```

**数据模型**：
```sql
CREATE TABLE sys_notification (
    notification_id BIGINT PRIMARY KEY,
    user_id         BIGINT NOT NULL,          -- 接收用户
    title           VARCHAR(128),
    content         TEXT,
    type            TINYINT,                  -- 1系统 2审批 3告警
    is_read         TINYINT DEFAULT 0,
    link_url        VARCHAR(256),             -- 跳转链接
    create_time     DATETIME,
    read_time       DATETIME
);
```

**新增 API**：
```
GET  /system/notifications          # 获取我的通知列表
PUT  /system/notifications/read     # 批量标记已读
GET  /system/notifications/unread/count  # 未读数量（轮询/WebSocket）
```

### 3.2 WebSocket 实时推送

**场景**：在线用户状态、消息实时推送、大屏数据刷新

**实现**：
```java
// 依赖
spring-boot-starter-websocket

// 端点配置
@ServerEndpoint("/ws/{userId}")
public class WebSocketServer {
    // 连接管理 + 心跳检测 + 消息分发
}

// 与 Redis Pub/Sub 结合，支持集群部署
// 多节点间消息广播：Redis Channel → 每节点 WebSocket 推送
```

### 3.3 工作流引擎集成（Flowable）

**场景**：请假审批、采购申请、费用报销、合同审批

**技术方案**：
```
业务发起申请
    → FlowApplicationService
        → Flowable ProcessEngine
            ├── 启动流程实例
            ├── 完成任务节点
            ├── 流程跳转/回退
            └── 流程图可视化

前端：在线设计器（Vue3 + bpmn.js）
```

**数据模型扩展**：
```sql
-- Flowable 自动创建约 20+ 张流程表
-- 业务关联表
CREATE TABLE biz_process_instance (
    id          BIGINT PRIMARY KEY,
    biz_type    VARCHAR(64),              -- 业务类型
    biz_id      BIGINT,                  -- 业务主键
    process_id  VARCHAR(64),             -- Flowable 流程实例 ID
    status      TINYINT,                 -- 1审批中 2通过 3拒绝 4撤回
    creator_id  BIGINT,
    create_time DATETIME
);
```

### 3.4 数据字典增强

**现状**：系统参数表（sys_config）承担了部分字典功能，但缺乏专用数据字典管理。

**新增功能**：
```sql
CREATE TABLE sys_dict_type (
    dict_id    BIGINT PRIMARY KEY,
    dict_name  VARCHAR(64),
    dict_type  VARCHAR(100) UNIQUE,       -- 字典类型标识，如 sys_user_status
    status     TINYINT DEFAULT 1,
    remark     VARCHAR(512)
);

CREATE TABLE sys_dict_data (
    dict_code  BIGINT PRIMARY KEY,
    dict_type  VARCHAR(100),
    dict_label VARCHAR(128),              -- 显示标签
    dict_value VARCHAR(128),             -- 实际值
    dict_sort  INT DEFAULT 0,
    is_default TINYINT DEFAULT 0,
    status     TINYINT DEFAULT 1
);
```

**API**：
```
GET /system/dict/type/list        # 字典类型列表
GET /system/dict/data/{dictType}  # 获取字典数据（前端下拉框专用，可缓存）
```

### 3.5 定时任务管理

**场景**：数据清理、报表生成、第三方数据同步、缓存预热

**技术方案**：
```
方案一（轻量级）：Spring Quartz + 数据库持久化
    - 支持 Cron 表达式配置
    - 任务执行日志记录
    - 可视化界面（新增菜单）

方案二（分布式）：xxl-job（多节点部署时推荐）
    - 分布式锁，避免重复执行
    - 任务分片、路由策略
    - 失败重试机制
```

---

## 四、长期计划（5-12 个月）— 平台化与生态扩展

> **目标**：将项目从脚手架升级为可支撑多业务的**低代码/开放平台基础**。

### 4.1 多租户 SaaS 架构

**应用场景**：将脚手架作为多租户 SaaS 平台基础，服务多个独立客户/组织。

**隔离策略选型**：

| 策略 | 适用场景 | 改造复杂度 |
|------|---------|-----------|
| **列隔离（tenant_id 字段）** | 小型多租户，快速落地 | 🟢 低 |
| **Schema 隔离（每租户一个 schema）** | 中型 SaaS，数据隔离性好 | 🟡 中 |
| **数据库隔离（每租户独立 DB）** | 大型企业客户，安全性最高 | 🔴 高 |

**推荐路径**（列隔离，利用现有 Dynamic Datasource 扩展）：
```java
// 1. 所有业务表增加 tenant_id 字段
// 2. TenantContext（ThreadLocal 存储当前租户 ID）
// 3. MyBatis-Plus 插件：TenantLineInnerInterceptor 自动注入 tenant_id 过滤
// 4. 租户管理模块（新模块 agileboot-tenant）

@Bean
public TenantLineInnerInterceptor tenantLineInnerInterceptor() {
    return new TenantLineInnerInterceptor(new TenantLineHandler() {
        @Override
        public Expression getTenantId() {
            return new LongValue(TenantContext.getCurrentTenantId());
        }
    });
}
```

**新增模块**：`agileboot-tenant`
- 租户注册 / 开通 / 暂停 / 注销
- 租户套餐管理（功能权限按套餐授权）
- 租户管理员账号自动开通
- 租户资源用量统计

### 4.2 低代码配置引擎

**应用场景**：通过配置生成标准 CRUD 页面，提高业务交付效率。

**实现路径**：
```
阶段一：表单配置器
    - 可视化配置表单字段（类型/校验/联动）
    - 生成 Vue3 组件代码（前端）
    - 生成 Java 实体/Service/Controller（后端代码生成）

阶段二：列表/查询配置器
    - 配置列表展示字段、排序、筛选条件
    - 配置导出模板

阶段三：工作流绑定
    - 将表单绑定到 Flowable 审批流程
    - 自动生成审批历史展示组件
```

**技术组件**：
- MyBatis-Plus Generator（已有）→ 增强为可视化界面
- Velocity/Freemarker 模板（已有）→ 扩充模板库

### 4.3 开放 API 平台（agileboot-api 模块完善）

**当前状态**：`agileboot-api` 模块仅有 `OrderController` 示例，几乎为空。

**建议方向**：将 api 模块建设为面向第三方的 **OpenAPI 网关入口**：

```
agileboot-api（对外开放）
├── 独立端口（如 8081，与 admin 隔离）
├── API Key 认证（非 JWT，更适合服务间调用）
├── 调用方管理（ClientApp 表：注册、授权、限流配额）
├── 接口白名单（每个 ClientApp 只能访问授权接口）
├── API 调用统计与计费基础
└── Webhook 回调（事件触发通知第三方系统）
```

**数据模型**：
```sql
CREATE TABLE api_client_app (
    app_id     BIGINT PRIMARY KEY,
    app_name   VARCHAR(64),
    app_key    VARCHAR(64) UNIQUE,         -- API Key
    app_secret VARCHAR(128),               -- 用于签名验证
    status     TINYINT DEFAULT 1,
    rate_limit INT DEFAULT 1000,           -- 每分钟调用上限
    expire_time DATETIME
);

CREATE TABLE api_call_log (
    log_id     BIGINT PRIMARY KEY,
    app_id     BIGINT,
    api_path   VARCHAR(256),
    method     VARCHAR(16),
    status_code SMALLINT,
    latency_ms  INT,
    call_time   DATETIME
);
```

### 4.4 可观测性全栈升级

**目标**：接入 Prometheus + Grafana + 链路追踪，达到生产级可观测性。

```
【指标监控】
Spring Boot Actuator + Micrometer
    → Prometheus（拉取指标）
    → Grafana Dashboard（可视化告警）

【链路追踪】
方案一（轻量）：Micrometer Tracing + Zipkin
方案二（重量级）：SkyWalking Java Agent（探针，无侵入）

【日志聚合】
Logback 结构化输出（JSON 格式）
    → Loki（日志存储）
    → Grafana（统一查询日志+指标+链路）

【告警】
Alertmanager（Prometheus 告警规则）
    → 接入通知中心（邮件/短信/钉钉）
```

**最小实现步骤**：
```xml
<!-- 添加依赖 -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```yaml
# 开放 Prometheus 抓取端点
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus,info
  metrics:
    tags:
      application: ${spring.application.name}
```

### 4.5 智能化功能探索（AI 增强）

**场景**：随着 AI 大模型生态成熟，可在脚手架层面提供标准 AI 集成点。

| 功能 | 技术方案 | 价值 |
|------|---------|------|
| **智能搜索** | 集成向量数据库（Milvus/Chroma）+ Embedding | 语义搜索代替关键字搜索 |
| **操作日志智能分析** | 日志 → LLM 分析 → 异常行为预警 | 安全审计增强 |
| **自然语言查询** | 用户输入自然语言 → LLM 生成 SQL → 执行返回结果 | 低代码报表 |
| **AI 代码生成** | 描述业务需求 → 自动生成 CRUD 代码 | 开发提效 |
| **智能客服** | 基于系统文档的 RAG 知识库问答 | 降低运维支持成本 |

**推荐入口**：Spring AI（Spring 官方 AI 框架，已支持主流模型）

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>
```

---

## 五、横向扩展方向

### 5.1 移动端支持

**场景**：移动端 App 或小程序需要对接 Admin 后台。

**方案**：
- `agileboot-api` 模块扩展为移动端专用 API 层
- 添加 OAuth2 授权服务器（Spring Authorization Server）
- 支持微信小程序 / APP 的 Token 刷新流程
- 提供专门的移动端接口（分页大小、图片压缩等适配）

### 5.2 微服务拆分路径

> 当单体应用流量瓶颈出现时，可沿以下边界逐步拆分。

```
当前单体
    ↓ 第一步：基础设施服务拆分
├── agileboot-auth-service（认证授权服务，独立部署）
├── agileboot-file-service（文件上传/存储服务）
└── agileboot-notification-service（通知推送服务）
    ↓ 第二步：核心业务拆分
├── agileboot-user-service（用户/权限管理）
├── agileboot-workflow-service（工作流服务）
└── agileboot-{业务}-service（按业务边界拆分）

网关层：Spring Cloud Gateway
服务发现：Nacos / Consul
配置中心：Nacos Config / Spring Cloud Config
```

### 5.3 多数据库方言支持

**现状**：代码已支持 MySQL + PostgreSQL 双驱动，但 SQL 脚本仅有 MySQL 版本。

**改进**：
- 补充 PostgreSQL 初始化 SQL 脚本
- 使用 Flyway 管理数据库版本迁移（代替手动执行 SQL 文件）
- 支持国产数据库（达梦 DM8 / 人大金仓 KingbaseES）——适合政务场景

```xml
<!-- Flyway 数据库版本管理 -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

---

## 六、优先级总览

| 计划阶段 | 方向 | 难度 | 预期价值 | 推荐优先级 |
| --------- | ------ | ------ | --------- | ----------- |
| **短期** | 修复 TODO / 技术债 | 🟢 低 | 提升稳定性与可维护性 | ⭐⭐⭐⭐⭐ |
| **短期** | 配置安全加固 | 🟢 低 | 消除生产安全风险 | ⭐⭐⭐⭐⭐ |
| **短期** | 测试体系完善 | 🟡 中 | 长期质量保障 | ⭐⭐⭐⭐ |
| **短期** | API 版本控制 | 🟢 低 | 支持平滑升级 | ⭐⭐⭐ |
| **中期** | 消息通知中心 | 🟡 中 | 高频业务需求 | ⭐⭐⭐⭐⭐ |
| **中期** | 数据字典管理 | 🟢 低 | 高频业务需求 | ⭐⭐⭐⭐⭐ |
| **中期** | 定时任务管理 | 🟡 中 | 高频业务需求 | ⭐⭐⭐⭐ |
| **中期** | WebSocket 实时推送 | 🟡 中 | 提升用户体验 | ⭐⭐⭐⭐ |
| **中期** | 工作流引擎（Flowable） | 🔴 高 | 覆盖审批类场景 | ⭐⭐⭐ |
| **长期** | 多租户 SaaS | 🔴 高 | 平台化商业价值 | ⭐⭐⭐⭐ |
| **长期** | 开放 API 平台 | 🟡 中 | 生态扩展 | ⭐⭐⭐ |
| **长期** | 可观测性全栈 | 🟡 中 | 生产运维必备 | ⭐⭐⭐⭐ |
| **长期** | 低代码引擎 | 🔴 高 | 差异化竞争力 | ⭐⭐⭐ |
| **长期** | AI 智能化功能 | 🔴 高 | 前沿探索 | ⭐⭐ |
| **横向** | 移动端 / OAuth2 | 🟡 中 | 扩展应用场景 | ⭐⭐⭐ |
| **横向** | Flyway 数据库迁移 | 🟢 低 | 工程规范 | ⭐⭐⭐⭐ |
| **横向** | Prometheus + Grafana | 🟡 中 | 生产运维必备 | ⭐⭐⭐⭐ |

---

## 七、下一步行动建议

### 立即可做（本周内）

1. **修复 Druid 密码硬编码**（生产安全风险）
2. **修复 JWT Secret 配置**（安全风险）
3. **解决 `SecurityConfig.java:138` 的 TODO 标记**（安全隐患）
4. **添加 Flyway 数据库迁移管理**（工程规范）

### 近期启动（本月内）

1. 补充 **数据字典管理**（sys_dict_type + sys_dict_data）功能
2. 建立 **集成测试基础**（Testcontainers + 核心服务测试）
3. 规划 **消息通知中心** 详细设计

### 中长期评估

- 根据实际业务方向选择：是走 **SaaS 多租户** 还是 **微服务拆分**
- 工作流引擎是否引入，取决于是否有审批类业务需求
- AI 功能集成，建议先从 **操作日志分析** 或 **智能搜索** 入手，成本低且价值明显
