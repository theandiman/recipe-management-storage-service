# Logging Improvements

This document outlines the observability improvements implemented in the Recipe Management Storage Service, focusing on structured JSON logging and OpenTelemetry integration.

## Overview

The service has been enhanced with structured JSON logging using the Logstash Logback Encoder, enabling better log analysis, correlation with distributed traces, and integration with modern observability platforms like Honeycomb.

## Implementation Details

### Structured JSON Logging

**Dependency Added:**
```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

**Configuration:** `src/main/resources/logback-spring.xml`

The logging configuration provides:
- JSON-formatted log output for all log messages
- Consistent structure for easier parsing and analysis
- Integration with log aggregation and analysis tools

### Service Metadata

Every log entry automatically includes service metadata:

- **service**: Service name (default: `recipe-storage-service`)
- **version**: Application version from Spring Boot properties

This metadata helps identify logs from specific services and versions in multi-service environments.

### MDC (Mapped Diagnostic Context) Fields

The following MDC fields are included in logs for distributed tracing and request correlation:

| Field | Description | Example |
|-------|-------------|---------|
| `trace_id` | OpenTelemetry trace ID | `5b8aa5a2d2c872e8321cf37308d69df2` |
| `span_id` | OpenTelemetry span ID | `051581bf3cb55c13` |
| `user.id` | Authenticated user identifier | `user123` |
| `recipe.id` | Recipe identifier in request context | `recipe456` |
| `request.id` | Unique request identifier | `req-789` |

These fields enable:
- Correlation of logs with distributed traces
- Filtering logs by specific users or resources
- Following a request's journey through the system

### Stack Trace Enhancement

Exception stack traces in JSON logs are optimized with:

- **Maximum depth**: 30 levels per throwable
- **Maximum length**: 2048 characters
- **Shortened class names**: Limited to 20 characters for readability
- **Root cause first**: Most relevant error shown first
- **Filtered frames**: Common reflection and proxy frames excluded

This configuration provides meaningful error information while keeping log sizes manageable.

### Log Levels

Package-specific log levels are configured for optimal visibility:

| Package | Level | Purpose |
|---------|-------|---------|
| `com.recipe.storage` | DEBUG | Detailed application logs |
| `org.springframework.web` | DEBUG | HTTP request/response details |
| `com.google.cloud.firestore` | DEBUG | Firestore operation details |
| Root logger | INFO | Default level for all other packages |

## Integration with OpenTelemetry

The logging configuration is designed to work seamlessly with OpenTelemetry:

1. **Automatic trace correlation**: `trace_id` and `span_id` from OpenTelemetry context are automatically included in logs
2. **Consistent naming**: Field names align with OpenTelemetry semantic conventions
3. **Honeycomb integration**: JSON logs can be sent directly to Honeycomb or other OTLP-compatible backends

## Usage

### Adding Context to Logs

Use SLF4J's MDC to add custom context:

```java
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecipeService {
    private static final Logger log = LoggerFactory.getLogger(RecipeService.class);
    
    public void processRecipe(String recipeId) {
        MDC.put("recipe.id", recipeId);
        try {
            log.info("Processing recipe");
            // Business logic
        } finally {
            MDC.remove("recipe.id");
        }
    }
}
```

### Log Examples

**Standard log entry:**
```json
{
  "@timestamp": "2024-12-10T09:58:14.576Z",
  "message": "Processing recipe",
  "logger_name": "com.recipe.storage.service.RecipeService",
  "level": "INFO",
  "service": "recipe-storage-service",
  "version": "0.0.17-SNAPSHOT",
  "trace_id": "5b8aa5a2d2c872e8321cf37308d69df2",
  "span_id": "051581bf3cb55c13",
  "recipe.id": "recipe456"
}
```

**Error with stack trace:**
```json
{
  "@timestamp": "2024-12-10T09:58:15.123Z",
  "message": "Failed to save recipe",
  "logger_name": "com.recipe.storage.service.RecipeService",
  "level": "ERROR",
  "service": "recipe-storage-service",
  "version": "0.0.17-SNAPSHOT",
  "trace_id": "5b8aa5a2d2c872e8321cf37308d69df2",
  "span_id": "051581bf3cb55c13",
  "recipe.id": "recipe456",
  "stack_trace": "com.google.cloud.firestore.FirestoreException: Failed to write\n\tat c.g.c.f.Firestore.save(Firestore.java:123)\n..."
}
```

## Environment Configuration

For local development with observability:

1. Copy `.env.honeycomb.example` to `.env`
2. Set your `HONEYCOMB_API_KEY`
3. Run with: `./start-with-observability.sh`

See the main [README.md](README.md#observability) for complete setup instructions.

## Benefits

1. **Improved debugging**: Structured logs with trace correlation make it easier to find and understand errors
2. **Better monitoring**: Consistent JSON format enables advanced querying and alerting
3. **Distributed tracing**: Automatic correlation between logs and traces
4. **Production readiness**: Optimized for high-volume production environments
5. **Developer experience**: Detailed DEBUG logs for local development

## Future Enhancements

Potential future improvements:
- Add custom JSON providers for specific log patterns
- Implement log sampling for high-volume endpoints
- Add metrics derived from log events
- Enhanced error categorization and tagging
