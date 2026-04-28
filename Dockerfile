ARG RUNTIME_BASE_IMAGE=eclipse-temurin:25-jre
FROM ${RUNTIME_BASE_IMAGE}

WORKDIR /app

ENV TZ=UTC \
  JAVA_TOOL_OPTIONS="-XX:InitialRAMPercentage=25.0 -XX:MaxRAMPercentage=75.0 -XX:MaxMetaspaceSize=256m -XX:MaxDirectMemorySize=128m -XX:+HeapDumpOnOutOfMemoryError -XX:+ExitOnOutOfMemoryError -XX:HeapDumpPath=/app/logs"

RUN if command -v apk >/dev/null 2>&1; then \
      addgroup -S keystone && adduser -S keystone -G keystone; \
    else \
      groupadd -r keystone && useradd -r -g keystone keystone; \
    fi \
  && mkdir -p /app/logs /app/data /app/config \
  && chown -R keystone:keystone /app

COPY --chown=keystone:keystone keystone-admin/build/libs/keystone-admin.jar /app/keystone-admin.jar

VOLUME ["/app/logs", "/app/data", "/app/config"]

USER keystone

EXPOSE 18080

ENTRYPOINT ["java", "-Dname=keystone-admin.jar", "-Duser.timezone=UTC", "-jar", "/app/keystone-admin.jar"]
