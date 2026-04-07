# --------------------------
# 第一阶段：构建阶段
# --------------------------
FROM eclipse-temurin:25-jdk-jammy AS build

WORKDIR /app

# 复制整个项目源码到构建镜像
COPY . .

# 构建多模块项目，跳过测试
RUN ./mvnw -B clean package -DskipTests

# --------------------------
# 第二阶段：运行阶段
# --------------------------
FROM eclipse-temurin:25-jre-jammy

WORKDIR /app

# 只复制 agileboot-admin 模块下的可执行 jar 到运行镜像
COPY --from=build /app/agileboot-admin/target/agileboot-admin.jar ./agileboot-admin.jar

# 创建日志目录
RUN mkdir -p /app/logs

# 暴露端口
EXPOSE 8080

ENTRYPOINT exec java \
  -Dname=agileboot-admin.jar \
  -Duser.timezone=Asia/Shanghai \
  -Xms64m \
  -Xmx256m \
  -XX:MaxMetaspaceSize=128m \
  -XX:MaxDirectMemorySize=64m \
  -XX:+HeapDumpOnOutOfMemoryError \
  -jar agileboot-admin.jar