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

ENV TZ=UTC \
  JAVA_OPTS="-XX:InitialRAMPercentage=25.0 -XX:MaxRAMPercentage=75.0 -XX:MaxMetaspaceSize=256m -XX:MaxDirectMemorySize=128m -XX:+HeapDumpOnOutOfMemoryError -XX:+ExitOnOutOfMemoryError -XX:HeapDumpPath=/app/logs"

# 只复制 agileboot-admin 模块下的可执行 jar 到运行镜像
COPY --from=build /app/agileboot-admin/target/agileboot-admin.jar ./agileboot-admin.jar

# 创建日志目录
RUN mkdir -p /app/logs

# 暴露端口
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java -Dname=agileboot-admin.jar -Duser.timezone=${TZ} ${JAVA_OPTS} -jar /app/agileboot-admin.jar"]