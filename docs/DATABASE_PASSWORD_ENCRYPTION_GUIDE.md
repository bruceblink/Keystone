# Keystone 数据库密码加密使用说明

本文档说明如何在 Keystone 中使用统一 AES-256-GCM 密文格式配置数据库密码。

## 1. 功能概览

推荐密文格式：

```text
secret:v1:aes-256-gcm:<nonce_base64>:<ciphertext_base64>
```

Keystone 启动时会在 `keystone.datasource.password-encryption.enabled=true` 时自动识别并解密该格式，然后注入数据源。

兼容说明：旧 `ENC(...)` AES/ECB 密文仍可解密，但不再推荐新部署使用。

支持的密码配置键：

- `spring.datasource.password`
- `spring.datasource.dynamic.datasource.<name>.password`

## 2. 启用配置

在基础配置中已提供以下参数：

- `keystone.datasource.password-encryption.enabled`
- `keystone.datasource.password-encryption.encrypt-key`

推荐通过环境变量注入：

- `KEYSTONE_DATASOURCE_PASSWORD_ENCRYPTION_ENABLED=true`
- `KEYSTONE_DATASOURCE_ENCRYPT_KEY=<32 字节原文密钥或 32 字节标准 base64 密钥>`

注意：

- 如果密码配置为密文，但未提供 `encrypt-key`，应用会在启动时报错并终止。
- 新格式要求密钥为 32 字节或标准 base64 编码后的 32 字节。

## 3. 生成密文

已提供工具类：

- `app.keystone.infrastructure.security.DataSourcePasswordEncryptor`

用法：

```bash
java DataSourcePasswordEncryptor <encryptKey> <plainPassword>
```

输出示例：

```text
secret:v1:aes-256-gcm:...
```

也可以使用 Keylo 仓库的 `scripts/secret_tool.py` 或其他项目中的 `scripts/encrypt_db_password.py` 生成同一格式密文。

## 4. 配置示例

### 4.1 Docker Compose 环境变量

```yaml
KEYSTONE_DATASOURCE_PASSWORD_ENCRYPTION_ENABLED: true
KEYSTONE_DATASOURCE_ENCRYPT_KEY: your-base64-32-byte-key
SPRING_DATASOURCE_PASSWORD: secret:v1:aes-256-gcm:...
```

### 4.2 application-dev.yml / application-prod.yml

```yaml
spring:
  datasource:
    dynamic:
      datasource:
        master:
          password: ${SPRING_DATASOURCE_PASSWORD:12345}
```

## 5. 排错指南

1. 启动报错提示缺少密钥
   - 检查是否设置了 `KEYSTONE_DATASOURCE_ENCRYPT_KEY`。
   - 检查 `KEYSTONE_DATASOURCE_PASSWORD_ENCRYPTION_ENABLED` 是否为 `true`。

2. 启动后数据库认证失败
   - 确认密钥与生成密文时使用的密钥完全一致。
   - 确认数据库实际密码与密文中的明文一致。

3. 解密失败
   - 确认密文没有被截断。
   - 确认密钥是 32 字节或标准 base64 编码后的 32 字节。
   - 确认密文格式以 `secret:v1:aes-256-gcm:` 开头。

## 6. 安全建议

- 不要把明文密码写入仓库。
- 密钥建议通过 CI/CD 密钥管理或容器 Secret 注入。
- 定期轮换密钥与数据库密码。
