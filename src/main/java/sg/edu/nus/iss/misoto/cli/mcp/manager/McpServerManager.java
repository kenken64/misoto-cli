package sg.edu.nus.iss.misoto.cli.mcp.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.misoto.cli.mcp.client.McpClient;
import sg.edu.nus.iss.misoto.cli.mcp.config.McpConfigurationService;
import sg.edu.nus.iss.misoto.cli.mcp.config.McpConfiguration;
import sg.edu.nus.iss.misoto.cli.mcp.model.McpTool;
import sg.edu.nus.iss.misoto.cli.mcp.model.McpToolResult;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manager for multiple MCP servers
 * Handles server discovery, load balancing, and tool execution across multiple servers
 */
@Service
@Slf4j
public class McpServerManager {
    
    @Value("${misoto.mcp.auto-initialize:true}")
    private boolean autoInitialize;
      
    @Autowired
    private McpConfigurationService mcpConfigurationService;
    
    // Map of server ID to MCP client
    private final Map<String, McpClient> serverClients = new ConcurrentHashMap<>();
    
    // Map of server ID to initialization status
    private final Map<String, Boolean> serverStatus = new ConcurrentHashMap<>();
    
    // Cache of tools available on each server
    private final Map<String, List<McpTool>> serverTools = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void initialize() {
        log.info("Initializing MCP Server Manager");
        if (autoInitialize) {
            initializeServers();
        } else {
            log.info("MCP server auto-initialization disabled");
        }
    }
    
    @PreDestroy
    public void cleanup() {
        log.info("Shutting down MCP Server Manager");
        disconnectAllServers();
    }
      /**
     * Initialize all enabled servers
     */
    public void initializeServers() {
        McpConfiguration mcpConfiguration = mcpConfigurationService.getCurrentConfiguration();
        Map<String, McpConfiguration.ServerConfig> enabledServers = mcpConfiguration.getEnabledServers();
        
        if (enabledServers.isEmpty()) {
            log.warn("No MCP servers configured or enabled");
            return;
        }
        
        log.info("Initializing {} MCP servers", enabledServers.size());
        
        for (Map.Entry<String, McpConfiguration.ServerConfig> entry : enabledServers.entrySet()) {
            String serverId = entry.getKey();
            McpConfiguration.ServerConfig serverConfig = entry.getValue();
            
            try {            McpClient client = createClientForServer(serverConfig);
            serverClients.put(serverId, client);
            
            // Initialize the client asynchronously
            client.initialize()
                .thenAccept(success -> {
                    serverStatus.put(serverId, success);
                    if (success) {
                        log.info("Successfully initialized MCP server: {} ({})", 
                            serverConfig.getName(), serverConfig.getUrl());
                        
                        // Cache tools for this server
                        cacheServerTools(serverId, client);
                    } else {
                        log.error("Failed to initialize MCP server: {} ({})", 
                            serverConfig.getName(), serverConfig.getUrl());
                    }
                })
                .exceptionally(throwable -> {
                    log.error("Error initializing MCP server: {} ({})", 
                        serverConfig.getName(), serverConfig.getUrl(), throwable);
                    serverStatus.put(serverId, false);
                    return null;
                });
                    
            } catch (Exception e) {
                log.error("Failed to create client for MCP server: {} ({})", 
                    serverConfig.getName(), serverConfig.getUrl(), e);
                serverStatus.put(serverId, false);
            }
        }
    }
      /**
     * Create MCP client for a specific server
     */
    private McpClient createClientForServer(McpConfiguration.ServerConfig serverConfig) {
        McpConfiguration mcpConfiguration = mcpConfigurationService.getCurrentConfiguration();
        return new McpClient(
            serverConfig.getUrl(),
            mcpConfiguration.getClient().getName(),
            mcpConfiguration.getClient().getVersion()
        );
    }
    
    /**
     * Cache tools for a specific server
     */
    private void cacheServerTools(String serverId, McpClient client) {
        client.listTools()
            .thenAccept(tools -> {
                serverTools.put(serverId, tools);
                log.debug("Cached {} tools for server: {}", tools.size(), serverId);
            })
            .exceptionally(throwable -> {
                log.warn("Failed to cache tools for server: {}", serverId, throwable);
                return null;
            });
    }
      /**
     * Get all available servers
     */
    public Map<String, McpConfiguration.ServerConfig> getAllServers() {
        return mcpConfigurationService.getCurrentConfiguration().getServers();
    }
    
    /**
     * Get all enabled servers
     */
    public Map<String, McpConfiguration.ServerConfig> getEnabledServers() {
        return mcpConfigurationService.getCurrentConfiguration().getEnabledServers();
    }
    
    /**
     * Get all initialized servers
     */
    public Map<String, McpClient> getInitializedServers() {
        return serverClients.entrySet().stream()
            .filter(entry -> serverStatus.getOrDefault(entry.getKey(), false))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    
    /**
     * Get server status
     */
    public Map<String, Boolean> getServerStatus() {
        return new HashMap<>(serverStatus);
    }
    
    /**
     * Get client for specific server
     */
    public McpClient getServerClient(String serverId) {
        return serverClients.get(serverId);
    }
    
    /**
     * Get default server client (first initialized server)
     */
    public McpClient getDefaultClient() {
        return getInitializedServers().values().stream()
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Get default server ID
     */
    public String getDefaultServerId() {
        return getInitializedServers().keySet().stream()
            .findFirst()
            .orElse(null);
    }
    
    /**
     * List all tools from all servers
     */
    public CompletableFuture<Map<String, List<McpTool>>> listAllTools() {
        Map<String, CompletableFuture<List<McpTool>>> futures = new HashMap<>();
        
        for (Map.Entry<String, McpClient> entry : getInitializedServers().entrySet()) {
            String serverId = entry.getKey();
            McpClient client = entry.getValue();
            futures.put(serverId, client.listTools());
        }
        
        return CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                Map<String, List<McpTool>> result = new HashMap<>();
                for (Map.Entry<String, CompletableFuture<List<McpTool>>> entry : futures.entrySet()) {
                    try {
                        result.put(entry.getKey(), entry.getValue().get());
                    } catch (Exception e) {
                        log.error("Failed to get tools from server: {}", entry.getKey(), e);
                        result.put(entry.getKey(), Collections.emptyList());
                    }
                }
                return result;
            });
    }
    
    /**
     * Find tool across all servers
     */
    public CompletableFuture<Map<String, McpTool>> findTool(String toolName) {
        return listAllTools()
            .thenApply(allTools -> {
                Map<String, McpTool> foundTools = new HashMap<>();
                
                for (Map.Entry<String, List<McpTool>> entry : allTools.entrySet()) {
                    String serverId = entry.getKey();
                    Optional<McpTool> tool = entry.getValue().stream()
                        .filter(t -> t.getName().equals(toolName))
                        .findFirst();
                    
                    if (tool.isPresent()) {
                        foundTools.put(serverId, tool.get());
                    }
                }
                
                return foundTools;
            });
    }
    
    /**
     * Execute tool on specific server
     */
    public CompletableFuture<McpToolResult> callTool(String serverId, String toolName, Map<String, Object> arguments) {
        McpClient client = getServerClient(serverId);
        if (client == null) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Server not found: " + serverId));
        }
        
        if (!serverStatus.getOrDefault(serverId, false)) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Server not initialized: " + serverId));
        }
        
        return client.callTool(toolName, arguments);
    }
    
    /**
     * Execute tool on any available server (try servers in priority order)
     */
    public CompletableFuture<McpToolResult> callToolAnyServer(String toolName, Map<String, Object> arguments) {
        return findTool(toolName)
            .thenCompose(foundTools -> {
                if (foundTools.isEmpty()) {
                    return CompletableFuture.failedFuture(
                        new IllegalArgumentException("Tool not found on any server: " + toolName));
                }
                
                // Try servers in order of priority/configuration
                String firstServerId = foundTools.keySet().stream().findFirst().get();
                return callTool(firstServerId, toolName, arguments);
            });
    }
    
    /**
     * Ping all servers
     */
    public CompletableFuture<Map<String, Boolean>> pingAllServers() {
        Map<String, CompletableFuture<Boolean>> futures = new HashMap<>();
        
        for (Map.Entry<String, McpClient> entry : serverClients.entrySet()) {
            String serverId = entry.getKey();
            McpClient client = entry.getValue();
            futures.put(serverId, client.ping());
        }
        
        return CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                Map<String, Boolean> result = new HashMap<>();
                for (Map.Entry<String, CompletableFuture<Boolean>> entry : futures.entrySet()) {
                    try {
                        result.put(entry.getKey(), entry.getValue().get());
                    } catch (Exception e) {
                        log.warn("Failed to ping server: {}", entry.getKey(), e);
                        result.put(entry.getKey(), false);
                    }
                }
                return result;
            });
    }
    
    /**
     * Connect to SSE for specific server
     */
    public void connectSSE(String serverId, java.util.function.Consumer<String> messageHandler) {
        McpClient client = getServerClient(serverId);
        if (client != null) {
            client.connectSSE(messageHandler);
        } else {
            log.error("Cannot connect SSE: Server not found: {}", serverId);
        }
    }
    
    /**
     * Connect to WebSocket for specific server
     */
    public void connectWebSocket(String serverId, java.util.function.Consumer<String> messageHandler) {
        McpClient client = getServerClient(serverId);
        if (client != null) {
            client.connectWebSocket(messageHandler);
        } else {
            log.error("Cannot connect WebSocket: Server not found: {}", serverId);
        }
    }
    
    /**
     * Disconnect from specific server
     */
    public void disconnectServer(String serverId) {
        McpClient client = serverClients.get(serverId);
        if (client != null) {
            client.disconnect();
            serverStatus.put(serverId, false);
            serverTools.remove(serverId);
            log.info("Disconnected from MCP server: {}", serverId);
        }
    }
    
    /**
     * Disconnect from all servers
     */
    public void disconnectAllServers() {
        for (String serverId : new HashSet<>(serverClients.keySet())) {
            disconnectServer(serverId);
        }
        serverClients.clear();
        serverStatus.clear();
        serverTools.clear();
    }
      /**
     * Reconnect to specific server
     */
    public CompletableFuture<Boolean> reconnectServer(String serverId) {
        McpConfiguration mcpConfiguration = mcpConfigurationService.getCurrentConfiguration();
        McpConfiguration.ServerConfig serverConfig = mcpConfiguration.getServer(serverId);
        if (serverConfig == null) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Server configuration not found: " + serverId));
        }
        
        // Disconnect first if connected
        disconnectServer(serverId);
        
        // Create new client and initialize
        try {
            McpClient client = createClientForServer(serverConfig);
            serverClients.put(serverId, client);
            
            return client.initialize()
                .thenApply(success -> {
                    serverStatus.put(serverId, success);
                    if (success) {
                        cacheServerTools(serverId, client);
                        log.info("Successfully reconnected to MCP server: {}", serverId);
                    }
                    return success;
                });
        } catch (Exception e) {
            log.error("Failed to reconnect to MCP server: {}", serverId, e);
            return CompletableFuture.completedFuture(false);
        }
    }
      /**
     * Get server information
     */
    public Map<String, Object> getServerInfo(String serverId) {
        McpConfiguration mcpConfiguration = mcpConfigurationService.getCurrentConfiguration();
        McpConfiguration.ServerConfig config = mcpConfiguration.getServer(serverId);
        McpClient client = getServerClient(serverId);
        Boolean status = serverStatus.get(serverId);
        List<McpTool> tools = serverTools.get(serverId);
        
        Map<String, Object> info = new HashMap<>();
        if (config != null) {
            info.put("config", config);
        }
        info.put("initialized", status != null ? status : false);
        info.put("connected", client != null);
        info.put("toolCount", tools != null ? tools.size() : 0);
        
        if (client != null && status != null && status) {
            info.put("serverCapabilities", client.getServerCapabilities());
        }
        
        return info;
    }
      /**
     * Initialize all servers (alias for initializeServers)
     */
    public void initializeAll() {
        initializeServers();
    }
    
    /**
     * Ping all servers and return true if any are responding
     */
    public boolean pingAll() {
        try {
            Map<String, Boolean> results = pingAllServers().get();
            return results.values().stream().anyMatch(Boolean::booleanValue);
        } catch (Exception e) {
            log.error("Error pinging servers", e);
            return false;
        }
    }
    
    /**
     * Get all tools from all servers as a flat list
     */
    public List<McpTool> getAllToolsFlat() {
        try {
            Map<String, List<McpTool>> toolsMap = listAllTools().get();
            return toolsMap.values().stream()
                .flatMap(List::stream)
                .toList();
        } catch (Exception e) {
            log.error("Error listing tools", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Call tool on any available server
     */
    public McpToolResult callTool(String toolName, Map<String, Object> arguments) {
        try {
            return callToolAnyServer(toolName, arguments).get();
        } catch (Exception e) {
            log.error("Error calling tool: {}", toolName, e);
            throw new RuntimeException("Failed to call tool: " + toolName, e);
        }
    }
    
    /**
     * Connect to SSE streams for all servers
     */
    public void connectAllSSE(java.util.function.Consumer<String> messageHandler) {
        for (String serverId : getInitializedServers().keySet()) {
            connectSSE(serverId, messageHandler);
        }
    }
    
    /**
     * Connect to WebSockets for all servers
     */
    public void connectAllWebSocket(java.util.function.Consumer<String> messageHandler) {
        for (String serverId : getInitializedServers().keySet()) {
            connectWebSocket(serverId, messageHandler);
        }
    }
    
    /**
     * Disconnect from all servers (alias for disconnectAllServers)
     */
    public void disconnectAll() {
        disconnectAllServers();
    }
}
