#!/bin/bash

# Recipe Storage Service Startup Script with Honeycomb Observability
# This script starts the service with OpenTelemetry agent for local development

set -e

echo "🚀 Starting Recipe Storage Service with Honeycomb observability..."

# Check if HONEYCOMB_API_KEY is set
if [ -z "$HONEYCOMB_API_KEY" ]; then
    echo "⚠️  Warning: HONEYCOMB_API_KEY environment variable not set"
    echo "   Honeycomb tracing will not be available"
    echo "   Set it with: export HONEYCOMB_API_KEY=your_api_key_here"
fi

# Download OpenTelemetry Java agent if not present
AGENT_JAR="opentelemetry-javaagent.jar"
AGENT_URL="https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.42.1/opentelemetry-javaagent.jar"

if [ ! -f "$AGENT_JAR" ]; then
    echo "📥 Downloading OpenTelemetry Java agent..."
    curl -L -o "$AGENT_JAR" "$AGENT_URL"
    echo "✅ OpenTelemetry agent downloaded"
fi

# Set JVM options for OpenTelemetry
OTEL_OPTS="-javaagent:$AGENT_JAR"
OTEL_OPTS="$OTEL_OPTS -Dotel.service.name=recipe-storage-service"
OTEL_OPTS="$OTEL_OPTS -Dotel.service.version=0.0.1-SNAPSHOT"
OTEL_OPTS="$OTEL_OPTS -Dotel.traces.exporter=otlp"
OTEL_OPTS="$OTEL_OPTS -Dotel.metrics.exporter=otlp"
OTEL_OPTS="$OTEL_OPTS -Dotel.exporter.otlp.endpoint=https://api.honeycomb.io:443"
OTEL_OPTS="$OTEL_OPTS -Dotel.exporter.otlp.protocol=grpc"
OTEL_OPTS="$OTEL_OPTS -Dotel.resource.attributes=service.instance.id=$HOSTNAME"

# Add API key if available
if [ -n "$HONEYCOMB_API_KEY" ]; then
    OTEL_OPTS="$OTEL_OPTS -Dotel.exporter.otlp.headers=api-key=$HONEYCOMB_API_KEY"
fi

# Set Java home if available
JAVA_CMD="java"
if [ -n "$JAVA_HOME" ]; then
    JAVA_CMD="$JAVA_HOME/bin/java"
fi

echo "🔍 Starting service with observability enabled..."
echo "   Service: recipe-storage-service"
echo "   Traces: https://ui.honeycomb.io/"
echo "   Metrics: https://ui.honeycomb.io/"

# Start the application
exec $JAVA_CMD $OTEL_OPTS -jar target/recipe-storage-service-0.0.1-SNAPSHOT.jar