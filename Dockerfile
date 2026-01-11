# --------------------------
# 第一阶段：构建阶段
# --------------------------
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app

# 复制项目
COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY src src

# 构建 jar，跳过测试
RUN ./mvnw clean package -DskipTests

# --------------------------
# 第二阶段：运行阶段
# --------------------------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 复制构建好的 jar 文件
COPY --from=build /app/target/*.jar agileboot-admin.jar

# 创建日志目录
RUN mkdir -p /app/logs

# 设置 JVM 参数
ENV JVM_OPTS="-Dname=agileboot-admin.jar -Duser.timezone=Asia/Shanghai \
    -Xms256m -Xmx1024m \
    -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m \
    -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDateStamps -XX:+PrintGCDetails \
    -XX:NewRatio=1 -XX:SurvivorRatio=30 -XX:+UseParallelGC -XX:+UseParallelOldGC"

# 暴露端口
EXPOSE 8080

# 直接启动 Java 应用（nohup / & 不需要 Docker 容器里用）
ENTRYPOINT ["sh", "-c", "java $JVM_OPTS -jar agileboot-admin.jar >> /app/logs/agileboot-admin.jar.log 2>&1"]