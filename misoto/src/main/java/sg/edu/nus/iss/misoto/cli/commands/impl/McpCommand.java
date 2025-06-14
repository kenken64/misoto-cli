package sg.edu.nus.iss.misoto.cli.commands.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sg.edu.nus.iss.misoto.cli.commands.Command;
import sg.edu.nus.iss.misoto.cli.mcp.client.McpClient;
import sg.edu.nus.iss.misoto.cli.mcp.config.McpCliOptions;
import sg.edu.nus.iss.misoto.cli.mcp.config.McpConfiguration;
import sg.edu.nus.iss.misoto.cli.mcp.config.McpConfigurationService;
import sg.edu.nus.iss.misoto.cli.mcp.manager.McpServerManager;
import sg.edu.nus.iss.misoto.cli.mcp.model.McpTool;
import sg.edu.nus.iss.misoto.cli.mcp.model.McpToolResult;

import java.util.List;
import java.util.Map;

/**
 * MCP (Model Context Protocol) command for interacting with MCP servers
 */
@Component
@Slf4j
public class McpCommand implements Command {
      @Autowired
    private McpServerManager mcpServerManager;
    
    @Autowired
    private McpConfigurationService configurationService;
    
    @Autowired
    private McpCliOptions cliOptions;
    
    @Override
    public String getName() {
        return "mcp";
    }
    
    @Override
    public String getDescription() {
        return "Interact with Model Context Protocol servers";
    }
      @Override
    public String getCategory() {
        return "MCP";
    }
    
    @Override
    public boolean isHidden() {
        return false;
    }
    
    @Override
    public boolean requiresAuth() {
        return false;
    }
      @Override
    public String getUsage() {
        return "mcp <subcommand> [options]";
    }
    
    @Override
    public List<String> getExamples() {
        return List.of(
            "mcp config show",
            "mcp config load /path/to/mcp.json",
            "mcp config create",
            "mcp init",
            "mcp tools list", 
            "mcp call echo",
            "mcp status",
            "mcp sse"
        );
    }
    
    @Override
    public void execute(List<String> argsList) throws Exception {
        String[] args = argsList.toArray(new String[0]);
        if (args.length < 1) {
            showUsage();
            return;
        }
        
        String subCommand = args[0].toLowerCase();
          try {
            switch (subCommand) {
                case "config":
                    handleConfig(args);
                    break;
                case "init":
                case "initialize":
                    handleInitialize();
                    break;
                case "ping":
                    handlePing();
                    break;
                case "tools":
                    handleTools(args);
                    break;
                case "call":
                    handleToolCall(args);
                    break;
                case "sse":
                    handleSSE();
                    break;
                case "ws":
                case "websocket":
                    handleWebSocket();
                    break;
                case "status":
                    handleStatus();
                    break;
                case "disconnect":
                    handleDisconnect();
                    break;
                default:
                    System.err.println("Unknown MCP command: " + subCommand);
                    showUsage();
                    break;
            }
        } catch (Exception e) {
            log.error("Error executing MCP command: {}", subCommand, e);
            System.err.println("Error: " + e.getMessage());
        }
    }
      private void handleConfig(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: mcp config <subcommand> [options]");
            System.err.println("Subcommands: show, load, create, validate, save");
            return;
        }
        
        String subCommand = args[1].toLowerCase();
        
        try {
            switch (subCommand) {
                case "show":
                    handleConfigShow();
                    break;
                case "load":
                    handleConfigLoad(args);
                    break;
                case "create":
                    handleConfigCreate(args);
                    break;
                case "validate":
                    handleConfigValidate(args);
                    break;
                case "save":
                    handleConfigSave(args);
                    break;
                default:
                    System.err.println("Unknown config subcommand: " + subCommand);
                    System.err.println("Available subcommands: show, load, create, validate, save");
                    break;
            }
        } catch (Exception e) {
            log.error("Error executing config command: {}", subCommand, e);
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    private void handleConfigShow() {
        System.out.println("Current MCP Configuration:");
        McpConfiguration config = configurationService.getCurrentConfiguration();
        
        if (config == null) {
            System.out.println("No MCP configuration loaded");
            return;
        }
          System.out.println("Client Configuration:");
        System.out.printf("  Name: %s%n", config.getClient().getName());
        System.out.printf("  Version: %s%n", config.getClient().getVersion());
        System.out.printf("  Connection Timeout: %d sec%n", config.getClient().getConnectTimeout());
        System.out.printf("  Read Timeout: %d sec%n", config.getClient().getReadTimeout());
        System.out.printf("  Write Timeout: %d sec%n", config.getClient().getWriteTimeout());
        
        System.out.println("\nServer Configurations:");
        config.getServers().forEach((key, server) -> {
            System.out.printf("  [%s] %s%n", key, server.getName());
            System.out.printf("    URL: %s%n", server.getUrl());
            System.out.printf("    Description: %s%n", server.getDescription());
            System.out.printf("    Enabled: %s%n", server.isEnabled());
            System.out.printf("    Priority: %d%n", server.getPriority());
            if (server.getHeaders() != null && !server.getHeaders().isEmpty()) {
                System.out.println("    Headers:");
                server.getHeaders().forEach((headerKey, headerValue) -> 
                    System.out.printf("      %s: %s%n", headerKey, headerValue)
                );
            }
        });
    }
    
    private void handleConfigLoad(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: mcp config load <config-file-path>");
            return;
        }
        
        String configPath = args[2];
        System.out.printf("Loading MCP configuration from: %s%n", configPath);
        
        try {
            configurationService.loadConfiguration(configPath);
            System.out.println("✓ MCP configuration loaded successfully");
            handleConfigShow(); // Show the loaded configuration
        } catch (Exception e) {
            System.err.printf("✗ Failed to load configuration: %s%n", e.getMessage());
        }
    }
    
    private void handleConfigCreate(String[] args) {
        String configPath = args.length > 2 ? args[2] : null;
        
        if (configPath == null) {
            configPath = System.getProperty("user.home") + "/.misoto/mcp.json";
        }
        
        System.out.printf("Creating default MCP configuration at: %s%n", configPath);
        
        try {
            configurationService.createDefaultConfiguration(configPath);
            System.out.println("✓ Default MCP configuration created successfully");
            System.out.printf("Edit the file at %s to customize your MCP settings%n", configPath);
        } catch (Exception e) {
            System.err.printf("✗ Failed to create configuration: %s%n", e.getMessage());
        }
    }
    
    private void handleConfigValidate(String[] args) {
        String configPath = args.length > 2 ? args[2] : null;
        
        if (configPath == null) {
            // Validate current configuration
            System.out.println("Validating current MCP configuration...");
            McpConfiguration config = configurationService.getCurrentConfiguration();
            if (config == null) {
                System.err.println("✗ No configuration loaded to validate");
                return;
            }
            
            if (validateConfiguration(config)) {
                System.out.println("✓ Current configuration is valid");
            } else {
                System.err.println("✗ Current configuration has issues");
            }
        } else {
            // Validate specific file
            System.out.printf("Validating MCP configuration file: %s%n", configPath);
            try {
                McpConfiguration config = configurationService.loadConfigurationFromFile(configPath);
                if (validateConfiguration(config)) {
                    System.out.println("✓ Configuration file is valid");
                } else {
                    System.err.println("✗ Configuration file has issues");
                }
            } catch (Exception e) {
                System.err.printf("✗ Failed to validate configuration: %s%n", e.getMessage());
            }
        }
    }
    
    private void handleConfigSave(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: mcp config save <config-file-path>");
            return;
        }
        
        String configPath = args[2];
        System.out.printf("Saving current MCP configuration to: %s%n", configPath);
        
        try {
            configurationService.saveConfiguration(configPath);
            System.out.println("✓ MCP configuration saved successfully");
        } catch (Exception e) {
            System.err.printf("✗ Failed to save configuration: %s%n", e.getMessage());
        }
    }
    
    private boolean validateConfiguration(McpConfiguration config) {
        boolean valid = true;
        
        // Validate client configuration
        if (config.getClient() == null) {
            System.err.println("  ✗ Missing client configuration");
            valid = false;
        } else {
            if (config.getClient().getName() == null || config.getClient().getName().trim().isEmpty()) {
                System.err.println("  ✗ Client name is required");
                valid = false;
            }
            if (config.getClient().getVersion() == null || config.getClient().getVersion().trim().isEmpty()) {
                System.err.println("  ✗ Client version is required");
                valid = false;
            }
        }
        
        // Validate server configurations
        if (config.getServers() == null || config.getServers().isEmpty()) {
            System.err.println("  ✗ At least one server configuration is required");
            valid = false;
        } else {
            config.getServers().forEach((key, server) -> {
                if (server.getUrl() == null || server.getUrl().trim().isEmpty()) {
                    System.err.printf("  ✗ Server '%s' is missing URL%n", key);
                }
                if (server.getName() == null || server.getName().trim().isEmpty()) {
                    System.err.printf("  ✗ Server '%s' is missing name%n", key);
                }
                try {
                    new java.net.URL(server.getUrl());
                } catch (java.net.MalformedURLException e) {
                    System.err.printf("  ✗ Server '%s' has invalid URL: %s%n", key, server.getUrl());
                }
            });
        }
        
        return valid;
    }
    
    private void handleInitialize() {
        System.out.println("Initializing MCP connections...");
        
        try {
            mcpServerManager.initializeAll();
            System.out.println("✓ MCP connections initialized successfully");
        } catch (Exception e) {
            log.error("Error initializing MCP connections", e);
            System.err.println("✗ Error initializing MCP connections: " + e.getMessage());
        }
    }
      private void handlePing() {
        System.out.println("Pinging MCP servers...");
        
        try {
            boolean anySuccess = mcpServerManager.pingAll();
            
            if (anySuccess) {
                System.out.println("✓ At least one MCP server is responding");
            } else {
                System.err.println("✗ No MCP servers are responding");
            }
        } catch (Exception e) {
            log.error("Error pinging MCP servers", e);
            System.err.println("✗ Error pinging MCP servers: " + e.getMessage());
        }
    }
    
    private void handleTools(String[] args) {
        if (args.length > 1 && "list".equals(args[1])) {
            listTools();
        } else {
            System.err.println("Usage: mcp tools list");
        }
    }    private void listTools() {
        System.out.println("Listing available MCP tools...");
        
        try {
            List<McpTool> tools = mcpServerManager.getAllToolsFlat();
            
            if (tools.isEmpty()) {
                System.out.println("No tools available");
            } else {
                System.out.println("Available tools:");
                for (McpTool tool : tools) {
                    System.out.printf("  • %s: %s%n", tool.getName(), tool.getDescription());
                    if (tool.getInputSchema() != null) {
                        System.out.printf("    Schema: %s%n", tool.getInputSchema());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error listing MCP tools", e);
            System.err.println("✗ Error listing tools: " + e.getMessage());
        }
    }
      private void handleToolCall(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: mcp call <tool-name> [arguments]");
            return;
        }
        
        String toolName = args[1];
        Map<String, Object> arguments = Map.of(); // Parse from args if needed
        
        System.out.printf("Calling tool '%s'...%n", toolName);
        
        try {
            McpToolResult result = mcpServerManager.callTool(toolName, arguments);
            
            System.out.println("Tool execution result:");
            if (result.getIsError()) {
                System.err.println("✗ Tool execution failed:");
            } else {
                System.out.println("✓ Tool executed successfully:");
            }
            
            if (result.getContent() != null) {
                for (McpToolResult.ContentItem item : result.getContent()) {
                    System.out.println("  " + item.getText());
                }
            }
        } catch (Exception e) {
            log.error("Error calling MCP tool: {}", toolName, e);
            System.err.println("✗ Error calling tool: " + e.getMessage());
        }
    }
      private void handleSSE() {
        System.out.println("Connecting to MCP SSE streams...");
        
        mcpServerManager.connectAllSSE(message -> {
            System.out.println("SSE Message: " + message);
        });
        
        System.out.println("✓ SSE connections established. Press any key to stop...");
        try {
            System.in.read();
        } catch (Exception e) {
            // Ignore
        }
        
        mcpServerManager.disconnectAll();
        System.out.println("SSE connections closed");
    }
      private void handleWebSocket() {
        System.out.println("Connecting to MCP WebSockets...");
        
        mcpServerManager.connectAllWebSocket(message -> {
            System.out.println("WebSocket Message: " + message);
        });
        
        System.out.println("✓ WebSocket connections established. Press any key to stop...");
        try {
            System.in.read();
        } catch (Exception e) {
            // Ignore
        }
        
        mcpServerManager.disconnectAll();
        System.out.println("WebSocket connections closed");
    }
      private void handleStatus() {
        System.out.println("MCP Server Manager Status:");
        
        try {
            Map<String, Boolean> serverStatus = mcpServerManager.getServerStatus();
            
            if (serverStatus.isEmpty()) {
                System.out.println("  No servers configured");
            } else {
                serverStatus.forEach((serverName, isConnected) -> {
                    String status = isConnected ? "✓ Connected" : "✗ Disconnected";
                    System.out.printf("  %s: %s%n", serverName, status);
                });
            }
        } catch (Exception e) {
            log.error("Error getting server status", e);
            System.err.println("✗ Error getting server status: " + e.getMessage());
        }
    }
      private void handleDisconnect() {
        System.out.println("Disconnecting from MCP servers...");
        mcpServerManager.disconnectAll();
        System.out.println("✓ Disconnected from all MCP servers");
    }
      private void showUsage() {
        System.out.println("MCP (Model Context Protocol) Commands:");
        System.out.println("  mcp config show          - Show current MCP configuration");
        System.out.println("  mcp config load <file>   - Load MCP configuration from file");
        System.out.println("  mcp config create [file] - Create default MCP configuration");
        System.out.println("  mcp config validate [file] - Validate MCP configuration");
        System.out.println("  mcp config save <file>   - Save current configuration to file");
        System.out.println("  mcp init                 - Initialize connections to MCP servers");
        System.out.println("  mcp ping                 - Test server connectivity");
        System.out.println("  mcp tools list           - List available tools");
        System.out.println("  mcp call <tool-name>     - Execute a tool");
        System.out.println("  mcp sse                  - Connect to SSE streams");
        System.out.println("  mcp websocket            - Connect to WebSockets");
        System.out.println("  mcp status               - Show connection status");
        System.out.println("  mcp disconnect           - Disconnect from servers");
        System.out.println();
        System.out.println("Configuration Examples:");
        System.out.println("  claude-code mcp config show");
        System.out.println("  claude-code mcp config create ~/.misoto/mcp.json");
        System.out.println("  claude-code mcp config load /path/to/mcp.json");
        System.out.println();
        System.out.println("Server Interaction Examples:");
        System.out.println("  claude-code mcp init");
        System.out.println("  claude-code mcp tools list");
        System.out.println("  claude-code mcp call echo");
        System.out.println("  claude-code mcp sse");
    }
}
