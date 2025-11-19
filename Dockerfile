# Runtime stage (distroless)
FROM debian:bookworm-slim AS runtime
RUN useradd --system --uid 1000 --create-home --home-dir /home/appuser appuser
WORKDIR /home/appuser

# Download OpenTelemetry Java agent for Honeycomb integration
RUN apt-get update && apt-get install -y wget && \
    wget -O opentelemetry-javaagent.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.42.1/opentelemetry-javaagent.jar && \
    apt-get remove -y wget && apt-get autoremove -y && apt-get clean && rm -rf /var/lib/apt/lists/*

# Copy jar and set ownership to non-root user
COPY target/*.jar /home/appuser/app.jar
RUN chown appuser:appuser /home/appuser/app.jar /home/appuser/opentelemetry-javaagent.jar && \
    chmod 500 /home/appuser/app.jar && \
    chmod 400 /home/appuser/opentelemetry-javaagent.jar

FROM gcr.io/distroless/java21-debian12
WORKDIR /app
# Copy the jar and agent from the intermediate runtime image; they already have correct ownership
COPY --from=runtime /home/appuser/app.jar /app/app.jar
COPY --from=runtime /home/appuser/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar
EXPOSE 8081
USER 1000:1000
ENTRYPOINT ["java", "-javaagent:/app/opentelemetry-javaagent.jar", "-jar", "/app/app.jar"]
