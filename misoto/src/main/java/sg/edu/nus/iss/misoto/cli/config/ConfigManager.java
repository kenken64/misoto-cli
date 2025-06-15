package sg.edu.nus.iss.misoto.cli.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.misoto.cli.errors.UserError;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Configuration Manager
 * 
 * Handles loading, validating, and providing access to application configuration.
 * Supports multiple sources like environment variables, config files, and CLI arguments.
 */
@Service
@Slf4j
public class ConfigManager {
    
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    
    private ApplicationConfig config;
    
    /**
     * Configuration file paths to check in order of preference
     */
    private final List<String> CONFIG_PATHS = List.of(
        // Current directory
        System.getProperty("user.dir") + File.separator + ".claude-code.json",
        System.getProperty("user.dir") + File.separator + ".claude-code.yml",
        System.getProperty("user.dir") + File.separator + ".claude-code.yaml",
        
        // User home directory
        System.getProperty("user.home") + File.separator + ".claude-code" + File.separator + "config.json",
        System.getProperty("user.home") + File.separator + ".claude-code.json",
        System.getProperty("user.home") + File.separator + ".claude-code" + File.separator + "config.yml",
        
        // Windows AppData
        Optional.ofNullable(System.getenv("APPDATA"))
            .map(appData -> appData + File.separator + "claude-code" + File.separator + "config.json")
            .orElse(null),
            
        // Linux/macOS config directory
        Optional.ofNullable(System.getenv("XDG_CONFIG_HOME"))
            .map(xdg -> xdg + File.separator + "claude-code" + File.separator + "config.json")
            .orElse(System.getProperty("user.home") + File.separator + ".config" + File.separator + "claude-code" + File.separator + "config.json")
    ).stream().filter(path -> path != null).toList();
      /**
     * Load configuration with CLI options
     */
    public ApplicationConfig loadConfig(Map<String, Object> cliOptions) {
        log.debug("Loading configuration with CLI options: {}", cliOptions);
        
        // Start with default configuration
        ApplicationConfig config = new ApplicationConfig();
          // Load configuration from files
        loadConfigFromFiles(config);
        
        // Load custom config file from CLI if specified
        loadCustomConfigFromCli(config, cliOptions);
        
        // Load configuration from environment variables  
        loadConfigFromEnvironment(config);
        
        // Override with CLI options (flags only, not config file)
        applyCliOptions(config, cliOptions);
        
        // Validate configuration
        validateConfig(config);
        
        this.config = config;
        log.info("Configuration loaded successfully");
        return config;
    }
    
    /**
     * Load configuration from the first available config file
     */
    private void loadConfigFromFiles(ApplicationConfig config) {
        for (String configPath : CONFIG_PATHS) {
            Path path = Paths.get(configPath);
            
            if (Files.exists(path) && Files.isRegularFile(path)) {
                try {
                    log.debug("Loading configuration from: {}", configPath);
                    
                    String content = Files.readString(path);
                    ApplicationConfig fileConfig;
                    
                    if (configPath.endsWith(".yml") || configPath.endsWith(".yaml")) {
                        fileConfig = yamlMapper.readValue(content, ApplicationConfig.class);
                    } else {
                        fileConfig = jsonMapper.readValue(content, ApplicationConfig.class);
                    }
                    
                    // Merge file config into current config
                    mergeConfig(config, fileConfig);
                    log.info("Configuration loaded from: {}", configPath);
                    return; // Use first found config file
                    
                } catch (IOException e) {
                    log.warn("Error loading configuration from: {}", configPath, e);
                }
            }
        }
        
        log.debug("No configuration file found, using defaults");
    }
      /**
     * Load custom config file from CLI if specified
     */
    private void loadCustomConfigFromCli(ApplicationConfig config, Map<String, Object> cliOptions) {
        if (cliOptions == null) return;
        
        String configFile = (String) cliOptions.get("config");
        if (configFile != null && !configFile.isBlank()) {
            loadCustomConfigFile(config, configFile);
        }
    }
    
    /**
     * Load configuration from environment variables
     */private void loadConfigFromEnvironment(ApplicationConfig config) {
        // Initialize dotenv loader
        DotenvLoader.initialize();
        
        // API key from environment (via dotenv or system environment)
        String apiKey = DotenvLoader.getEnv("ANTHROPIC_API_KEY");
        if (apiKey != null && !apiKey.isBlank()) {
            config.setApiKey(apiKey);
            log.debug("ANTHROPIC_API_KEY found and loaded from environment (length: {} characters)", apiKey.length());
        } else {
            log.debug("ANTHROPIC_API_KEY not found in environment variables");
        }        
        // API base URL
        String apiUrl = DotenvLoader.getEnv("CLAUDE_API_URL");
        if (apiUrl != null && !apiUrl.isBlank()) {
            config.setApiBaseUrl(apiUrl);
        }
        
        // Log level
        String logLevel = DotenvLoader.getEnv("CLAUDE_LOG_LEVEL");
        if (logLevel != null && !logLevel.isBlank()) {
            try {
                config.setLogLevel(LogLevel.valueOf(logLevel.toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid log level in environment: {}", logLevel);
            }
        }
        
        // Telemetry opt-out
        String telemetryEnabled = DotenvLoader.getEnv("CLAUDE_TELEMETRY");
        if ("0".equals(telemetryEnabled) || "false".equalsIgnoreCase(telemetryEnabled)) {
            config.setTelemetryEnabled(false);
        }
          // AI model override
        String model = DotenvLoader.getEnv("CLAUDE_MODEL");
        if (model != null && !model.isBlank()) {
            config.setAiModel(model);
        }
    }
      /**
     * Apply CLI options to configuration (flags only, not config file)
     */
    private void applyCliOptions(ApplicationConfig config, Map<String, Object> cliOptions) {
        if (cliOptions == null) return;
        
        // Verbose/debug logging
        if (Boolean.TRUE.equals(cliOptions.get("verbose")) || Boolean.TRUE.equals(cliOptions.get("debug"))) {
            config.setLogLevel(LogLevel.DEBUG);
        }
        
        // Quiet logging
        if (Boolean.TRUE.equals(cliOptions.get("quiet"))) {
            config.setLogLevel(LogLevel.ERROR);
        }
    }
    
    /**
     * Load a custom configuration file
     */
    private void loadCustomConfigFile(ApplicationConfig config, String configFile) {
        Path path = Paths.get(configFile);
        
        if (!Files.exists(path)) {
            throw new UserError("Configuration file not found: " + configFile);
        }
        
        try {
            String content = Files.readString(path);
            ApplicationConfig customConfig;
            
            if (configFile.endsWith(".yml") || configFile.endsWith(".yaml")) {
                customConfig = yamlMapper.readValue(content, ApplicationConfig.class);
            } else {
                customConfig = jsonMapper.readValue(content, ApplicationConfig.class);
            }
            
            mergeConfig(config, customConfig);
            log.info("Custom configuration loaded from: {}", configFile);
            
        } catch (IOException e) {
            throw new UserError("Failed to load configuration from: " + configFile + " - " + e.getMessage());
        }
    }
    
    /**
     * Merge a config object into the target config
     */
    private void mergeConfig(ApplicationConfig target, ApplicationConfig source) {
        if (source.getApiKey() != null) target.setApiKey(source.getApiKey());
        if (source.getApiBaseUrl() != null) target.setApiBaseUrl(source.getApiBaseUrl());
        if (source.getApiVersion() != null) target.setApiVersion(source.getApiVersion());
        if (source.getApiTimeout() != null) target.setApiTimeout(source.getApiTimeout());
        
        if (source.getAiModel() != null) target.setAiModel(source.getAiModel());
        if (source.getAiTemperature() != null) target.setAiTemperature(source.getAiTemperature());
        if (source.getAiMaxTokens() != null) target.setAiMaxTokens(source.getAiMaxTokens());
        if (source.getAiMaxHistoryLength() != null) target.setAiMaxHistoryLength(source.getAiMaxHistoryLength());
        
        if (source.getLogLevel() != null) target.setLogLevel(source.getLogLevel());
        if (source.getLogTimestamps() != null) target.setLogTimestamps(source.getLogTimestamps());
        if (source.getLogColors() != null) target.setLogColors(source.getLogColors());
        
        if (source.getTerminalTheme() != null) target.setTerminalTheme(source.getTerminalTheme());
        if (source.getTerminalUseColors() != null) target.setTerminalUseColors(source.getTerminalUseColors());
        if (source.getTerminalShowProgress() != null) target.setTerminalShowProgress(source.getTerminalShowProgress());
        if (source.getTerminalCodeHighlighting() != null) target.setTerminalCodeHighlighting(source.getTerminalCodeHighlighting());
        
        if (source.getTelemetryEnabled() != null) target.setTelemetryEnabled(source.getTelemetryEnabled());
        if (source.getTelemetryAnonymize() != null) target.setTelemetryAnonymize(source.getTelemetryAnonymize());
        
        if (source.getFileOpsMaxReadSize() != null) target.setFileOpsMaxReadSize(source.getFileOpsMaxReadSize());
    }
    
    /**
     * Validate the configuration
     */
    private void validateConfig(ApplicationConfig config) {
        // Validate API base URL
        if (config.getApiBaseUrl() == null || config.getApiBaseUrl().isBlank()) {
            throw new UserError("API base URL is not configured");
        }
        
        // Validate AI model
        if (config.getAiModel() == null || config.getAiModel().isBlank()) {
            throw new UserError("AI model is not configured");
        }
        
        // Warn if no API key is configured
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            log.warn("No API key configured. You may need to authenticate using the 'login' command.");
        }
        
        log.debug("Configuration validation passed");
    }
    
    /**
     * Get the current configuration
     */
    public ApplicationConfig getConfig() {
        if (config == null) {
            throw new IllegalStateException("Configuration not loaded. Call loadConfig() first.");
        }
        return config;
    }
    
    /**
     * Check if configuration is loaded
     */
    public boolean isConfigLoaded() {
        return config != null;
    }
}
