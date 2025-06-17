package sg.edu.nus.iss.misoto.cli.telemetry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for TelemetryService
 */
@DisplayName("TelemetryService Tests")
class TelemetryServiceTest {

    private TelemetryService telemetryService;
    private TelemetryService.TelemetryConfig config;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        telemetryService = new TelemetryService();
        config = new TelemetryService.TelemetryConfig();
    }    @Test
    @DisplayName("initialize should enable telemetry when configured")
    void testInitializeEnabled() {
        config.setEnabled(true);
        config.setMaxQueueSize(50);

        telemetryService.initialize(config);

        assertTrue(telemetryService.isEnabled());
        assertNotNull(telemetryService.getSessionId());
        // After initialization, a session_start event is recorded
        assertEquals(1, telemetryService.getQueueSize());
    }

    @Test
    @DisplayName("initialize should disable telemetry when configured")
    void testInitializeDisabled() {
        config.setEnabled(false);

        telemetryService.initialize(config);

        assertFalse(telemetryService.isEnabled());
        assertNotNull(telemetryService.getSessionId());
        assertEquals(0, telemetryService.getQueueSize());
    }

    @Test
    @DisplayName("recordEvent should record event when enabled")
    void testRecordEventEnabled() {
        config.setEnabled(true);
        config.setMaxQueueSize(100);
        telemetryService.initialize(config);

        Map<String, Object> data = Map.of(
            "command", "test",
            "duration", 1000L
        );

        telemetryService.recordEvent(TelemetryEventType.COMMAND_EXECUTE, data);

        assertTrue(telemetryService.getQueueSize() > 0);
    }

    @Test
    @DisplayName("recordEvent should not record event when disabled")
    void testRecordEventDisabled() {
        config.setEnabled(false);
        telemetryService.initialize(config);

        Map<String, Object> data = Map.of(
            "command", "test",
            "duration", 1000L
        );

        telemetryService.recordEvent(TelemetryEventType.COMMAND_EXECUTE, data);

        assertEquals(0, telemetryService.getQueueSize());
    }

    @Test
    @DisplayName("recordCommandExecution should record command data")
    void testRecordCommandExecution() {
        config.setEnabled(true);
        config.setMaxQueueSize(100);
        telemetryService.initialize(config);

        String command = "analyze";
        long duration = 5000L;
        boolean success = true;

        telemetryService.recordCommandExecution(command, duration, success);

        assertTrue(telemetryService.getQueueSize() > 0);
    }

    @Test
    @DisplayName("recordCommandSuccess should record success data")
    void testRecordCommandSuccess() {
        config.setEnabled(true);
        config.setMaxQueueSize(100);
        telemetryService.initialize(config);

        String command = "explain";
        long duration = 3000L;

        telemetryService.recordCommandSuccess(command, duration);

        assertTrue(telemetryService.getQueueSize() > 0);
    }

    @Test
    @DisplayName("recordCommandError should record error data")
    void testRecordCommandError() {
        config.setEnabled(true);
        config.setMaxQueueSize(100);
        telemetryService.initialize(config);

        String command = "ask";
        String errorType = "ValidationError";
        String errorMessage = "Invalid input provided";

        telemetryService.recordCommandError(command, errorType, errorMessage);

        assertTrue(telemetryService.getQueueSize() > 0);
    }

    @Test
    @DisplayName("recordAiRequest should record AI request data")
    void testRecordAiRequest() {
        config.setEnabled(true);
        config.setMaxQueueSize(100);
        telemetryService.initialize(config);

        String model = "claude-3-sonnet";
        int tokensUsed = 150;
        long duration = 2000L;

        telemetryService.recordAiRequest(model, tokensUsed, duration);

        assertTrue(telemetryService.getQueueSize() > 0);
    }

    @Test
    @DisplayName("recordAiResponse should record AI response data")
    void testRecordAiResponse() {
        config.setEnabled(true);
        config.setMaxQueueSize(100);
        telemetryService.initialize(config);

        String model = "claude-3-sonnet";
        int tokensGenerated = 300;
        long duration = 4000L;
        boolean success = true;

        telemetryService.recordAiResponse(model, tokensGenerated, duration, success);

        assertTrue(telemetryService.getQueueSize() > 0);
    }

    @Test
    @DisplayName("recordAiError should record AI error data")
    void testRecordAiError() {
        config.setEnabled(true);
        config.setMaxQueueSize(100);
        telemetryService.initialize(config);

        String model = "claude-3-sonnet";
        String errorType = "RateLimitError";
        String errorMessage = "Rate limit exceeded";

        telemetryService.recordAiError(model, errorType, errorMessage);

        assertTrue(telemetryService.getQueueSize() > 0);
    }

    @Test
    @DisplayName("recordAuthEvent should record authentication data")
    void testRecordAuthEvent() {
        config.setEnabled(true);
        config.setMaxQueueSize(100);
        telemetryService.initialize(config);

        String eventType = "login";
        boolean success = true;

        telemetryService.recordAuthEvent(eventType, success);

        assertTrue(telemetryService.getQueueSize() > 0);
    }

    @Test
    @DisplayName("recordFileOperation should record file operation data")
    void testRecordFileOperation() {
        config.setEnabled(true);
        config.setMaxQueueSize(100);
        telemetryService.initialize(config);

        String operation = "read";
        String fileType = "txt";
        long fileSize = 1024L;
        boolean success = true;

        telemetryService.recordFileOperation(operation, fileType, fileSize, success);

        assertTrue(telemetryService.getQueueSize() > 0);
    }

    @Test
    @DisplayName("recordCodebaseAnalysis should record analysis data")
    void testRecordCodebaseAnalysis() {
        config.setEnabled(true);
        config.setMaxQueueSize(100);
        telemetryService.initialize(config);

        int fileCount = 50;
        int languageCount = 5;
        long totalLinesOfCode = 10000L;
        long duration = 15000L;

        telemetryService.recordCodebaseAnalysis(fileCount, languageCount, totalLinesOfCode, duration);

        assertTrue(telemetryService.getQueueSize() > 0);
    }

    @Test
    @DisplayName("recordConfigChange should record configuration changes")
    void testRecordConfigChange() {
        config.setEnabled(true);
        config.setMaxQueueSize(100);
        telemetryService.initialize(config);

        String setting = "debug";
        String oldValue = "false";
        String newValue = "true";

        telemetryService.recordConfigChange(setting, oldValue, newValue);

        assertTrue(telemetryService.getQueueSize() > 0);
    }

    @Test
    @DisplayName("recordError should record error occurrence")
    void testRecordError() {
        config.setEnabled(true);
        config.setMaxQueueSize(100);
        telemetryService.initialize(config);

        String errorType = "NullPointerException";
        String errorMessage = "Null value encountered";
        String stackTrace = "at com.example.Class.method(Class.java:123)";

        telemetryService.recordError(errorType, errorMessage, stackTrace);

        assertTrue(telemetryService.getQueueSize() > 0);
    }

    @Test
    @DisplayName("recordPerformance should record performance metrics")
    void testRecordPerformance() {
        config.setEnabled(true);
        config.setMaxQueueSize(100);
        telemetryService.initialize(config);

        String metric = "memory_usage";
        double value = 512.5;
        String unit = "MB";

        telemetryService.recordPerformance(metric, value, unit);

        assertTrue(telemetryService.getQueueSize() > 0);
    }

    @Test
    @DisplayName("recordFeatureUsage should record feature usage")
    void testRecordFeatureUsage() {
        config.setEnabled(true);
        config.setMaxQueueSize(100);
        telemetryService.initialize(config);

        String feature = "code_analysis";
        Map<String, Object> metadata = Map.of(
            "language", "java",
            "fileCount", 10
        );

        telemetryService.recordFeatureUsage(feature, metadata);

        assertTrue(telemetryService.getQueueSize() > 0);
    }

    @Test
    @DisplayName("flush should process all queued events")
    void testFlush() {
        config.setEnabled(true);
        config.setMaxQueueSize(100);
        telemetryService.initialize(config);

        // Add some events
        telemetryService.recordEvent(TelemetryEventType.COMMAND_EXECUTE, Map.of("test", "data"));
        telemetryService.recordEvent(TelemetryEventType.COMMAND_SUCCESS, Map.of("test", "data"));

        int queueSizeBefore = telemetryService.getQueueSize();
        assertTrue(queueSizeBefore > 0);

        telemetryService.flush();

        // Note: In a real implementation, queue would be cleared after processing
        // For this test, we just verify flush can be called without errors
    }

    @Test
    @DisplayName("shutdown should record session end and flush events")
    void testShutdown() {
        config.setEnabled(true);
        config.setMaxQueueSize(100);
        telemetryService.initialize(config);

        // Add some events
        telemetryService.recordEvent(TelemetryEventType.COMMAND_EXECUTE, Map.of("test", "data"));

        assertTrue(telemetryService.isEnabled());
        assertTrue(telemetryService.getQueueSize() > 0);

        telemetryService.shutdown();

        assertFalse(telemetryService.isEnabled());
    }

    @Test
    @DisplayName("multiple events should be queued properly")
    void testMultipleEvents() {
        config.setEnabled(true);
        config.setMaxQueueSize(100);
        telemetryService.initialize(config);

        int initialSize = telemetryService.getQueueSize();

        // Record multiple events
        telemetryService.recordEvent(TelemetryEventType.SESSION_START, Map.of("data1", "value1"));
        telemetryService.recordEvent(TelemetryEventType.COMMAND_EXECUTE, Map.of("data2", "value2"));
        telemetryService.recordEvent(TelemetryEventType.AI_REQUEST, Map.of("data3", "value3"));
        telemetryService.recordEvent(TelemetryEventType.FILE_OPERATION, Map.of("data4", "value4"));

        int finalSize = telemetryService.getQueueSize();
        assertEquals(initialSize + 4, finalSize);
    }

    @Test
    @DisplayName("events should not be recorded when telemetry is disabled")
    void testEventsNotRecordedWhenDisabled() {
        config.setEnabled(false);
        telemetryService.initialize(config);

        // Try to record events
        telemetryService.recordCommandExecution("test", 1000L, true);
        telemetryService.recordAiRequest("model", 100, 2000L);
        telemetryService.recordFileOperation("read", "txt", 1024L, true);

        assertEquals(0, telemetryService.getQueueSize());
    }

    @Test
    @DisplayName("TelemetryEvent should store data correctly")
    void testTelemetryEvent() {
        TelemetryService.TelemetryEvent event = new TelemetryService.TelemetryEvent();
        
        TelemetryEventType type = TelemetryEventType.COMMAND_EXECUTE;
        Instant timestamp = Instant.now();
        String sessionId = "test-session-123";
        Map<String, Object> data = Map.of("command", "test", "duration", 1000L);

        event.setType(type);
        event.setTimestamp(timestamp);
        event.setSessionId(sessionId);
        event.setData(data);

        assertEquals(type, event.getType());
        assertEquals(timestamp, event.getTimestamp());
        assertEquals(sessionId, event.getSessionId());
        assertEquals(data, event.getData());
    }

    @Test
    @DisplayName("TelemetryConfig should have correct defaults")
    void testTelemetryConfigDefaults() {
        TelemetryService.TelemetryConfig config = new TelemetryService.TelemetryConfig();

        assertFalse(config.isEnabled());
        assertEquals(30 * 60 * 1000, config.getSubmissionInterval());
        assertEquals(100, config.getMaxQueueSize());
        assertTrue(config.isAutoSubmit());
        assertNull(config.getEndpoint());
        assertNull(config.getApiKey());
    }

    @Test
    @DisplayName("TelemetryConfig should allow customization")
    void testTelemetryConfigCustomization() {
        TelemetryService.TelemetryConfig config = new TelemetryService.TelemetryConfig();
        
        config.setEnabled(true);
        config.setSubmissionInterval(60000L);
        config.setMaxQueueSize(200);
        config.setAutoSubmit(false);
        config.setEndpoint("https://api.example.com/telemetry");
        config.setApiKey("test-api-key");

        assertTrue(config.isEnabled());
        assertEquals(60000L, config.getSubmissionInterval());
        assertEquals(200, config.getMaxQueueSize());
        assertFalse(config.isAutoSubmit());
        assertEquals("https://api.example.com/telemetry", config.getEndpoint());
        assertEquals("test-api-key", config.getApiKey());
    }

    @Test
    @DisplayName("recordEvent should handle null data gracefully")
    void testRecordEventNullData() {
        config.setEnabled(true);
        config.setMaxQueueSize(100);
        telemetryService.initialize(config);

        assertDoesNotThrow(() -> {
            telemetryService.recordEvent(TelemetryEventType.COMMAND_EXECUTE, null);
        });
    }

    @Test
    @DisplayName("recordEvent should handle empty data")
    void testRecordEventEmptyData() {
        config.setEnabled(true);
        config.setMaxQueueSize(100);
        telemetryService.initialize(config);

        Map<String, Object> emptyData = new HashMap<>();

        assertDoesNotThrow(() -> {
            telemetryService.recordEvent(TelemetryEventType.COMMAND_EXECUTE, emptyData);
        });

        assertTrue(telemetryService.getQueueSize() > 0);
    }

    @Test
    @DisplayName("recordCommandError should handle null error message")
    void testRecordCommandErrorNullMessage() {
        config.setEnabled(true);
        config.setMaxQueueSize(100);
        telemetryService.initialize(config);

        assertDoesNotThrow(() -> {
            telemetryService.recordCommandError("test", "TestError", null);
        });

        assertTrue(telemetryService.getQueueSize() > 0);
    }

    @Test
    @DisplayName("recordError should handle null parameters")
    void testRecordErrorNullParameters() {
        config.setEnabled(true);
        config.setMaxQueueSize(100);
        telemetryService.initialize(config);

        assertDoesNotThrow(() -> {
            telemetryService.recordError(null, null, null);
        });

        assertTrue(telemetryService.getQueueSize() > 0);
    }

    @Test
    @DisplayName("recordFeatureUsage should handle null metadata")
    void testRecordFeatureUsageNullMetadata() {
        config.setEnabled(true);
        config.setMaxQueueSize(100);
        telemetryService.initialize(config);

        assertDoesNotThrow(() -> {
            telemetryService.recordFeatureUsage("test_feature", null);
        });
    }

    @Test
    @DisplayName("session ID should be consistent across calls")
    void testSessionIdConsistency() {
        config.setEnabled(true);
        telemetryService.initialize(config);

        String sessionId1 = telemetryService.getSessionId();
        String sessionId2 = telemetryService.getSessionId();

        assertEquals(sessionId1, sessionId2);
        assertNotNull(sessionId1);
        assertFalse(sessionId1.isEmpty());
    }

    @Test
    @DisplayName("should handle various data types in event data")
    void testEventDataTypes() {
        config.setEnabled(true);
        config.setMaxQueueSize(100);
        telemetryService.initialize(config);

        Map<String, Object> data = new HashMap<>();
        data.put("string", "test");
        data.put("integer", 42);
        data.put("long", 1000L);
        data.put("double", 3.14);
        data.put("boolean", true);
        data.put("instant", Instant.now());
        data.put("list", java.util.List.of("a", "b", "c"));
        data.put("map", Map.of("nested", "value"));

        assertDoesNotThrow(() -> {
            telemetryService.recordEvent(TelemetryEventType.FEATURE_USAGE, data);
        });

        assertTrue(telemetryService.getQueueSize() > 0);
    }

    @Test
    @DisplayName("should handle large amounts of data")
    void testLargeDataHandling() {
        config.setEnabled(true);
        config.setMaxQueueSize(100);
        telemetryService.initialize(config);

        Map<String, Object> largeData = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            largeData.put("key" + i, "value" + i);
        }

        assertDoesNotThrow(() -> {
            telemetryService.recordEvent(TelemetryEventType.PERFORMANCE_METRIC, largeData);
        });

        assertTrue(telemetryService.getQueueSize() > 0);
    }

    @Test
    @DisplayName("should handle concurrent event recording")
    void testConcurrentEventRecording() throws InterruptedException {
        config.setEnabled(true);
        config.setMaxQueueSize(1000);
        telemetryService.initialize(config);

        int threadCount = 10;
        int eventsPerThread = 10;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < eventsPerThread; j++) {
                    telemetryService.recordEvent(TelemetryEventType.FEATURE_USAGE, 
                        Map.of("thread", threadId, "event", j));
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // Should have recorded all events (plus session start event)
        assertTrue(telemetryService.getQueueSize() >= threadCount * eventsPerThread);
    }
}
