# --------------------------
# 第一阶段：构建阶段
# --------------------------
ARG BUILDER_BASE_IMAGE=ghcr.io/adoptium/temurin:25-jdk-jammy
ARG RUNTIME_BASE_IMAGE=ghcr.io/adoptium/temurin:25-jre-alpine
FROM ${BUILDER_BASE_IMAGE} AS build

WORKDIR /workspace

# 允许通过构建参数注入企业/代理根证书（base64）以修复 Maven TLS 握手失败
ARG EXTRA_CA_CERT_BASE64=""

RUN apt-get update \
  && apt-get install -y --no-install-recommends ca-certificates \
  && rm -rf /var/lib/apt/lists/* \
  && if [ -n "$EXTRA_CA_CERT_BASE64" ]; then \
       echo "$EXTRA_CA_CERT_BASE64" | base64 -d > /usr/local/share/ca-certificates/extra-ca.crt; \
       update-ca-certificates; \
     fi

# 先复制 Gradle 描述文件，尽量复用依赖层缓存
COPY gradlew gradlew.bat build.gradle settings.gradle gradle.properties ./
COPY gradle ./gradle
COPY keystone-admin/build.gradle ./keystone-admin/
COPY keystone-api/build.gradle ./keystone-api/
COPY keystone-common/build.gradle ./keystone-common/
COPY keystone-domain/build.gradle ./keystone-domain/
COPY keystone-infrastructure/build.gradle ./keystone-infrastructure/

# 再复制源码，避免无关文件进入构建上下文
COPY keystone-admin/src ./keystone-admin/src
COPY keystone-api/src ./keystone-api/src
COPY keystone-common/src ./keystone-common/src
COPY keystone-domain/src ./keystone-domain/src
COPY keystone-infrastructure/src ./keystone-infrastructure/src

# 只构建 keystone-admin 的可执行 Jar，并提取分层内容供运行镜像复用
RUN chmod +x gradlew \
  && ./gradlew --no-daemon --no-configuration-cache :keystone-admin:bootJar -x test \
  && mkdir -p /layers \
  && cd /layers \
  && java -Djarmode=tools -jar /workspace/keystone-admin/build/libs/keystone-admin.jar extract --layers --launcher

# --------------------------
# 第二阶段：运行阶段
# --------------------------
FROM ${RUNTIME_BASE_IMAGE}

WORKDIR /app

ENV TZ=UTC \
  JAVA_TOOL_OPTIONS="-XX:InitialRAMPercentage=25.0 -XX:MaxRAMPercentage=75.0 -XX:MaxMetaspaceSize=256m -XX:MaxDirectMemorySize=128m -XX:+HeapDumpOnOutOfMemoryError -XX:+ExitOnOutOfMemoryError -XX:HeapDumpPath=/app/logs"

RUN addgroup -S keystone \
  && adduser -S keystone -G keystone \
  && mkdir -p /app/logs /app/data \
  && chown -R keystone:keystone /app

COPY --from=build --chown=keystone:keystone /layers/dependencies/ ./
COPY --from=build --chown=keystone:keystone /layers/spring-boot-loader/ ./
COPY --from=build --chown=keystone:keystone /layers/snapshot-dependencies/ ./
COPY --from=build --chown=keystone:keystone /layers/application/ ./

USER keystone

EXPOSE 18080

ENTRYPOINT ["java", "-Dname=keystone-admin.jar", "-Duser.timezone=UTC", "org.springframework.boot.loader.launch.JarLauncher"]