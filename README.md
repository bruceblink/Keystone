# AgileBoot Back End

[![Release](https://img.shields.io/badge/Release-V3.1.0-green.svg)](https://github.com/bruceblink/AgileBoot-Back-End)
[![JDK](https://img.shields.io/badge/JDK-25-green.svg)](https://github.com/bruceblink/AgileBoot-Back-End)
[![Spring%20Boot](https://img.shields.io/badge/Spring%20Boot-3.5.13-blue.svg)](https://github.com/bruceblink/AgileBoot-Back-End)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![CI](https://img.shields.io/badge/tests-passing-success)](https://github.com/bruceblink/AgileBoot-Back-End)

> 一个面向中小团队与个人开发者的 **高质量 Java 后端脚手架**。  
> 基于 Spring Boot 3.5 + Vue3 生态，开箱即用、结构清晰、测试完善。

## ✨ 为什么选择 AgileBoot

- 🚀 **上手快**：模块化清晰，业务代码可快速落地
- 🔐 **安全默认**：Spring Security + JWT 认证授权
- 🧩 **可扩展**：分层架构，便于二开和团队协作
- 🧪 **质量可控**：单元测试 + 集成测试覆盖关键链路
- 🛠️ **工程友好**：支持 Docker 本地环境，文档完善

## 🧱 技术亮点

- **JDK 25**
- **Spring Boot 3.5.13**
- MyBatis-Plus / Dynamic Datasource / Druid
- Redis / PostgreSQL / MySQL
- springdoc-openapi（Swagger UI）
- JUnit 5 + Mockito + JaCoCo

## 📦 模块结构

```text
agileboot-admin           # 管理后台接口
agileboot-api             # 对外开放接口
agileboot-common          # 通用基础能力
agileboot-infrastructure  # 配置与基础设施
agileboot-domain          # 核心业务领域
```

## ⚡ 30 秒开始

```bash
git clone https://github.com/bruceblink/AgileBoot-Back-End
cd AgileBoot-Back-End
./gradlew clean build -x test
./gradlew test
```

启动类：`com.agileboot.admin.AgileBootAdminApplication`

## 🌍 在线体验

前端演示地址：<https://agileboot-front-end.pages.dev>

## 📚 文档入口

- 完整工程文档：[文档说明.md](文档说明.md)
- Docker 启动指南：[docker/run.md](docker/run.md)
- 数据库脚本：[sql/mysql8/01_database.sql](sql/mysql8/01_database.sql)

## 🤝 贡献

欢迎提 Issue / PR，一起把 AgileBoot 做得更好。

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
