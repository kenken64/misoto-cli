package sg.edu.nus.iss.misoto.cli.mcp.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Map;
import java.util.HashMap;

/**
 * Configuration for MCP (Model Context Protocol) servers loaded from JSON
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class McpConfiguration {    /**
     * Client information
     */
    @JsonProperty("client")
    @Builder.Default
    private ClientConfig client = new ClientConfig();
    
    /**
     * Multiple server configurations
     */
    @JsonProperty("servers")
    @Builder.Default
    private Map<String, ServerConfig> servers = new HashMap<>();
      @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClientConfig {
        @Builder.Default
        private String name = "misoto-cli";
        @Builder.Default
        private String version = "1.0.0";
        @Builder.Default
        private int connectTimeout = 30;
        @Builder.Default
        private int readTimeout = 60;
        @Builder.Default
        private int writeTimeout = 30;
    }
      @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ServerConfig {
        private String url;
        private String name;
        private String description;
        @Builder.Default
        private boolean enabled = false;
        @Builder.Default
        private int priority = 0;
        @Builder.Default
        private Map<String, String> headers = new HashMap<>();
        private AuthConfig auth;
          @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class AuthConfig {
            private String type; // "none", "bearer", "basic", "apikey"
            private String token;
            private String username;
            private String password;
            @Builder.Default
            private String header = "Authorization";
        }
    }
    
    /**
     * Get enabled servers sorted by priority
     */
    public Map<String, ServerConfig> getEnabledServers() {
        return servers.entrySet().stream()
            .filter(entry -> entry.getValue().isEnabled())
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                java.util.LinkedHashMap::new
            ));
    }
    
    /**
     * Get server configuration by ID
     */
    public ServerConfig getServer(String serverId) {
        return servers.get(serverId);
    }
    
    /**
     * Get default server (first enabled server)
     */
    public ServerConfig getDefaultServer() {
        return getEnabledServers().values().stream()
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Get default server ID
     */
    public String getDefaultServerId() {
        return getEnabledServers().keySet().stream()
            .findFirst()
            .orElse(null);
    }
}
