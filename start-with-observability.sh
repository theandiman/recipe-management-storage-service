#!/bin/bash

# Recipe Storage Service Startup Script with Honeycomb Observability
# This script starts the service with OpenTelemetry agent for local development

set -e

# Configuration - update these versions as needed
OTEL_AGENT_VERSION="${OTEL_AGENT_VERSION:-v2.21.0}"
SERVICE_NAME="${SERVICE_NAME:-recipe-storage-service}"
SERVICE_VERSION="${SERVICE_VERSION:-0.0.1-SNAPSHOT}"

echo "🚀 Starting ${SERVICE_NAME} with Honeycomb observability..."

# Check if HONEYCOMB_API_KEY is set
if [ -z "$HONEYCOMB_API_KEY" ]; then
    echo "⚠️  Warning: HONEYCOMB_API_KEY environment variable not set"
    echo "   Honeycomb tracing will not be available"
    echo "   Set it with: export HONEYCOMB_API_KEY=your_api_key_here"
fi

# Download OpenTelemetry Java agent if not present
AGENT_JAR="opentelemetry-javaagent.jar"
AGENT_URL="https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar"

if [ ! -f "$AGENT_JAR" ]; then
    echo "📥 Downloading OpenTelemetry Java agent (${OTEL_AGENT_VERSION})..."
    if command -v curl >/dev/null 2>&1; then
        curl -L -o "$AGENT_JAR" "$AGENT_URL"
    elif command -v wget >/dev/null 2>&1; then
        wget -O "$AGENT_JAR" "$AGENT_URL"
    else
        echo "❌ Neither curl nor wget found. Please install one of them."
        exit 1
    fi

    # Verify download
    if [ ! -s "$AGENT_JAR" ]; then
        echo "❌ Failed to download OpenTelemetry agent"
        rm -f "$AGENT_JAR"
        exit 1
    fi

    echo "✅ OpenTelemetry agent downloaded successfully"
fi

# Set JVM options for OpenTelemetry
# Note: Configuration is handled by application.properties to avoid duplication
OTEL_OPTS="-javaagent:$AGENT_JAR"

# Set Java home if available
JAVA_CMD="java"
if [ -n "$JAVA_HOME" ]; then
    JAVA_CMD="$JAVA_HOME/bin/java"
fi

echo "🔍 Starting service with observability enabled..."
echo "   Service: ${SERVICE_NAME}"
echo "   Version: ${SERVICE_VERSION}"
echo "   Traces: https://ui.honeycomb.io/"
echo "   Metrics: https://ui.honeycomb.io/"
echo "   Agent: ${OTEL_AGENT_VERSION}"

# Start the application
exec $JAVA_CMD $OTEL_OPTS -jar target/${SERVICE_NAME}-${SERVICE_VERSION}.jar
