# Keystone

[![Release](https://img.shields.io/badge/Release-V3.2.0-green.svg)](https://github.com/bruceblink/Keystone)
[![JDK](https://img.shields.io/badge/JDK-25-green.svg)](https://github.com/bruceblink/Keystone)
[![Spring%20Boot](https://img.shields.io/badge/Spring%20Boot-3.5.13-blue.svg)](https://github.com/bruceblink/Keystone)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![CI](https://img.shields.io/badge/tests-passing-success)](https://github.com/bruceblink/Keystone)

> 一个面向中小团队与个人开发者的 **高质量 Java 后端脚手架**。  
> 基于 Spring Boot 3.5 + Vue3 生态，开箱即用、结构清晰、测试完善。

## ✨ 为什么选择 Keystone

- 🚀 **上手快**：模块化清晰，业务代码可快速落地
- 🔐 **安全默认**：Spring Security + JWT 认证授权，敏感配置强制环境变量注入
- 🧩 **可扩展**：分层架构，便于二开和团队协作
- 🧪 **质量可控**：单元测试 + 集成测试覆盖关键链路（136 个测试全部通过）
- 🛠️ **工程友好**：支持 Docker 本地环境，文档完善
- 📚 **数据字典**：内置字典类型 + 字典数据管理，支持 Redis 缓存

## 🧱 技术亮点

- **JDK 25**
- **Spring Boot 3.5.13**
- MyBatis-Plus / Dynamic Datasource / Druid
- **Caffeine** 本地高性能缓存 + Redis 分布式缓存（三级缓存架构）
- springdoc-openapi（Swagger UI）
- JUnit 5 + Mockito + JaCoCo
- Gradle Configuration Cache，增量构建可低至 1s

## 📦 模块结构

```text
keystone-admin           # 管理后台接口
keystone-api             # 对外开放接口
keystone-common          # 通用基础能力
keystone-infrastructure  # 配置与基础设施
keystone-domain          # 核心业务领域
```

## ⚡ 30 秒开始

```bash
git clone https://github.com/bruceblink/Keystone
cd Keystone
./gradlew clean build -x test
./gradlew test
```

启动类：`app.keystone.admin.KeystoneAdminApplication`

## 🌍 在线体验

前端演示地址：<https://agileboot-front-end.pages.dev>

## 🐳 Docker 开发环境

### 服务组成

| 服务 | 镜像 | 容器名 | 宿主机端口 |
|------|------|--------|-----------|
| MySQL 8.4 | `mysql:8.4` | `infra_mysql` | `3306` |
| Redis 8.6.2 | `redis:8.6.2-alpine` | `infra_redis` | `6379` |
| Spring Boot 后端 | `keystone:latest` | `sys_manage_backend` | `18080` |

> 所有服务时区均为 **UTC**。

### 快速启动

#### 1. 仅启动中间件（MySQL + Redis，本地 IDE 运行应用）

```bash
cd docker
docker compose up -d mysql redis
```

#### 2. 启动完整环境（含构建应用镜像）

```bash
cd docker
docker compose up -d --build
```

#### 3. 查看日志

```bash
# 所有服务
docker compose logs -f

# 单个服务
docker compose logs -f sys_manage_backend
docker compose logs -f mysql
```

### 环境变量

默认值在 `docker/.env` 中。如需本地覆盖（例如修改密码），创建 `docker/.env.local`：

```bash
cp .env .env.local
# 编辑 .env.local，修改 MYSQL_ROOT_PASSWORD / REDIS_PASSWORD 等
```

然后启动时指定：

```bash
docker compose --env-file .env.local up -d
```

### 挂载自定义 application.yml

容器会额外读取 `/app/config/` 目录下的 Spring Boot 配置文件：

- 如果宿主机 `docker/app/config/application.yml` 存在，则优先覆盖镜像内置默认配置
- 如果该文件不存在，则继续使用镜像内置的 `application.yml`

示例：

```bash
mkdir -p docker/app/config
# 将自定义配置放到 docker/app/config/application.yml
docker compose up -d
```

### 停止 / 销毁

```bash
# 停止（保留 volume 数据）
docker compose down

# 停止并删除所有数据（慎用）
docker compose down -v
```

### 本地 IDE 开发时的连接配置

`application-dev.yml` 已配置好宿主机访问地址，无需修改：

- MySQL: `localhost:3306` / DB: `keystone` / User: `root` / Pass: `12345`
- Redis: `localhost:6379` / Pass: `12345`

Spring Boot 启动时使用 profile：`basic,dev`

## 📚 文档入口

- 完整工程文档：[文档说明.md](文档说明.md)
- Docker 启动指南：见本文档上方“Docker 开发环境”章节
- 数据库脚本：
  - [sql/mysql8/01_database.sql](sql/mysql8/01_database.sql) — 建库
  - [sql/mysql8/02_agileboot-20230814.sql](sql/mysql8/02_agileboot-20230814.sql) — 基础数据
  - [sql/mysql8/03_dict-20240101.sql](sql/mysql8/03_dict-20240101.sql) — 数据字典（v3.2.0 新增）
- 数据库密码加密：[DATABASE_PASSWORD_ENCRYPTION_GUIDE.md](DATABASE_PASSWORD_ENCRYPTION_GUIDE.md)
- Keylo 对接说明（含用户新增后注册、统一 `/login` 后端 Keylo 凭证鉴权）：见 [文档说明.md](文档说明.md) 的“Keylo 集成与用户注册流程”章节

## 🤝 贡献

欢迎提 Issue / PR，一起把 Keystone 做得更好。

提交信息统一使用 Conventional Commits 风格：`type: subject` 或 `type(scope): subject`。

允许的 `type`：`build`、`chore`、`ci`、`docs`、`feat`、`fix`、`perf`、`refactor`、`release`、`revert`、`style`、`test`。

示例：

```text
feat(auth): add refresh token rotation
fix: handle null query parameter in user listing
docs(readme): update quick start commands
```

启用仓库内校验钩子：

```bash
git config core.hooksPath .githooks
```

如果这个项目对你有帮助，欢迎点个 **Star** ⭐
