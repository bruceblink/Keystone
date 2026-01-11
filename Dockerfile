# --------------------------
# 第一阶段：构建阶段
# --------------------------
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app

# 复制整个项目源码到构建镜像
COPY . .

# 在父项目根目录打包整个多模块项目，只构建 jar，跳过测试
RUN ./mvnw clean package -DskipTests

# --------------------------
# 第二阶段：运行阶段
# --------------------------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 只复制 agileboot-admin 模块下的可执行 jar 到运行镜像
COPY --from=build /app/agileboot-admin/target/agileboot-admin.jar ./agileboot-admin.jar

# 创建日志目录
RUN mkdir -p /app/logs

# 暴露端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java","-Dname=agileboot-admin.jar","-Duser.timezone=Asia/Shanghai","-Xms256m","-Xmx512m","-XX:MetaspaceSize=128m","-XX:MaxMetaspaceSize=256m","-XX:+HeapDumpOnOutOfMemoryError","-jar","agileboot-admin.jar"]