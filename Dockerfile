ARG BUILDER_BASE_IMAGE=ghcr.io/graalvm/native-image-community:25
ARG RUNTIME_BASE_IMAGE=debian:bookworm-slim

FROM ${BUILDER_BASE_IMAGE} AS builder

WORKDIR /workspace

COPY . .

RUN sed -i 's/\r$//' gradlew \\
    && chmod +x gradlew \\
    && ./gradlew --no-daemon :keystone-admin:nativeCompile

FROM ${RUNTIME_BASE_IMAGE}

WORKDIR /app

ENV TZ=UTC

COPY --from=builder /workspace/keystone-admin/build/native/nativeCompile/keystone-admin /app/keystone-admin

VOLUME ["/app/logs", "/app/data", "/app/config"]

EXPOSE 18080

RUN useradd --system --uid 10001 --create-home keystone \\
    && mkdir -p /app/logs /app/data /app/config \\
    && chown -R keystone:keystone /app

USER keystone

ENTRYPOINT ["/app/keystone-admin"]
