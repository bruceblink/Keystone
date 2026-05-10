ARG BUILDER_BASE_IMAGE=eclipse-temurin:25-jdk
ARG RUNTIME_BASE_IMAGE=eclipse-temurin:25-jre

FROM ${BUILDER_BASE_IMAGE} AS builder

WORKDIR /workspace

COPY . .

RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew --no-daemon :keystone-admin:bootJar

FROM ${RUNTIME_BASE_IMAGE}

WORKDIR /app

ENV TZ=UTC \
  JAVA_TOOL_OPTIONS="-XX:InitialRAMPercentage=25.0 -XX:MaxRAMPercentage=75.0 -XX:MaxMetaspaceSize=256m -XX:MaxDirectMemorySize=128m -XX:+HeapDumpOnOutOfMemoryError -XX:+ExitOnOutOfMemoryError -XX:HeapDumpPath=/app/logs"

COPY --from=builder /workspace/keystone-admin/build/libs/keystone-admin.jar /app/keystone-admin.jar

VOLUME ["/app/logs", "/app/data", "/app/config"]

EXPOSE 18080

ENTRYPOINT ["sh", "-c", "mkdir -p /app/logs /app/data /app/config && exec java -Dname=keystone-admin.jar -Duser.timezone=UTC -jar /app/keystone-admin.jar"]
