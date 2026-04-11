# --------------------------
# 第一阶段：构建阶段
# --------------------------
FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

# 复制整个项目源码到构建镜像
COPY . .

# 容器内统一走 Maven Central，避免镜像构建受第三方镜像源校验异常影响
RUN mkdir -p /root/.m2 && \
  printf '%s\n' \
  '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">' \
  '  <mirrors>' \
  '    <mirror>' \
  '      <id>central</id>' \
  '      <name>Maven Central</name>' \
  '      <url>https://repo.maven.apache.org/maven2</url>' \
  '      <mirrorOf>*</mirrorOf>' \
  '    </mirror>' \
  '  </mirrors>' \
  '</settings>' > /root/.m2/settings.xml

# 构建多模块项目，跳过测试
RUN ./mvnw -B clean package -DskipTests

# --------------------------
# 第二阶段：运行阶段
# --------------------------
FROM eclipse-temurin:25-jre

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