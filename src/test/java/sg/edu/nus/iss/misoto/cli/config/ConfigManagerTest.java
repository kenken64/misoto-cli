package sg.edu.nus.iss.misoto.cli.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test cases for ConfigManager
 */
@DisplayName("ConfigManager Tests")
class ConfigManagerTest {

    private ConfigManager configManager;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        configManager = new ConfigManager();
    }

    @Test
    @DisplayName("loadConfig should create default configuration when no files exist")
    void testLoadConfigDefault() {
        Map<String, Object> cliOptions = new HashMap<>();
        
        ApplicationConfig config = configManager.loadConfig(cliOptions);
        
        assertNotNull(config);
        assertEquals("https://api.anthropic.com", config.getApiBaseUrl());
        assertEquals("claude-3-haiku-20240307", config.getAiModel());
        assertEquals(0.5, config.getAiTemperature());
        assertTrue(config.isTelemetryEnabled());
    }

    @Test
    @DisplayName("loadConfig should apply CLI options")
    void testLoadConfigWithCliOptions() {
        Map<String, Object> cliOptions = new HashMap<>();
        cliOptions.put("verbose", true);
        cliOptions.put("quiet", false);
        
        ApplicationConfig config = configManager.loadConfig(cliOptions);
        
        assertNotNull(config);
        assertEquals(LogLevel.DEBUG, config.getLogLevel());
    }

    @Test
    @DisplayName("loadConfig should apply quiet CLI option")
    void testLoadConfigWithQuietOption() {
        Map<String, Object> cliOptions = new HashMap<>();
        cliOptions.put("quiet", true);
        
        ApplicationConfig config = configManager.loadConfig(cliOptions);
        
        assertNotNull(config);
        assertEquals(LogLevel.ERROR, config.getLogLevel());
    }

    @Test
    @DisplayName("loadConfig should load custom config file")
    void testLoadConfigWithCustomFile() throws IOException {
        // Create a temporary config file
        Path configFile = tempDir.resolve("custom-config.json");
        String configContent = """
            {
                "api_base_url": "https://custom.api.com",
                "ai_model": "custom-model",
                "ai_temperature": 0.8
            }
            """;
        Files.writeString(configFile, configContent);
        
        Map<String, Object> cliOptions = new HashMap<>();
        cliOptions.put("config", configFile.toString());
        
        ApplicationConfig config = configManager.loadConfig(cliOptions);
        
        assertNotNull(config);
        assertEquals("https://custom.api.com", config.getApiBaseUrl());
        assertEquals("custom-model", config.getAiModel());
        assertEquals(0.8, config.getAiTemperature());
    }

    @Test
    @DisplayName("loadConfig should handle invalid custom config file")
    void testLoadConfigWithInvalidCustomFile() {
        Map<String, Object> cliOptions = new HashMap<>();
        cliOptions.put("config", "/nonexistent/config.json");
        
        assertThrows(Exception.class, () -> configManager.loadConfig(cliOptions));
    }

    @Test
    @DisplayName("loadConfig should load environment variables")
    void testLoadConfigWithEnvironmentVariables() {
        try (MockedStatic<DotenvLoader> mockedDotenv = Mockito.mockStatic(DotenvLoader.class)) {
            mockedDotenv.when(DotenvLoader::initialize).thenAnswer(invocation -> null);
            mockedDotenv.when(() -> DotenvLoader.getEnv("ANTHROPIC_API_KEY"))
                .thenReturn("test-api-key");
            mockedDotenv.when(() -> DotenvLoader.getEnv("CLAUDE_API_URL"))
                .thenReturn("https://env.api.com");
            mockedDotenv.when(() -> DotenvLoader.getEnv("CLAUDE_LOG_LEVEL"))
                .thenReturn("INFO");
            mockedDotenv.when(() -> DotenvLoader.getEnv("CLAUDE_TELEMETRY"))
                .thenReturn("false");
            mockedDotenv.when(() -> DotenvLoader.getEnv("CLAUDE_MODEL"))
                .thenReturn("env-model");
            
            Map<String, Object> cliOptions = new HashMap<>();
            ApplicationConfig config = configManager.loadConfig(cliOptions);
            
            assertNotNull(config);
            assertEquals("test-api-key", config.getApiKey());
            assertEquals("https://env.api.com", config.getApiBaseUrl());
            assertEquals(LogLevel.INFO, config.getLogLevel());
            assertFalse(config.isTelemetryEnabled());
            assertEquals("env-model", config.getAiModel());
        }
    }

    @Test
    @DisplayName("loadConfig should handle invalid log level from environment")
    void testLoadConfigWithInvalidLogLevel() {
        try (MockedStatic<DotenvLoader> mockedDotenv = Mockito.mockStatic(DotenvLoader.class)) {
            mockedDotenv.when(DotenvLoader::initialize).thenAnswer(invocation -> null);
            mockedDotenv.when(() -> DotenvLoader.getEnv("CLAUDE_LOG_LEVEL"))
                .thenReturn("INVALID_LEVEL");
            
            Map<String, Object> cliOptions = new HashMap<>();
            
            // Should not throw exception, just warn and continue
            assertDoesNotThrow(() -> configManager.loadConfig(cliOptions));
        }
    }

    @Test
    @DisplayName("getConfig should return loaded configuration")
    void testGetConfig() {
        Map<String, Object> cliOptions = new HashMap<>();
        ApplicationConfig config = configManager.loadConfig(cliOptions);
        
        ApplicationConfig retrievedConfig = configManager.getConfig();
        
        assertSame(config, retrievedConfig);
    }

    @Test
    @DisplayName("getConfig should throw when configuration not loaded")
    void testGetConfigNotLoaded() {
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> configManager.getConfig()
        );
        assertEquals("Configuration not loaded. Call loadConfig() first.", exception.getMessage());
    }

    @Test
    @DisplayName("isConfigLoaded should return correct state")
    void testIsConfigLoaded() {
        assertFalse(configManager.isConfigLoaded());
        
        Map<String, Object> cliOptions = new HashMap<>();
        configManager.loadConfig(cliOptions);
        
        assertTrue(configManager.isConfigLoaded());
    }

    @Test
    @DisplayName("loadConfig should validate configuration")
    void testConfigValidation() {
        // Test that validation passes with valid config
        Map<String, Object> cliOptions = new HashMap<>();
        assertDoesNotThrow(() -> configManager.loadConfig(cliOptions));
    }

    @Test
    @DisplayName("loadConfig should handle YAML configuration files")
    void testLoadConfigWithYamlFile() throws IOException {
        // Create a temporary YAML config file
        Path configFile = tempDir.resolve("config.yml");
        String yamlContent = """
            api_base_url: "https://yaml.api.com"
            ai_model: "yaml-model"
            ai_temperature: 0.9
            """;
        Files.writeString(configFile, yamlContent);
        
        Map<String, Object> cliOptions = new HashMap<>();
        cliOptions.put("config", configFile.toString());
        
        ApplicationConfig config = configManager.loadConfig(cliOptions);
        
        assertNotNull(config);
        assertEquals("https://yaml.api.com", config.getApiBaseUrl());
        assertEquals("yaml-model", config.getAiModel());
        assertEquals(0.9, config.getAiTemperature());
    }

    @Test
    @DisplayName("loadConfig should handle malformed JSON configuration")
    void testLoadConfigWithMalformedJson() throws IOException {
        Path configFile = tempDir.resolve("bad-config.json");
        String badJson = """
            {
                "api_base_url": "https://api.com"
                "missing_comma": true
            }
            """;
        Files.writeString(configFile, badJson);
        
        Map<String, Object> cliOptions = new HashMap<>();
        cliOptions.put("config", configFile.toString());
        
        assertThrows(Exception.class, () -> configManager.loadConfig(cliOptions));
    }

    @Test
    @DisplayName("loadConfig should merge configurations correctly")
    void testConfigMerging() throws IOException {
        // Create config file with some values
        Path configFile = tempDir.resolve("base-config.json");
        String configContent = """
            {
                "api_base_url": "https://file.api.com",
                "ai_model": "file-model"
            }
            """;
        Files.writeString(configFile, configContent);
        
        try (MockedStatic<DotenvLoader> mockedDotenv = Mockito.mockStatic(DotenvLoader.class)) {
            mockedDotenv.when(DotenvLoader::initialize).thenAnswer(invocation -> null);
            // Environment should override file
            mockedDotenv.when(() -> DotenvLoader.getEnv("ANTHROPIC_API_KEY"))
                .thenReturn("env-api-key");
            mockedDotenv.when(() -> DotenvLoader.getEnv("CLAUDE_MODEL"))
                .thenReturn("env-model");
            
            Map<String, Object> cliOptions = new HashMap<>();
            cliOptions.put("config", configFile.toString());
            // CLI should override everything
            cliOptions.put("verbose", true);
            
            ApplicationConfig config = configManager.loadConfig(cliOptions);
            
            // File config
            assertEquals("https://file.api.com", config.getApiBaseUrl());
            // Environment override
            assertEquals("env-api-key", config.getApiKey());
            assertEquals("env-model", config.getAiModel());
            // CLI override
            assertEquals(LogLevel.DEBUG, config.getLogLevel());
        }
    }

    @Test
    @DisplayName("loadConfig should handle telemetry environment variables correctly")
    void testTelemetryEnvironmentVariables() {
        try (MockedStatic<DotenvLoader> mockedDotenv = Mockito.mockStatic(DotenvLoader.class)) {
            mockedDotenv.when(DotenvLoader::initialize).thenAnswer(invocation -> null);
            
            // Test "0" value
            mockedDotenv.when(() -> DotenvLoader.getEnv("CLAUDE_TELEMETRY"))
                .thenReturn("0");
            
            Map<String, Object> cliOptions = new HashMap<>();
            ApplicationConfig config = configManager.loadConfig(cliOptions);
            
            assertFalse(config.isTelemetryEnabled());
        }
        
        try (MockedStatic<DotenvLoader> mockedDotenv = Mockito.mockStatic(DotenvLoader.class)) {
            mockedDotenv.when(DotenvLoader::initialize).thenAnswer(invocation -> null);
            
            // Test "false" value (case insensitive)
            mockedDotenv.when(() -> DotenvLoader.getEnv("CLAUDE_TELEMETRY"))
                .thenReturn("FALSE");
            
            Map<String, Object> cliOptions = new HashMap<>();
            ApplicationConfig config = configManager.loadConfig(cliOptions);
            
            assertFalse(config.isTelemetryEnabled());
        }
    }
}
