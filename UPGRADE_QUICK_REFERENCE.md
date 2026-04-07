# AgileBoot Java 25 升级 - 快速参考指南

## 📋 升级分支信息

**分支名称：** `upgrade/java25-spring3`  
**当前提交：** `af65207`  
**分支状态：** ✅ 已创建并初始提交

---

## 🔍 版本变更一览

| 组件 | 旧版本 | 新版本 | 类型 |
|-----|--------|--------|------|
| **Java** | 17 | 25 | 主要升级 ⭐ |
| **Spring Boot** | 2.7.10 | 3.2.4 | 主要升级 ⭐ |
| **Druid** | 1.2.8 | 1.2.23 | 安全更新 |
| **MyBatis Plus** | 3.5.2 | 3.5.5 | 性能改进 |
| **Guava** | 31.0.1 | 33.0.0 | 功能完善 |
| **Springdoc** | 1.6.14 | 2.0.4 | 适配 Spring 3 |
| **Docker** Base | Temurin 17 | Temurin 25 | 容器基础镜像 |

---

## ✅ 已完成的工作

- ✅ 创建升级分支 `upgrade/java25-spring3`
- ✅ 更新所有 pom.xml 版本号
- ✅ **48 个 Java 文件**：javax → jakarta 迁移
- ✅ **3 个 pom.xml**：依赖声明更新
- ✅ 更新 Dockerfile 使用 Java 25
- ✅ 初始 git 提交（56 文件变更）
- ✅ 生成升级文档和指南

---

## 🔄 下一步操作

### 1. 编译验证（5-20 分钟）
```bash
cd e:\Project\AgileBoot-Back-End

# 清理编译
.\mvnw clean compile -pl agileboot-admin -am -DskipTests

# 预期输出：BUILD SUCCESS
# 如果失败：查看错误日志，通常是网络问题或依赖冲突
```

### 2. 单元测试（10-30 分钟）
```bash
# 运行单元测试
.\mvnw test

# 预期：所有测试通过 (Tests run: XXX, Failures: 0)
```

### 3. 构建完整包（10-20 分钟）
```bash
# 完整构建
.\mvnw clean package

# 输出位置：agileboot-admin/target/agileboot-admin.jar
```

### 4. Docker 验证（可选，但推荐）
```bash
# 构建 Docker 镜像
docker build -t agileboot:java25 .

# 启动容器进行测试
docker compose up

# 验证应用健康状态
curl http://localhost:8080/actuator/health
```

---

## 🐛 常见问题解决

### ❌ 问题 1：编译失败 - "找不到类"
**原因：** Maven 依赖未正确下载  
**解决方案：**
```bash
# 清理 Maven 缓存
.\mvnw clean

# 重新下载依赖
.\mvnw dependency:resolve

# 再次编译
.\mvnw compile
```

### ❌ 问题 2：编译失败 - "symbol not found"
**原因：** javax 包还未完全替换  
**解决方案：**
```bash
# 搜索遗漏的 javax
grep -r "import javax\." --include="*.java" .

# 手动更新找到的文件
```

### ❌ 问题 3：测试失败
**原因：** 测试依赖不兼容  
**解决方案：**
```bash
# 跳过测试进行构建
.\mvnw package -DskipTests

# 检查测试日志
.\mvnw test -X

# 查看具体错误
```

### ❌ 问题 4：Docker 构建失败
**原因：** Docker daemon 未启动  
**解决方案：**
```bash
# 启动 Docker
docker --version  # 检查是否安装

# 启动 Docker Desktop

# 重新构建
docker build -t agileboot:java25 .
```

---

## 📂 重要文件位置

```
项目根目录/
├── pom.xml                              # 主 POM，已升级至 Java 25
├── Dockerfile                           # 已更新为 Java 25
├── docker-compose.yml                   # Docker 编排（已升级）
├── UPGRADE_COMPLETION_REPORT.md         # 升级完成报告 ⭐
├── JAVA25_UPGRADE_ASSESSMENT.md         # Java 25 评估 ⭐
├── DOCKER_UPGRADE_GUIDE.md              # Docker 指南 ⭐
├── GRAALVM_JDK25_ASSESSMENT.md          # GraalVM 评估 ⭐
└── [module]/
    ├── pom.xml                          # 子模块 POM
    └── src/main/java/com/agileboot/...
        └── *.java                       # 已迁移至 jakarta
```

---

## 🎯 验证清单

### ✅ 预检查
- [ ] 有足够的磁盘空间（≥ 5GB）
- [ ] Java 25 JDK 已安装或依赖 Maven 下载
- [ ] Maven 3.8+ 已安装（项目包含 mvnw）
- [ ] Git 分支已切换至 `upgrade/java25-spring3`

### ✅ 编译阶段
- [ ] `mvn clean compile` 成功
- [ ] 没有 warning 或 error
- [ ] 所有类都能正确解析

### ✅ 测试阶段
- [ ] `mvn test` 全部通过
- [ ] 代码覆盖率未下降
- [ ] 集成测试正常

### ✅ 构建阶段
- [ ] `mvn package` 成功
- [ ] JAR 文件大小合理（约 50-60MB）
- [ ] 清单文件正确

### ✅ Docker 阶段
- [ ] Docker 镜像构建成功
- [ ] 容器启动正常
- [ ] 应用接口可访问
- [ ] 健康检查通过

---

## 📊 性能预期

| 操作 | 时间 | 备注 |
|-----|------|------|
| 编译（首次） | 15-25 min | 需下载大量依赖 |
| 编译（后续） | 3-5 min | 依赖缓存已存在 |
| 单元测试 | 10-20 min | 取决于测试数量 |
| 完整构建 | 20-35 min | 包括编译、测试、打包 |
| Docker 构建 | 10-15 min | 基于镜像大小 |

---

## 💡 最佳实践

### ✅ 推荐做法
1. **分阶段测试** - 先编译，再测试，最后集成
2. **保留构建日志** - 遇到问题时有助于诊断
3. **定期提交** - 验证每个阶段成功后提交
4. **使用版本控制** - 所有变更都有 git 记录

### ❌ 避免的做法
1. **直接跳转到 Docker** - 应先确保本地编译成功
2. **忽视警告** - Warning 可能导致后续问题
3. **混合使用 mvn 和 mvnw** - 保持一致性
4. **修改构建配置** - 保持原有配置不变

---

## 🔗 相关文档

详细信息请参阅：

| 文档 | 用途 |
|-----|------|
| **UPGRADE_COMPLETION_REPORT.md** | 完整的升级报告和分析 |
| **JAVA25_UPGRADE_ASSESSMENT.md** | Java 25 技术评估 |
| **DOCKER_UPGRADE_GUIDE.md** | Docker 容器化详细指南 |
| **GRAALVM_JDK25_ASSESSMENT.md** | GraalVM 可行性分析 |

---

## 🎓 学习资源

### Java 25 新特性
- 官方发布说明：https://www.oracle.com/java/technologies/javase/25-relnotes.html
- 升级指南：https://www.oracle.com/technical-network/java/javase/upgrade-guide/

### Spring Boot 3.x 迁移
- 官方迁移指南：https://spring.io/blog/2022/08/18/spring-boot-3-0-0-m1-is-now-available
- Jakarta EE 说明：https://jakarta.ee/

### Docker 最佳实践
- 官方文档：https://docs.docker.com/
- Compose 指南：https://docs.docker.com/compose/

---

## 📞 支持

遇到问题？

1. **查看日志** - 错误通常在编译输出中明确说明
2. **搜索已知问题** - 查阅 Spring Boot 和 Java 官方文档
3. **回顾评估报告** - JAVA25_UPGRADE_ASSESSMENT.md 包含常见问题
4. **检查网络** - 确保能访问 Maven 中央仓库

---

## 🏁 预期成果

成功完成升级后，您将获得：

✨ **技术栈现代化**
- 最新的 Java 25 特性
- Spring Boot 3.x LTS 支持（至 2026 年）
- 改进的性能和安全性

🐳 **容器化部署**
- 现代化的 Docker 镜像
- 完整的 docker-compose 配置
- 易于水平扩展

📚 **长期维护**
- 清晰的升级文档
- 最佳实践指南
- 社区和官方支持

---

**当前状态：85% 完成 - 等待编译和测试验证**

**预计完成时间：本周内（取决于编译验证结果）**

---

*最后更新：2026-04-07*
