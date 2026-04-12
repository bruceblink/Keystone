# AgileBoot 数据库密码加密使用说明

本文档说明如何在 AgileBoot 中使用 `ENC(...)` 形式的数据库密码配置。

## 1. 功能概览

项目已支持在配置文件中对数据库密码使用密文格式：

- 明文格式：`password: 12345`
- 密文格式：`password: ENC(xxxxxxxxx)`

应用启动时会自动识别 `ENC(...)` 并解密后注入数据源。

支持的密码配置键：

- `spring.datasource.password`
- `spring.datasource.dynamic.datasource.<name>.password`

## 2. 启用配置

在基础配置中已提供以下参数：

- `agileboot.datasource.password-encryption.enabled`
- `agileboot.datasource.password-encryption.encrypt-key`

推荐通过环境变量注入：

- `AGILEBOOT_DATASOURCE_PASSWORD_ENCRYPTION_ENABLED=true`
- `AGILEBOOT_DATASOURCE_ENCRYPT_KEY=<你的密钥>`

注意：

- 如果密码配置为 `ENC(...)`，但未提供 `encrypt-key`，应用会在启动时报错并终止。
- 建议密钥长度不少于 16 字符，并妥善保管。

## 3. 生成密文

已提供工具类：

- `com.agileboot.infrastructure.security.DataSourcePasswordEncryptor`

用法：

```bash
java DataSourcePasswordEncryptor <encryptKey> <plainPassword>
```

输出示例：

```text
ENC(2M5xYfD8iZk7...)
```

你可以在 IDE 中直接运行该 `main` 方法，传入两个参数后复制输出结果。

## 4. 配置示例

### 4.1 Docker Compose 环境变量

```yaml
AGILEBOOT_DATASOURCE_PASSWORD_ENCRYPTION_ENABLED: true
AGILEBOOT_DATASOURCE_ENCRYPT_KEY: your-secret-key
SPRING_DATASOURCE_PASSWORD: ENC(2M5xYfD8iZk7...)
```

### 4.2 application-docker.yml

```yaml
spring:
  datasource:
    dynamic:
      datasource:
        master:
          password: ${SPRING_DATASOURCE_PASSWORD:12345}
```

当 `SPRING_DATASOURCE_PASSWORD` 是 `ENC(...)` 时会自动解密。

## 5. 排错指南

1. 启动报错提示缺少密钥
- 检查是否设置了 `AGILEBOOT_DATASOURCE_ENCRYPT_KEY`。
- 检查 `AGILEBOOT_DATASOURCE_PASSWORD_ENCRYPTION_ENABLED` 是否为 `true`。

2. 启动后数据库认证失败
- 确认密钥与生成密文时使用的密钥完全一致。
- 确认 `ENC(...)` 内容没有被截断或包含多余空格。

3. 配置未生效
- 确认密码键是否为支持的路径（见第 1 节）。
- 确认当前激活 profile 下确实加载到了对应配置。

## 6. 安全建议

- 不要把明文密码写入仓库。
- 密钥建议通过 CI/CD 密钥管理或容器 Secret 注入。
- 定期轮换密钥与数据库密码。
