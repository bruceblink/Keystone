# AgileBoot 升级至 Java 25 + Spring Boot 3.x 实施方案
# 针对 Docker 容器化 + 长期维护

## 📋 推荐方案

**方案：Java 25 + Spring Boot 3.x（JVM 模式）+ Docker**

| 方面 | 指标 |
|-----|-----|
| **工作量** | 20-30 天 |
| **难度** | 中等 |
| **风险** | 低 |
| **长期支持** | ✅ Spring Boot 3.x 支持到 2026 年 |
| **维护难度** | ✅ 简单（成熟的技术栈） |
| **Docker 友好** | ✅ 完美支持 |

---

## 🚀 分阶段升级计划

### 第 1 阶段：准备工作（1-2 天）

```bash
# 1. 创建升级分支
git checkout -b upgrade/java25-spring3

# 2. 备份当前代码
git tag -a v2.0.0-before-upgrade -m "Backup before Java 25 upgrade"

# 3. 验证当前测试
mvn clean test -pl agileboot-admin -am
```

### 第 2 阶段：Spring Boot 升级（3-5 天）

**步骤 1：更新 pom.xml（主 pom）**

```xml
<!-- 当前版本 -->
<spring.boot.version>2.7.10</spring.boot.version>
<java.version>17</java.version>

<!-- 升级到 -->
<spring.boot.version>3.2.4</spring.boot.version>
<java.version>25</java.version>
<maven.compiler.plugin.version>3.13.0</maven.compiler.plugin.version>
```

**步骤 2：验证 Spring Boot 3.x 依赖版本**

```xml
<!-- 自动更新的关键依赖 -->
<maven-surefire-plugin.version>3.2.2</maven-surefire-plugin.version>
<junit.jupiter.version>5.10.0</junit.jupiter.version>
<mockito.version>5.7.0</mockito.version>
```

### 第 3 阶段：包名迁移（3-5 天）

**全局替换 javax → jakarta**

```
javax.servlet.* → jakarta.servlet.*
javax.annotation.* → jakarta.annotation.*
javax.validation.* → jakarta.validation.*
javax.xml.bind.* → jakarta.xml.bind.*
```

**需要更新的文件类型：**
- 所有 .java 源文件
- pom.xml 依赖

### 第 4 阶段：依赖库更新（2-3 天）

```xml
<!-- 更新关键库版本 -->

<!-- Druid 升级（支持 Native Image） -->
<druid.version>1.2.23</druid.version>

<!-- MyBatis Plus 升级 -->
<mybatis-plus.version>3.5.5</mybatis-plus.version>

<!-- Guava 升级 -->
<com.google.guava.version>33.0.0-jre</com.google.guava.version>

<!-- 其他推荐更新 -->
<hutool.version>5.8.40</hutool.version>
<commons-lang3.version>3.18.0</commons-lang3.version>
```

### 第 5 阶段：Docker 配置（1-2 天）

**使用提供的 Dockerfile 和 docker-compose.yml**

```bash
# 验证 Docker 配置
docker build -t agileboot:latest .
docker compose up -d
```

### 第 6 阶段：测试验证（3-5 天）

```bash
# 编译验证
mvn clean compile

# 单元测试
mvn test

# 集成测试
mvn verify

# 构建 Docker 镜像
docker build -t agileboot:java25 .

# Docker 容器测试
docker compose up
docker compose exec agileboot curl http://localhost:8080/actuator/health
```

---

## 📦 Docker 部署详解

### 容器化优势

✅ **一致性部署** - 开发、测试、生产环境一致  
✅ **版本管理** - 轻松回滚  
✅ **可扩展性** - 轻松横向扩展  
✅ **隔离性** - 不同服务独立运行  
✅ **长期支持** - 容器镜像长期保存

### Dockerfile 说明

```dockerfile
# 构建阶段（多阶段构建，减小镜像大小）
FROM eclipse-temurin:25-jdk-jammy as builder

# 运行阶段（仅包含 JRE，镜像更小）
FROM eclipse-temurin:25-jre-jammy

# JVM 优化参数
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --retries=3
```

### Docker Compose 说明

包含完整的依赖服务：
- **MySQL 8.0** - 主数据库
- **PostgreSQL 16** - 备选数据库
- **Redis 7** - 缓存和会话
- **Application** - AgileBoot 应用

---

## 🔧 应用配置更新

### 1. 新增 Actuator 端点（用于健康检查）

在 `agileboot-infrastructure/pom.xml` 中添加：

```xml
<!-- Spring Boot Actuator for health check -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

在 `application-prod.yml` 中配置：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: when-authorized
```

### 2. 应用配置文件优化

**application-prod.yml**

```yaml
server:
  port: 8080
  servlet:
    context-path: /

spring:
  profiles:
    active: prod
  
  datasource:
    url: jdbc:mysql://mysql:3306/agileboot
    username: agileboot
    password: agileboot123
    driver-class-name: com.mysql.cj.jdbc.Driver
    type: com.alibaba.druid.pool.DruidDataSource
    druid:
      initial-size: 5
      min-idle: 5
      max-active: 20
      max-wait: 60000
      test-on-borrow: false
      test-on-return: false
      test-while-idle: true
      validation-query: SELECT 1
      time-between-eviction-runs-millis: 60000
      min-evictable-idle-time-millis: 30000
  
  redis:
    host: redis
    port: 6379
    timeout: 2000ms
    jedis:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
```

### 3. 日志配置优化

**logback-spring.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProfile name="dev">
        <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
        <property name="LOG_FILE" value="${LOG_FILE:-${LOG_PATH:-${LOG_TEMP:-${java.io.tmpdir:-/tmp}}/}spring.log}"/>
        <include resource="org/springframework/boot/logging/logback/console-appender.xml"/>
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>

    <springProfile name="prod">
        <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
        <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        <root level="INFO">
            <appender-ref ref="STDOUT"/>
        </root>
    </springProfile>
</configuration>
```

---

## 🐳 Docker 使用指南

### 本地开发

```bash
# 启动所有服务
docker compose up

# 查看日志
docker compose logs -f agileboot

# 停止服务
docker compose down

# 清理所有数据
docker compose down -v
```

### CI/CD 集成

**GitHub Actions 示例**

```yaml
name: Build and Push Docker Image

on:
  push:
    branches: [main, develop]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 25
        uses: actions/setup-java@v3
        with:
          java-version: '25'
          distribution: 'temurin'
      
      - name: Build with Maven
        run: mvn clean package -DskipTests
      
      - name: Build Docker image
        run: docker build -t agileboot:${{ github.sha }} .
      
      - name: Push to Docker Registry
        run: |
          echo ${{ secrets.DOCKER_PASSWORD }} | docker login -u ${{ secrets.DOCKER_USERNAME }} --password-stdin
          docker tag agileboot:${{ github.sha }} your-registry/agileboot:latest
          docker push your-registry/agileboot:latest
```

### 生产部署

**Kubernetes 部署示例**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: agileboot
spec:
  replicas: 3
  selector:
    matchLabels:
      app: agileboot
  template:
    metadata:
      labels:
        app: agileboot
    spec:
      containers:
      - name: agileboot
        image: your-registry/agileboot:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            configMapKeyRef:
              name: agileboot-config
              key: db_url
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 5
```

---

## 🛠️ 长期维护计划

### 1. 版本维护策略

| 版本 | Java | Spring Boot | 支持截止 | 维护模式 |
|-----|------|------------|---------|--------|
| v2.0.x | 17 | 2.7 | 2023-12 | ⚠️ 停止更新 |
| v3.0.x | 25 | 3.2 | 2026-12 | ✅ LTS 主版本 |
| v3.1.x | 25+ | 3.3+ | 2027-06 | ✅ 后续跟进 |

### 2. 定期更新计划

**每季度（3 个月）：**
- 检查依赖库安全补丁
- 更新容器基础镜像（Temurin）
- 运行全量测试

**每半年（6 个月）：**
- 评估 Spring Boot 小版本更新
- 评估 Java 补丁更新
- 发布安全补丁版本

**每年：**
- 评估主版本升级
- 进行架构审视
- 更新文档

### 3. 容器镜像管理

```bash
# 标记版本
docker build -t agileboot:3.0.0 .
docker build -t agileboot:latest .

# 发布到仓库
docker push your-registry/agileboot:3.0.0
docker push your-registry/agileboot:latest

# 保留历史版本（便于回滚）
# latest -> 3.0.0
# 3.0.0 -> 3.0.1
# 3.0.1 -> 3.1.0
# ...
```

### 4. 安全扫描

```bash
# 使用 Trivy 扫描镜像漏洞
trivy image agileboot:latest

# 使用 Snyk 扫描依赖
snyk test --docker agileboot:latest
```

---

## 📊 升级时间表

```
Week 1:
  Day 1-2: 准备工作（创建分支、备份）
  Day 3-4: Spring Boot 升级（pom.xml）
  Day 5: 包名迁移（javax → jakarta）

Week 2:
  Day 1-2: 依赖库更新
  Day 3-4: Docker 配置和测试
  Day 5: 集成测试和验证

Week 3:
  Day 1-2: 功能测试
  Day 3: 文档更新
  Day 4-5: 代码审查和合并
```

---

## 📝 检查清单

### 代码升级
- [ ] 创建升级分支
- [ ] Spring Boot 升级到 3.2.x
- [ ] Java 版本升级到 25
- [ ] javax → jakarta 全局替换
- [ ] 依赖库版本更新
- [ ] 编译成功（mvn clean compile）
- [ ] 所有单元测试通过
- [ ] 所有集成测试通过

### Docker 容器化
- [ ] Dockerfile 验证
- [ ] docker-compose.yml 验证
- [ ] 本地 Docker 构建成功
- [ ] Docker Compose 启动成功
- [ ] 容器健康检查工作
- [ ] 应用成功启动
- [ ] API 可正常访问

### 长期维护
- [ ] CI/CD 流程配置
- [ ] 容器镜像仓库设置
- [ ] 监控告警配置
- [ ] 文档完整性检查
- [ ] 版本维护计划制定
- [ ] 安全扫描工具配置

---

## 📖 快速启动

### 首次运行

```bash
# 1. 克隆并切换分支
git clone https://github.com/bruceblink/AgileBoot-Back-End.git
cd AgileBoot-Back-End
git checkout upgrade/java25-spring3

# 2. 本地编译
mvn clean package -DskipTests

# 3. Docker 启动
docker compose up --build

# 4. 验证服务
curl http://localhost:8080/actuator/health

# 5. 访问应用
# http://localhost:8080
```

### 常见问题

**Q: Docker 内存不足？**
```yaml
# docker-compose.yml 中调整
environment:
  JAVA_OPTS: "-Xms512m -Xmx1024m"
```

**Q: 数据库连接失败？**
```bash
# 检查容器网络
docker network inspect agileboot-network

# 检查服务健康状态
docker compose ps
```

**Q: 镜像构建太慢？**
```bash
# 使用构建缓存
docker build --cache-from agileboot:latest -t agileboot:new .
```

---

## 🎯 总结

这个方案提供了：
✅ **最优的成本/收益比** - 工作量不大，收益明显  
✅ **长期支持保证** - Spring Boot 3.x 支持到 2026 年  
✅ **完美的容器支持** - 开箱即用的 Docker  
✅ **简洁的维护流程** - 标准的 Java 技术栈  
✅ **清晰的升级路径** - 逐步迭代优化  

**建议立即启动第 1-2 阶段，预计 2-3 周完成全部升级。**
