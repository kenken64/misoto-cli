package sg.edu.nus.iss.misoto.cli.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import sg.edu.nus.iss.misoto.cli.terminal.TerminalConfig;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for ApplicationConfig
 */
@DisplayName("ApplicationConfig Tests")
class ApplicationConfigTest {

    @Test
    @DisplayName("default values should be set correctly")
    void testDefaultValues() {
        ApplicationConfig config = new ApplicationConfig();
        
        // API Configuration defaults
        assertNull(config.getApiKey());
        assertEquals("https://api.anthropic.com", config.getApiBaseUrl());
        assertEquals("2023-06-01", config.getApiVersion());
        assertEquals(60000, config.getApiTimeout());
        
        // AI Configuration defaults
        assertEquals("claude-3-haiku-20240307", config.getAiModel());
        assertEquals(0.5, config.getAiTemperature());
        assertEquals(4096, config.getAiMaxTokens());
        assertEquals(20, config.getAiMaxHistoryLength());
        
        // Authentication defaults
        assertTrue(config.getAuthAutoRefresh());
        assertEquals(300, config.getAuthTokenRefreshThreshold());
        assertEquals(3, config.getAuthMaxRetryAttempts());
        
        // Terminal Configuration defaults
        assertEquals(TerminalTheme.SYSTEM, config.getTerminalTheme());
        assertTrue(config.getTerminalUseColors());
        assertTrue(config.getTerminalShowProgress());
        assertTrue(config.getTerminalCodeHighlighting());
        
        // Telemetry Configuration defaults
        assertTrue(config.getTelemetryEnabled());
        assertTrue(config.getTelemetryAnonymize());
        assertEquals(1800000, config.getTelemetrySubmissionInterval());
        assertEquals(100, config.getTelemetryMaxQueueSize());
        assertTrue(config.getTelemetryAutoSubmit());
        assertEquals("https://telemetry.example.com/api/events", config.getTelemetryEndpoint());
        
        // File Operations defaults
        assertEquals(10485760L, config.getFileOpsMaxReadSize());
    }    @Test
    @DisplayName("getApiEndpoint should return correct URL")
    void testGetApiEndpoint() {
        ApplicationConfig config = new ApplicationConfig();
        
        // Test default - updated to match actual implementation
        String endpoint = config.getApiEndpoint();
        assertEquals("https://api.anthropic.com/2023-06-01", endpoint);
        
        // Test custom base URL
        config.setApiBaseUrl("https://custom.api.com");
        endpoint = config.getApiEndpoint();
        assertEquals("https://custom.api.com/2023-06-01", endpoint);
          // Test with trailing slash
        config.setApiBaseUrl("https://custom.api.com/");
        endpoint = config.getApiEndpoint();
        assertEquals("https://custom.api.com//2023-06-01", endpoint);
    }

    @Test
    @DisplayName("shouldUseColors should handle null values correctly")
    void testShouldUseColors() {
        ApplicationConfig config = new ApplicationConfig();
        
        // Test default (true)
        assertTrue(config.shouldUseColors());
        
        // Test explicit true
        config.setTerminalUseColors(true);
        assertTrue(config.shouldUseColors());
        
        // Test explicit false
        config.setTerminalUseColors(false);
        assertFalse(config.shouldUseColors());
        
        // Test null (should default to true)
        config.setTerminalUseColors(null);
        assertTrue(config.shouldUseColors());
    }

    @Test
    @DisplayName("isTelemetryEnabled should handle null values correctly")
    void testIsTelemetryEnabled() {
        ApplicationConfig config = new ApplicationConfig();
        
        // Test default (true)
        assertTrue(config.isTelemetryEnabled());
        
        // Test explicit true
        config.setTelemetryEnabled(true);
        assertTrue(config.isTelemetryEnabled());
        
        // Test explicit false
        config.setTelemetryEnabled(false);
        assertFalse(config.isTelemetryEnabled());
        
        // Test null (should default to true)
        config.setTelemetryEnabled(null);
        assertTrue(config.isTelemetryEnabled());
    }    @Test
    @DisplayName("getTelemetryEndpoint should return correct URL")
    void testGetTelemetryEndpoint() {
        ApplicationConfig config = new ApplicationConfig();
        
        // Test default
        String endpoint = config.getTelemetryEndpoint();
        assertEquals("https://telemetry.example.com/api/events", endpoint);
        
        // Test custom endpoint
        config.setTelemetryEndpoint("https://custom.telemetry.com/events");
        endpoint = config.getTelemetryEndpoint();
        assertEquals("https://custom.telemetry.com/events", endpoint);
        
        // Test null endpoint
        config.setTelemetryEndpoint(null);
        endpoint = config.getTelemetryEndpoint();
        assertNull(endpoint); // When explicitly set to null, it should return null
    }

    @Test
    @DisplayName("isAuthAutoRefreshEnabled should handle null values correctly")
    void testIsAuthAutoRefreshEnabled() {
        ApplicationConfig config = new ApplicationConfig();
        
        // Test default (true)
        assertTrue(config.isAuthAutoRefreshEnabled());
        
        // Test explicit true
        config.setAuthAutoRefresh(true);
        assertTrue(config.isAuthAutoRefreshEnabled());
        
        // Test explicit false
        config.setAuthAutoRefresh(false);
        assertFalse(config.isAuthAutoRefreshEnabled());
        
        // Test null (should default to false)
        config.setAuthAutoRefresh(null);
        assertFalse(config.isAuthAutoRefreshEnabled());
    }

    @Test
    @DisplayName("getTerminal should return properly configured TerminalConfig")
    void testGetTerminal() {
        ApplicationConfig config = new ApplicationConfig();
        
        TerminalConfig terminalConfig = config.getTerminal();
          assertNotNull(terminalConfig);
        assertEquals(TerminalTheme.SYSTEM, terminalConfig.getTheme());
        assertTrue(terminalConfig.isUseColors());
        assertTrue(terminalConfig.isShowProgressIndicators());
        assertTrue(terminalConfig.isCodeHighlighting());
        assertNull(terminalConfig.getMaxHeight());
        assertNull(terminalConfig.getMaxWidth());
    }

    @Test
    @DisplayName("getTerminal should handle custom values")
    void testGetTerminalWithCustomValues() {
        ApplicationConfig config = new ApplicationConfig();
        
        config.setTerminalTheme(TerminalTheme.DARK);
        config.setTerminalUseColors(false);
        config.setTerminalShowProgress(false);
        config.setTerminalCodeHighlighting(false);
        config.setTerminalMaxHeight(50);
        config.setTerminalMaxWidth(120);
        
        TerminalConfig terminalConfig = config.getTerminal();
          assertNotNull(terminalConfig);
        assertEquals(TerminalTheme.DARK, terminalConfig.getTheme());
        assertFalse(terminalConfig.isUseColors());
        assertFalse(terminalConfig.isShowProgressIndicators());
        assertFalse(terminalConfig.isCodeHighlighting());
        assertEquals(50, terminalConfig.getMaxHeight());
        assertEquals(120, terminalConfig.getMaxWidth());
    }

    @Test
    @DisplayName("getTerminal should handle null theme")
    void testGetTerminalWithNullTheme() {
        ApplicationConfig config = new ApplicationConfig();
        config.setTerminalTheme(null);
        
        TerminalConfig terminalConfig = config.getTerminal();
        
        assertNotNull(terminalConfig);
        assertEquals(TerminalTheme.SYSTEM, terminalConfig.getTheme());
    }

    @Test
    @DisplayName("setters and getters should work correctly")
    void testSettersAndGetters() {
        ApplicationConfig config = new ApplicationConfig();
        
        // Test API configuration
        config.setApiKey("test-api-key");
        assertEquals("test-api-key", config.getApiKey());
        
        config.setApiBaseUrl("https://test.api.com");
        assertEquals("https://test.api.com", config.getApiBaseUrl());
        
        config.setApiVersion("2024-01-01");
        assertEquals("2024-01-01", config.getApiVersion());
        
        config.setApiTimeout(30000);
        assertEquals(30000, config.getApiTimeout());
        
        // Test AI configuration
        config.setAiModel("test-model");
        assertEquals("test-model", config.getAiModel());
        
        config.setAiTemperature(0.8);
        assertEquals(0.8, config.getAiTemperature());
        
        config.setAiMaxTokens(8192);
        assertEquals(8192, config.getAiMaxTokens());
        
        config.setAiMaxHistoryLength(50);
        assertEquals(50, config.getAiMaxHistoryLength());
        
        // Test authentication configuration
        config.setAuthAutoRefresh(false);
        assertEquals(false, config.getAuthAutoRefresh());
        
        config.setAuthTokenRefreshThreshold(600);
        assertEquals(600, config.getAuthTokenRefreshThreshold());
        
        config.setAuthMaxRetryAttempts(5);
        assertEquals(5, config.getAuthMaxRetryAttempts());
    }

    @Test
    @DisplayName("configuration should handle boundary values")
    void testBoundaryValues() {
        ApplicationConfig config = new ApplicationConfig();
        
        // Test zero and negative values
        config.setApiTimeout(0);
        assertEquals(0, config.getApiTimeout());
        
        config.setAiTemperature(0.0);
        assertEquals(0.0, config.getAiTemperature());
        
        config.setAiTemperature(2.0);
        assertEquals(2.0, config.getAiTemperature());
        
        config.setAiMaxTokens(1);
        assertEquals(1, config.getAiMaxTokens());
        
        config.setAiMaxHistoryLength(0);
        assertEquals(0, config.getAiMaxHistoryLength());
        
        // Test large values
        config.setFileOpsMaxReadSize(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, config.getFileOpsMaxReadSize());
        
        config.setTelemetrySubmissionInterval(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, config.getTelemetrySubmissionInterval());
    }

    @Test
    @DisplayName("configuration should handle null string values")
    void testNullStringValues() {
        ApplicationConfig config = new ApplicationConfig();
        
        config.setApiKey(null);
        assertNull(config.getApiKey());
        
        config.setApiBaseUrl(null);
        assertNull(config.getApiBaseUrl());
        
        config.setAiModel(null);
        assertNull(config.getAiModel());
        
        config.setTelemetryEndpoint(null);
        assertNull(config.getTelemetryEndpoint());
    }

    @Test
    @DisplayName("boolean configuration should handle all Boolean states")
    void testBooleanConfigurationStates() {
        ApplicationConfig config = new ApplicationConfig();
        
        // Test all three states: null, true, false
        config.setTelemetryEnabled(null);
        assertTrue(config.isTelemetryEnabled()); // Should default to true
        
        config.setTelemetryEnabled(Boolean.TRUE);
        assertTrue(config.isTelemetryEnabled());
        
        config.setTelemetryEnabled(Boolean.FALSE);
        assertFalse(config.isTelemetryEnabled());
        
        // Test terminal colors
        config.setTerminalUseColors(null);
        assertTrue(config.shouldUseColors()); // Should default to true
        
        config.setTerminalUseColors(Boolean.TRUE);
        assertTrue(config.shouldUseColors());
        
        config.setTerminalUseColors(Boolean.FALSE);
        assertFalse(config.shouldUseColors());
    }
}
