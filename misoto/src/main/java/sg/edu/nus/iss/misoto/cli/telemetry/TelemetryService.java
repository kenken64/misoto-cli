package sg.edu.nus.iss.misoto.cli.telemetry;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Telemetry Service
 * 
 * Collects anonymous usage data and error reports to help improve the tool.
 * Respects user privacy and can be disabled.
 */
@Service
@Slf4j
public class TelemetryService {
    
    private final Queue<TelemetryEvent> eventQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean enabled = new AtomicBoolean(false);
    private final String sessionId = UUID.randomUUID().toString();
    private final Map<String, Object> sessionContext = new HashMap<>();
    
    private TelemetryConfig config;
    
    /**
     * Initialize telemetry service with configuration
     */
    public void initialize(TelemetryConfig config) {
        this.config = config;
        this.enabled.set(config.isEnabled());
        
        if (enabled.get()) {
            log.debug("Telemetry service enabled with session ID: {}", sessionId);
            
            // Set session context
            sessionContext.put("sessionId", sessionId);
            sessionContext.put("startTime", Instant.now());
            sessionContext.put("platform", getPlatformInfo());
            sessionContext.put("version", getApplicationVersion());
            
            // Record session start
            recordEvent(TelemetryEventType.SESSION_START, Map.of(
                "sessionId", sessionId
            ));
        } else {
            log.debug("Telemetry service disabled");
        }
    }
    
    /**
     * Record a telemetry event
     */    public void recordEvent(TelemetryEventType type, Map<String, Object> data) {
        if (!enabled.get()) {
            return;
        }
        
        try {
            TelemetryEvent event = new TelemetryEvent();
            event.setType(type);
            event.setTimestamp(Instant.now());
            event.setSessionId(sessionId);
            
            // Handle null data parameter
            Map<String, Object> eventData = data != null ? new HashMap<>(data) : new HashMap<>();
            event.setData(eventData);
            
            // Add session context
            event.getData().putAll(sessionContext);
            
            eventQueue.offer(event);
            
            // Process queue if it's getting large
            if (eventQueue.size() >= config.getMaxQueueSize()) {
                processQueue();
            }
            
            log.debug("Recorded telemetry event: {} with {} data points", type, eventData.size());
        } catch (Exception e) {
            log.warn("Failed to record telemetry event", e);
        }
    }
    
    /**
     * Record command execution
     */
    public void recordCommandExecution(String command, long durationMs, boolean success) {
        recordEvent(TelemetryEventType.COMMAND_EXECUTE, Map.of(
            "command", command,
            "duration", durationMs,
            "success", success
        ));
    }
    
    /**
     * Record command success
     */
    public void recordCommandSuccess(String command, long durationMs) {
        recordEvent(TelemetryEventType.COMMAND_SUCCESS, Map.of(
            "command", command,
            "duration", durationMs
        ));
    }
      /**
     * Record command error
     */
    public void recordCommandError(String command, String errorType, String errorMessage) {
        Map<String, Object> params = new HashMap<>();
        params.put("command", command);
        params.put("errorType", errorType);
        String sanitizedMessage = sanitizeErrorMessage(errorMessage);
        if (sanitizedMessage != null) {
            params.put("errorMessage", sanitizedMessage);
        }
        recordEvent(TelemetryEventType.COMMAND_ERROR, params);
    }
    
    /**
     * Record AI request
     */
    public void recordAiRequest(String model, int tokensUsed, long durationMs) {
        recordEvent(TelemetryEventType.AI_REQUEST, Map.of(
            "model", model,
            "tokensUsed", tokensUsed,
            "duration", durationMs
        ));
    }
    
    /**
     * Record AI response
     */
    public void recordAiResponse(String model, int tokensGenerated, long durationMs, boolean success) {
        recordEvent(TelemetryEventType.AI_RESPONSE, Map.of(
            "model", model,
            "tokensGenerated", tokensGenerated,
            "duration", durationMs,
            "success", success
        ));
    }
    
    /**
     * Record AI error
     */
    public void recordAiError(String model, String errorType, String errorMessage) {
        recordEvent(TelemetryEventType.AI_ERROR, Map.of(
            "model", model,
            "errorType", errorType,
            "errorMessage", sanitizeErrorMessage(errorMessage)
        ));
    }
    
    /**
     * Record authentication event
     */
    public void recordAuthEvent(String eventType, boolean success) {
        recordEvent(TelemetryEventType.AUTH_SUCCESS, Map.of(
            "authEventType", eventType,
            "success", success
        ));
    }
    
    /**
     * Record file operation
     */
    public void recordFileOperation(String operation, String fileType, long fileSize, boolean success) {
        recordEvent(TelemetryEventType.FILE_OPERATION, Map.of(
            "operation", operation,
            "fileType", fileType,
            "fileSize", fileSize,
            "success", success
        ));
    }
    
    /**
     * Record codebase analysis
     */
    public void recordCodebaseAnalysis(int fileCount, int languageCount, long totalLinesOfCode, long durationMs) {
        recordEvent(TelemetryEventType.CODEBASE_ANALYSIS, Map.of(
            "fileCount", fileCount,
            "languageCount", languageCount,
            "totalLinesOfCode", totalLinesOfCode,
            "duration", durationMs
        ));
    }
    
    /**
     * Record configuration change
     */
    public void recordConfigChange(String setting, String oldValue, String newValue) {
        recordEvent(TelemetryEventType.CONFIG_CHANGE, Map.of(
            "setting", setting,
            "oldValue", sanitizeValue(oldValue),
            "newValue", sanitizeValue(newValue)
        ));
    }
      /**
     * Record error occurrence
     */
    public void recordError(String errorType, String errorMessage, String stackTrace) {
        Map<String, Object> params = new HashMap<>();
        params.put("errorType", errorType);
        String sanitizedMessage = sanitizeErrorMessage(errorMessage);
        if (sanitizedMessage != null) {
            params.put("errorMessage", sanitizedMessage);
        }
        String sanitizedStackTrace = sanitizeStackTrace(stackTrace);
        if (sanitizedStackTrace != null) {
            params.put("stackTrace", sanitizedStackTrace);
        }
        recordEvent(TelemetryEventType.ERROR_OCCURRED, params);
    }
    
    /**
     * Record performance metric
     */
    public void recordPerformance(String metric, double value, String unit) {
        recordEvent(TelemetryEventType.PERFORMANCE_METRIC, Map.of(
            "metric", metric,
            "value", value,
            "unit", unit
        ));
    }
      /**
     * Record feature usage
     */
    public void recordFeatureUsage(String feature, Map<String, Object> metadata) {
        Map<String, Object> data = new HashMap<>();
        data.put("feature", feature);
        if (metadata != null) {
            data.putAll(metadata);
        }
        recordEvent(TelemetryEventType.FEATURE_USAGE, data);
    }
    
    /**
     * Flush all pending events
     */
    public void flush() {
        if (!enabled.get()) {
            return;
        }
        
        processQueue();
    }
    
    /**
     * Shutdown telemetry service
     */
    public void shutdown() {
        if (!enabled.get()) {
            return;
        }
        
        log.debug("Shutting down telemetry service");
        
        // Record session end
        recordEvent(TelemetryEventType.SESSION_END, Map.of(
            "sessionDuration", Instant.now().toEpochMilli() - 
                ((Instant) sessionContext.get("startTime")).toEpochMilli()
        ));
        
        // Flush remaining events
        flush();
        
        enabled.set(false);
    }
    
    /**
     * Check if telemetry is enabled
     */
    public boolean isEnabled() {
        return enabled.get();
    }
    
    /**
     * Get current queue size
     */
    public int getQueueSize() {
        return eventQueue.size();
    }
    
    /**
     * Get session ID
     */
    public String getSessionId() {
        return sessionId;
    }
    
    // Private helper methods
    
    @Async
    private void processQueue() {
        if (!enabled.get() || eventQueue.isEmpty()) {
            return;
        }
        
        List<TelemetryEvent> events = new ArrayList<>();
        TelemetryEvent event;
        
        // Drain up to maxQueueSize events
        int processed = 0;
        while ((event = eventQueue.poll()) != null && processed < config.getMaxQueueSize()) {
            events.add(event);
            processed++;
        }
        
        if (!events.isEmpty()) {
            try {
                submitEvents(events);
                log.debug("Submitted {} telemetry events", events.size());
            } catch (Exception e) {
                log.warn("Failed to submit telemetry events", e);
                // Re-queue events on failure (up to a limit)
                if (eventQueue.size() < config.getMaxQueueSize() * 2) {
                    events.forEach(eventQueue::offer);
                }
            }
        }
    }
    
    private void submitEvents(List<TelemetryEvent> events) {
        // In a real implementation, this would send events to a telemetry service
        // For now, we'll just log them in debug mode
        if (log.isDebugEnabled()) {
            for (TelemetryEvent event : events) {
                log.debug("Telemetry event: {} at {} with data: {}", 
                         event.getType(), event.getTimestamp(), event.getData());
            }
        }
        
        // TODO: Implement actual telemetry submission
        // This could use HTTP client to send to analytics service
        // or write to local files for later processing
    }
    
    private String sanitizeErrorMessage(String message) {
        if (message == null) {
            return null;
        }
        
        // Remove potentially sensitive information
        String sanitized = message;
        
        // Remove file paths
        sanitized = sanitized.replaceAll("[A-Za-z]:\\\\[^\\s]+", "[PATH]");
        sanitized = sanitized.replaceAll("/[^\\s]+", "[PATH]");
        
        // Remove URLs
        sanitized = sanitized.replaceAll("https?://[^\\s]+", "[URL]");
        
        // Remove API keys or tokens
        sanitized = sanitized.replaceAll("(?i)(key|token|password|secret)[=:][^\\s]+", "$1=[REDACTED]");
        
        return sanitized;
    }
    
    private String sanitizeStackTrace(String stackTrace) {
        if (stackTrace == null) {
            return null;
        }
        
        // Keep only the first few lines and remove file paths
        String[] lines = stackTrace.split("\n");
        StringBuilder sanitized = new StringBuilder();
        
        for (int i = 0; i < Math.min(5, lines.length); i++) {
            String line = lines[i];
            // Remove file paths but keep class and method names
            line = line.replaceAll("\\([^)]*\\.java:\\d+\\)", "([FILE])");
            sanitized.append(line).append("\n");
        }
        
        return sanitized.toString();
    }
    
    private String sanitizeValue(String value) {
        if (value == null) {
            return null;
        }
        
        // Don't log potentially sensitive configuration values
        if (value.length() > 50 || value.contains("key") || value.contains("token") || 
            value.contains("password") || value.contains("secret")) {
            return "[REDACTED]";
        }
        
        return value;
    }
    
    private Map<String, Object> getPlatformInfo() {
        Map<String, Object> platform = new HashMap<>();
        platform.put("os", System.getProperty("os.name"));
        platform.put("osVersion", System.getProperty("os.version"));
        platform.put("osArch", System.getProperty("os.arch"));
        platform.put("javaVersion", System.getProperty("java.version"));
        platform.put("javaVendor", System.getProperty("java.vendor"));
        return platform;
    }
    
    private String getApplicationVersion() {
        // Try to get version from package
        Package pkg = getClass().getPackage();
        if (pkg != null && pkg.getImplementationVersion() != null) {
            return pkg.getImplementationVersion();
        }
        return "unknown";
    }
    
    /**
     * Telemetry event data structure
     */
    @Data
    public static class TelemetryEvent {
        private TelemetryEventType type;
        private Instant timestamp;
        private String sessionId;
        private Map<String, Object> data;
    }
    
    /**
     * Telemetry configuration
     */
    @Data
    public static class TelemetryConfig {
        private boolean enabled = false;
        private long submissionInterval = 30 * 60 * 1000; // 30 minutes
        private int maxQueueSize = 100;
        private boolean autoSubmit = true;
        private String endpoint;
        private String apiKey;
    }
}
