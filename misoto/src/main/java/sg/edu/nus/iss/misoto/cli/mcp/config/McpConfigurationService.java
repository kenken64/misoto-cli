package sg.edu.nus.iss.misoto.cli.mcp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service for managing MCP configuration loading and saving
 */
@Service
@Slf4j
public class McpConfigurationService {
    
    private final McpConfigurationLoader configurationLoader;
    private McpConfiguration currentConfiguration;
    private String currentConfigPath;
    
    @Autowired
    public McpConfigurationService(McpConfigurationLoader configurationLoader) {
        this.configurationLoader = configurationLoader;
    }
      /**
     * Load configuration from specified path or use default
     */
    public McpConfiguration loadConfiguration(String configPath) throws IOException {
        // Check for system property first (set by main application for CLI args)
        String systemConfigPath = System.getProperty("mcp.config.file");
        if (systemConfigPath != null && !systemConfigPath.trim().isEmpty()) {
            configPath = systemConfigPath;
        }
        
        if (configPath != null && !configPath.trim().isEmpty()) {
            log.info("Loading MCP configuration from specified path: {}", configPath);
            currentConfiguration = configurationLoader.loadFromFile(configPath);
            currentConfigPath = configPath;
        } else {
            log.info("Loading default MCP configuration");
            currentConfiguration = configurationLoader.loadDefault();
            currentConfigPath = getDefaultConfigPath();
        }
        
        log.info("MCP configuration loaded successfully with {} servers", 
                currentConfiguration.getServers().size());
        
        return currentConfiguration;
    }
    
    /**
     * Get current configuration
     */
    public McpConfiguration getCurrentConfiguration() {
        if (currentConfiguration == null) {
            log.warn("No configuration loaded, loading default");
            try {
                return loadConfiguration(null);
            } catch (IOException e) {
                log.error("Failed to load default configuration", e);
                return configurationLoader.createFallbackConfiguration();
            }
        }
        return currentConfiguration;
    }
    
    /**
     * Save current configuration to file
     */
    public void saveConfiguration() throws IOException {
        if (currentConfiguration == null) {
            throw new IllegalStateException("No configuration loaded to save");
        }
        
        if (currentConfigPath == null) {
            currentConfigPath = getDefaultConfigPath();
        }
        
        configurationLoader.saveToFile(currentConfiguration, currentConfigPath);
    }
    
    /**
     * Save configuration to specific path
     */
    public void saveConfiguration(String configPath) throws IOException {
        if (currentConfiguration == null) {
            throw new IllegalStateException("No configuration loaded to save");
        }
        
        configurationLoader.saveToFile(currentConfiguration, configPath);
        currentConfigPath = configPath;
    }
    
    /**
     * Update server configuration
     */
    public void updateServerConfig(String serverId, McpConfiguration.ServerConfig serverConfig) {
        if (currentConfiguration == null) {
            getCurrentConfiguration();
        }
        
        currentConfiguration.getServers().put(serverId, serverConfig);
        log.info("Updated server configuration for: {}", serverId);
    }
    
    /**
     * Remove server configuration
     */
    public void removeServerConfig(String serverId) {
        if (currentConfiguration == null) {
            getCurrentConfiguration();
        }
        
        currentConfiguration.getServers().remove(serverId);
        log.info("Removed server configuration for: {}", serverId);
    }
    
    /**
     * Load configuration from a specific file without setting it as current
     */
    public McpConfiguration loadConfigurationFromFile(String configPath) throws IOException {
        log.info("Loading MCP configuration from file: {}", configPath);
        return configurationLoader.loadFromFile(configPath);
    }
    
    /**
     * Create default configuration at specified path
     */
    public void createDefaultConfiguration(String configPath) throws IOException {
        if (configPath == null) {
            configPath = getDefaultConfigPath();
        }
        
        log.info("Creating default MCP configuration at: {}", configPath);
        McpConfiguration defaultConfig = configurationLoader.loadDefault();
        configurationLoader.saveToFile(defaultConfig, configPath);
    }
    
    /**
     * Get default config file path
     */
    private String getDefaultConfigPath() {
        String userHome = System.getProperty("user.home");
        Path defaultPath = Paths.get(userHome, ".misoto", "mcp.json");
        
        // Create directory if it doesn't exist
        try {
            Files.createDirectories(defaultPath.getParent());
        } catch (IOException e) {
            log.warn("Could not create config directory, using current directory", e);
            return "mcp.json";
        }
        
        return defaultPath.toString();
    }
    
    /**
     * Check if config file exists at path
     */
    public boolean configExists(String configPath) {
        if (configPath == null) {
            configPath = getDefaultConfigPath();
        }
        return Files.exists(Paths.get(configPath));
    }
    
    /**
     * Create default config file if it doesn't exist
     */
    public void createDefaultConfigIfNotExists(String configPath) throws IOException {
        if (configPath == null) {
            configPath = getDefaultConfigPath();
        }
        
        if (!configExists(configPath)) {
            log.info("Creating default MCP configuration at: {}", configPath);
            McpConfiguration defaultConfig = configurationLoader.loadDefault();
            configurationLoader.saveToFile(defaultConfig, configPath);
        }
    }
}
