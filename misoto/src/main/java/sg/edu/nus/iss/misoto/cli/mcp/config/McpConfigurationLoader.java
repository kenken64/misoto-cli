package sg.edu.nus.iss.misoto.cli.mcp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration loader for MCP settings from JSON files
 */
@Component
@Slf4j
public class McpConfigurationLoader {
    
    private final ObjectMapper objectMapper;
    
    public McpConfigurationLoader() {
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Load MCP configuration from a JSON file path
     */
    public McpConfiguration loadFromFile(String filePath) throws IOException {
        log.info("Loading MCP configuration from file: {}", filePath);
        
        File configFile = new File(filePath);
        if (!configFile.exists()) {
            throw new IOException("MCP configuration file not found: " + filePath);
        }
        
        return objectMapper.readValue(configFile, McpConfiguration.class);
    }
    
    /**
     * Load MCP configuration from classpath resource
     */
    public McpConfiguration loadFromClasspath(String resourcePath) throws IOException {
        log.info("Loading MCP configuration from classpath: {}", resourcePath);
        
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("MCP configuration resource not found: " + resourcePath);
            }
            
            return objectMapper.readValue(inputStream, McpConfiguration.class);
        }
    }
    
    /**
     * Load default MCP configuration
     */
    public McpConfiguration loadDefault() {
        try {
            return loadFromClasspath("mcp.json");
        } catch (IOException e) {
            log.warn("Could not load default MCP configuration, using fallback", e);
            return createFallbackConfiguration();
        }
    }
    
    /**
     * Save MCP configuration to a JSON file
     */
    public void saveToFile(McpConfiguration configuration, String filePath) throws IOException {
        log.info("Saving MCP configuration to file: {}", filePath);
        
        File configFile = new File(filePath);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(configFile, configuration);
        
        log.info("MCP configuration saved successfully");
    }
      /**
     * Create a fallback configuration if default loading fails
     */
    public McpConfiguration createFallbackConfiguration() {
        log.info("Creating fallback MCP configuration");
        
        McpConfiguration config = new McpConfiguration();
        
        // Set client configuration
        McpConfiguration.ClientConfig clientConfig = McpConfiguration.ClientConfig.builder()
            .name("misoto-cli")
            .version("1.0.0")
            .connectTimeout(30)
            .readTimeout(60)
            .writeTimeout(30)
            .build();
        config.setClient(clientConfig);
        
        // Set default server configuration
        Map<String, McpConfiguration.ServerConfig> servers = new HashMap<>();
        
        McpConfiguration.ServerConfig defaultServer = McpConfiguration.ServerConfig.builder()
            .url("http://localhost:8080")
            .name("Local MCP Server")
            .description("Local development MCP server")
            .enabled(true)
            .priority(0)
            .headers(new HashMap<>())
            .build();
        
        servers.put("default", defaultServer);
        config.setServers(servers);
        
        return config;
    }
}
