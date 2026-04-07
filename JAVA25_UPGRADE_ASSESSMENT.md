# AgileBoot 升级至 Java 25 评估报告

## 📊 项目基本信息

| 项目属性 | 当前配置 |
|--------|--------|
| **当前 Java 版本** | Java 17 |
| **Spring Boot 版本** | 2.7.10 |
| **项目版本** | 2.0.0 |
| **构建工具** | Maven |
| **模块数量** | 5 个 |

---

## 1️⃣ 升级可行性分析

### 1.1 总体可行性评级：**⭐⭐⭐⭐☆ (4/5 - 高度可行)**

升级到 Java 25 **总体可行**，但需要进行中等工作量的调整。

---

## 2️⃣ 关键兼容性问题识别

### 2.1 **强制迁移 - 必须处理** ⚠️

#### 问题 1：javax → jakarta 命名空间迁移（最严重）
**影响范围：高**

**检测到的 javax 包使用：**
- ✗ `javax.servlet.*` - 大量使用（TraceIdFilter、TestFilter、GlobalExceptionFilter 等）
- ✗ `javax.annotation.*` - 广泛使用（@PostConstruct、@PreDestroy、@Resource）
- ✗ `javax.validation.*` - 验证注解
- ✗ `javax.xml.bind.jaxb-api` - XML 绑定

**文件清单：**
```
基础设施层 (agileboot-infrastructure):
  - src/main/java/com/agileboot/infrastructure/filter/TraceIdFilter.java
  - src/main/java/com/agileboot/infrastructure/filter/TestFilter.java
  - src/main/java/com/agileboot/infrastructure/exception/GlobalExceptionFilter.java
  - src/main/java/com/agileboot/infrastructure/thread/ShutdownHook.java
  - src/main/java/com/agileboot/infrastructure/config/redis/EmbeddedRedisConfig.java
  - src/main/java/com/agileboot/infrastructure/cache/aop/GuavaCacheBean.java

通用层 (agileboot-common):
  - src/main/java/com/agileboot/common/utils/ServletHolderUtil.java
  - src/main/java/com/agileboot/common/utils/poi/CustomExcelUtil.java
  - 等多个文件

业务层 (agileboot-domain):
  - 多个服务和 DTO 类中的验证注解
  - 测试类中的 @Resource 注解

API 层 (agileboot-api):
  - src/main/java/com/agileboot/api/customize/service/JwtTokenService.java
  - src/main/java/com/agileboot/api/customize/config/JwtAuthenticationFilter.java
```

**迁移策略：**
1. 升级 Spring Boot 至 3.x（需要同步）→ 自动处理 jakarta 映射
2. 或手动替换所有 import 语句
3. 不支持同时使用 javax 和 jakarta

---

#### 问题 2：Spring Boot 版本兼容性
**影响范围：高**

**当前状态：**
- 当前版本：Spring Boot 2.7.10（已停止维护）
- Java 25 支持：Spring Boot 2.x 不官方支持 Java 25（Java 21 是 2.x 最后支持版本）

**解决方案：**
```
升级路径：
Java 17 → Java 21 (可选中间步骤) → Java 25
同时需要：
Spring Boot 2.7.10 → Spring Boot 3.x
```

**为什么需要同步升级：**
- Spring Boot 3.x 使用 jakarta 命名空间
- Spring Boot 3.x 全面支持 Java 17-25
- Spring Boot 3.0+ 更好地利用 Java 新特性

---

### 2.2 **中等风险 - 需要调整**

#### 问题 3：`java.util.Date` 使用过多
**影响范围：中**

检测到大量使用过时的 `java.util.Date`：
```
- agileboot-domain 中 20+ 个 DTO 和 Entity 类
- agileboot-infrastructure 中的时间处理
- agileboot-common 中的时间工具类
```

**当前使用位置：**
- UserDTO, SysUserEntity
- NoticeDTO, PostDTO, RoleDTO
- OperationLogDTO, LoginLogDTO
- 等众多业务实体

**建议改进（非强制）：**
```java
// 不推荐（当前）
import java.util.Date;
private Date createTime;

// 推荐（Java 8+）
import java.time.LocalDateTime;
private LocalDateTime createTime;
```

**工作量：** 20-30 个文件，但 Spring Data 和 JSON 序列化会自动处理，**实际影响低**

---

#### 问题 4：SimpleDateFormat 使用
**影响范围：低-中**

检测到的位置：
```
- agileboot-common/src/main/java/com/agileboot/common/utils/jackson/JacksonUtil.java
```

**当前代码：**
```java
SimpleDateFormat sdf = new SimpleDateFormat(dateTimeFormat);
objectMapper.setDateFormat(sdf);
```

**问题：** SimpleDateFormat 非线程安全，但在这个上下文中（配置时设置一次）影响有限

---

### 2.3 **低风险 - 注意即可**

#### 问题 5：旧版依赖库版本
**影响范围：低**

某些依赖版本可能需要更新以获得 Java 25 支持：
```
- druid: 1.2.8（建议更新至 1.2.23+）
- mybatis-plus: 3.5.2（建议更新至 3.5.5+）
- guava: 31.0.1（建议更新至 33.0.0+）
- jersey/swagger 相关库
```

**检测状态：** 多数库已支持 Java 17+，升级到 Java 25 时应验证

---

## 3️⃣ 工作量评估

### 3.1 详细工作分解

| 任务 | 工作量 | 难度 | 风险 |
|-----|------|------|------|
| **1. Spring Boot 升级（2.7 → 3.1+）** | 2-3 天 | 中 | 中 |
| **2. javax → jakarta 迁移** | 3-5 天 | 低 | 低 |
| **3. 依赖库版本更新** | 1-2 天 | 低 | 低 |
| **4. java.util.Date 现代化** | 2-4 天（可选） | 低 | 低 |
| **5. 代码测试与验证** | 3-5 天 | 中 | 中 |
| **6. CI/CD 配置更新** | 1 天 | 低 | 低 |
| **总计** | **12-20 天** | 中等 | 中等 |

### 3.2 按优先级分类

#### 🔴 必须完成（关键路径）
1. Spring Boot 2.7.10 → 3.1+（或 3.2+）
2. javax → jakarta 包名替换
3. 依赖库版本兼容性验证
4. 编译和单元测试通过

#### 🟡 建议完成（改进）
1. 升级到 Java 21（中间步骤，可选）
2. java.util.Date → LocalDateTime 迁移
3. 依赖库小版本更新

#### 🟢 可后续处理（优化）
1. SimpleDateFormat 现代化
2. 代码风格优化
3. 性能调优

---

## 4️⃣ 详细升级步骤

### 阶段 1：准备工作（1 天）
```
1.1 备份当前代码
    git branch -b upgrade/java25
    
1.2 分析当前测试覆盖率
    - 目前项目有大量单元测试和集成测试
    - 这是升级的强大保障
    
1.3 更新 CI/CD 配置
    - .github/workflows/ci-cd.yml
    - 添加 Java 21 和 25 的构建矩阵
```

### 阶段 2：Spring Boot 升级（2-3 天）
```
2.1 更新 pom.xml
    - spring.boot.version: 2.7.10 → 3.2.0+
    
2.2 处理 breaking changes
    - 检查 Spring Boot 迁移指南
    - 调整配置文件格式
    - 验证自动配置
    
2.3 更新所有依赖版本
    - 由于 Spring Boot 3.x 要求 Jakarta，很多库也需更新
```

### 阶段 3：包名迁移（3-5 天）
```
3.1 批量替换 javax → jakarta
    - 使用 IDE 全局替换
    - javax.servlet.* → jakarta.servlet.*
    - javax.annotation.* → jakarta.annotation.*
    - javax.validation.* → jakarta.validation.*
    - javax.xml.bind.* → jakarta.xml.bind.*
    
3.2 更新 pom.xml 依赖
    - jaxb-api
    - validation-api
    - servlet-api
    
3.3 验证导入和引用
    - 全局搜索检查是否有遗漏
```

### 阶段 4：依赖版本更新（1-2 天）
```
4.1 关键库版本更新
    druid: 1.2.8 → 1.2.23+
    mybatis-plus: 3.5.2 → 3.5.5+
    guava: 31.0.1 → 33.0.0+
    mysql-connector-java: 8.0.31 → 8.0.33+
    postgresql: 42.7.8 → 42.7.8+（已支持）
    
4.2 可选库升级
    lombok: 1.18.30 → 1.18.30+
    hutool: 5.8.40 → 5.8.40+
```

### 阶段 5：代码现代化（可选，2-4 天）
```
5.1 java.util.Date → LocalDateTime 迁移
    - 影响 20-30 个文件
    - 编辑 JSON 序列化配置
    
5.2 更新时间工具类
    - DatePickUtil 等
    
5.3 评估 SimpleDateFormat 使用
    - 可保持（当前上下文安全）
    - 或替换为 DateTimeFormatter
```

### 阶段 6：测试与验证（3-5 天）
```
6.1 编译验证
    mvn clean compile
    
6.2 运行单元测试
    mvn test
    - 项目有大量测试，这是强有力的验证
    
6.3 集成测试
    mvn integration-test
    
6.4 应用启动测试
    - 本地运行应用
    - 验证所有模块正常加载
    
6.5 功能测试
    - 测试关键业务流程
    - 权限管理
    - 菜单加载
    - 数据库操作
```

### 阶段 7：部署前准备（1 天）
```
7.1 更新 CI/CD 配置
    - GitHub Actions 工作流
    - 添加 Java 25 构建
    
7.2 文档更新
    - README 中的版本标签
    - 升级指南文档
    
7.3 发布准备
    - 版本号更新（考虑 2.0.1 或 2.1.0）
    - 变更日志
```

---

## 5️⃣ 必要的代码更改示例

### 5.1 pom.xml 主要变更

```xml
<!-- 当前 -->
<java.version>17</java.version>
<spring.boot.version>2.7.10</spring.boot.version>

<!-- 升级后 -->
<java.version>21</java.version>  <!-- 或 25 -->
<spring.boot.version>3.2.0</spring.boot.version>
```

### 5.2 包名替换示例

**文件：** `agileboot-infrastructure/src/main/java/com/agileboot/infrastructure/filter/TraceIdFilter.java`

```java
// 替换前
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// 替换后
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
```

**文件：** `agileboot-domain/src/main/java/com/agileboot/domain/common/cache/CacheCenter.java`

```java
// 替换前
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

// 替换后
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
```

### 5.3 时间 API 现代化示例（可选）

```java
// UserDTO.java
// 替换前
import java.util.Date;
public class UserDTO {
    private Date createTime;
    private Date updateTime;
}

// 替换后
import java.time.LocalDateTime;
public class UserDTO {
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

### 5.4 依赖更新示例

```xml
<!-- druid 更新 -->
<druid.version>1.2.8</druid.version>
<!-- 改为 -->
<druid.version>1.2.23</druid.version>

<!-- mybatis-plus 更新 -->
<mybatis-plus.version>3.5.2</mybatis-plus.version>
<!-- 改为 -->
<mybatis-plus.version>3.5.5</mybatis-plus.version>
```

---

## 6️⃣ 风险评估与缓解措施

### 6.1 主要风险清单

| 风险 | 概率 | 影响 | 缓解措施 |
|-----|------|------|--------|
| 依赖库不兼容 | 中 | 高 | 逐步升级，充分测试 |
| 第三方库滞后 | 低 | 中 | 评估替代方案 |
| 数据库驱动问题 | 低 | 高 | 官方驱动已支持，充分测试 |
| 功能回归 | 中 | 高 | **充分利用现有测试套件** |
| 配置不兼容 | 低 | 中 | 详细阅读 Spring Boot 迁移指南 |

### 6.2 缓解策略

1. **充分利用现有测试**
   - ✅ 项目已有大量单元测试和集成测试
   - 这是最强有力的质量保证
   - 在每个阶段都要运行完整测试套件

2. **分阶段升级**
   - 先升级到 Java 21 验证（可选）
   - 再升级到 Java 25
   - 中间验证每一步

3. **依赖版本评估**
   - 升级前检查各库的 Java 25 支持状态
   - 必要时联系库维护者

4. **回滚方案**
   - 使用 git 分支隔离升级
   - 保留原始版本标签
   - 必要时快速回滚

---

## 7️⃣ Java 25 新特性建议应用

升级后可考虑利用的新特性：

### 7.1 Virtual Threads（Java 19+）
```java
// 优化高并发场景
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    // 异步任务处理
}
```

### 7.2 Records（Java 14+）
```java
// 简化 DTO 定义
public record UserDTO(
    Long id,
    String username,
    LocalDateTime createTime
) {}
```

### 7.3 Pattern Matching（Java 16+）
```java
// 简化类型检查
if (obj instanceof String str) {
    System.out.println(str.length());
}
```

### 7.4 改进的 Stream API
```java
// Java 21+ 新增虚拟线程增强流处理
```

---

## 8️⃣ 检查清单

### 升级前检查
- [ ] 代码已提交到 git
- [ ] 创建升级分支 `upgrade/java25`
- [ ] 本地环境配置正确
- [ ] 所有测试现在通过

### 升级中检查
- [ ] Spring Boot 升级完成
- [ ] javax → jakarta 替换完成
- [ ] 所有编译错误已解决
- [ ] 依赖版本更新完成

### 升级后检查
- [ ] 编译通过（`mvn clean compile`）
- [ ] 所有单元测试通过
- [ ] 所有集成测试通过
- [ ] 应用成功启动
- [ ] 关键功能测试通过
- [ ] CI/CD 管道正常
- [ ] 版本号已更新
- [ ] 文档已更新

---

## 9️⃣ 预计时间表

```
Week 1:
  Day 1: 准备工作 (分支、备份、计划)
  Day 2-3: Spring Boot 升级
  Day 4-5: javax → jakarta 迁移

Week 2:
  Day 1-2: 依赖版本更新 + 编译修复
  Day 3-4: 单元测试和集成测试
  Day 5: 功能测试和最终验证

总计：约 10-12 个工作日
```

---

## 🔟 其他建议

### 10.1 长期规划
- 考虑在升级完成后 3-6 个月进行下一次升级评估
- Java 的发布周期是 6 个月，保持跟进
- Spring Boot 也会持续更新，定期评估

### 10.2 文档维护
- 更新 README 中的版本标志
- 补充升级日志
- 记录任何遇到的问题和解决方案

### 10.3 团队沟通
- 提前告知所有开发者升级计划
- 在升级期间避免并行开发
- 升级完成后同步给全体

---

## 📋 总结

### ✅ 升级到 Java 25 的主要结论

**可行性：⭐⭐⭐⭐☆ 高度可行**

**工作量：12-20 个工作日（中等）**

**关键障碍：**
1. Spring Boot 版本升级（2.7 → 3.x）**【必须】**
2. javax → jakarta 包名迁移 **【必须】**
3. 依赖库版本更新 **【必须】**

**项目优势：**
- ✅ 项目有大量单元测试和集成测试（强大的质量保障）
- ✅ 模块化架构清晰（便于分阶段升级）
- ✅ 代码规范（易于维护和迁移）
- ✅ 使用现代框架（Spring Boot、Spring Security）

**建议：**
1. **立即开始** - 没有技术障碍
2. **阶段升级** - 先升级 Spring Boot 和基础包，再升级 Java 版本
3. **充分测试** - 充分利用现有的测试套件进行验证
4. **文档记录** - 记录升级过程中遇到的问题和解决方案

---

**报告生成日期：** 2026-04-07
**评估版本：** AgileBoot v2.0.0
