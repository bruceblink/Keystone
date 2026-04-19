# Docker 开发环境

## 服务组成

| 服务 | 镜像 | 容器名 | 宿主机端口 |
|------|------|--------|-----------|
| MySQL 8.4 | `mysql:8.4` | `keystone-mysql` | `3306` |
| Redis 7.2 | `redis:7.2` | `keystone-redis` | `6379` |
| Spring Boot 后端 | `keystone-admin:dev` | `keystone-admin` | `8080` |

> 所有服务时区均为 **UTC**。

---

## 快速启动

### 1. 仅启动中间件（MySQL + Redis，本地 IDE 运行应用）

```bash
cd docker
docker compose up -d mysql redis
```

### 2. 启动完整环境（含构建应用镜像）

```bash
cd docker
docker compose up -d --build
```

### 3. 查看日志

```bash
# 所有服务
docker compose logs -f

# 单个服务
docker compose logs -f keystone-admin
docker compose logs -f mysql
```

---

## 环境变量

默认值在 `docker/.env` 中。如需本地覆盖（例如修改密码），创建 `docker/.env.local`：

```bash
cp .env .env.local
# 编辑 .env.local，修改 MYSQL_ROOT_PASSWORD / REDIS_PASSWORD 等
```

然后启动时指定：

```bash
docker compose --env-file .env.local up -d
```

---

## 停止 / 销毁

```bash
# 停止（保留 volume 数据）
docker compose down

# 停止并删除所有数据（慎用）
docker compose down -v
```

---

## 本地 IDE 开发时的连接配置

`application-dev.yml` 已配置好宿主机访问地址，无需修改：

- MySQL: `localhost:3306` / DB: `keystone` / User: `root` / Pass: `12345`
- Redis: `localhost:6379` / Pass: `12345`

Spring Boot 启动时使用 profile：`basic,dev`

