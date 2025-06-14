package sg.edu.nus.iss.mcp.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import sg.edu.nus.iss.mcp.server.protocol.*;
import sg.edu.nus.iss.mcp.server.model.*;
import sg.edu.nus.iss.mcp.server.service.SseService;
import sg.edu.nus.iss.mcp.server.service.ToolService;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.List;
import java.util.UUID;

/**
 * Main MCP Server REST Controller
 * Handles MCP protocol requests and provides SSE endpoints
 */
@RestController
@RequestMapping("/mcp")
@CrossOrigin(origins = "*")
@Slf4j
public class McpController {
    
    @Autowired
    private SseService sseService;
    
    @Autowired
    private ToolService toolService;
    
    /**
     * Initialize MCP connection
     */
    @PostMapping("/initialize")
    public ResponseEntity<McpResponse> initialize(@RequestBody McpRequest request) {
        log.info("Received initialize request: {}", request.getId());
        
        try {
            Map<String, Object> capabilities = Map.of(
                "tools", Map.of("listChanged", true),
                "resources", Map.of("subscribe", true, "listChanged", true),
                "logging", Map.of(),
                "experimental", Map.of("streaming", true)
            );
            
            Map<String, Object> serverInfo = Map.of(
                "name", "MCP Demo Server",
                "version", "1.0.0",
                "capabilities", capabilities
            );
            
            McpResponse response = new McpResponse(request.getId(), Map.of(
                "protocolVersion", "2024-11-05",
                "serverInfo", serverInfo
            ));
            
            log.info("Initialization successful for request: {}", request.getId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Initialization failed for request: {}", request.getId(), e);
            return ResponseEntity.ok(new McpResponse(request.getId(), McpError.internalError()));
        }
    }
    
    /**
     * List available tools
     */
    @PostMapping("/tools/list")
    public ResponseEntity<McpResponse> listTools(@RequestBody McpRequest request) {
        log.info("Received tools/list request: {}", request.getId());
        
        try {
            List<McpTool> tools = toolService.getAllTools();
            McpResponse response = new McpResponse(request.getId(), Map.of("tools", tools));
            
            log.info("Returning {} tools for request: {}", tools.size(), request.getId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to list tools for request: {}", request.getId(), e);
            return ResponseEntity.ok(new McpResponse(request.getId(), McpError.internalError()));
        }
    }
    
    /**
     * Execute a tool
     */
    @PostMapping("/tools/call")
    public ResponseEntity<McpResponse> callTool(@RequestBody McpRequest request) {
        log.info("Received tools/call request: {}", request.getId());
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) request.getParams();
            String toolName = (String) params.get("name");
            @SuppressWarnings("unchecked")
            Map<String, Object> arguments = (Map<String, Object>) params.getOrDefault("arguments", Map.of());
            
            if (toolName == null) {
                return ResponseEntity.ok(new McpResponse(request.getId(), McpError.invalidParams()));
            }
            
            if (!toolService.toolExists(toolName)) {
                return ResponseEntity.ok(new McpResponse(request.getId(), McpError.toolNotFound(toolName)));
            }
            
            McpToolResult result = toolService.executeTool(toolName, arguments);
            McpResponse response = new McpResponse(request.getId(), result);
            
            log.info("Tool {} executed for request: {}", toolName, request.getId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to execute tool for request: {}", request.getId(), e);
            return ResponseEntity.ok(new McpResponse(request.getId(), McpError.internalError()));
        }
    }
    
    /**
     * Handle ping requests
     */
    @PostMapping("/ping")
    public ResponseEntity<McpResponse> ping(@RequestBody McpRequest request) {
        log.debug("Received ping request: {}", request.getId());
        return ResponseEntity.ok(new McpResponse(request.getId(), Map.of("status", "pong")));
    }
    
    /**
     * Create SSE connection for real-time communication
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter createSseConnection(
            @RequestParam(value = "clientId", required = false) String clientId) {
        
        if (clientId == null) {
            clientId = "client-" + UUID.randomUUID().toString().substring(0, 8);
        }
        
        log.info("Creating SSE connection for client: {}", clientId);
        return sseService.createConnection(clientId);
    }
    
    /**
     * Send a message via SSE to a specific client
     */
    @PostMapping("/stream/{clientId}/send")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @PathVariable String clientId,
            @RequestBody Map<String, Object> message) {
        
        log.info("Sending message to client: {}", clientId);
        
        String eventName = (String) message.getOrDefault("event", "message");
        Object data = message.getOrDefault("data", message);
        
        sseService.sendToClient(clientId, eventName, data);
        
        return ResponseEntity.ok(Map.of(
            "status", "sent",
            "clientId", clientId,
            "event", eventName
        ));
    }
    
    /**
     * Broadcast a message to all connected clients
     */
    @PostMapping("/stream/broadcast")
    public ResponseEntity<Map<String, Object>> broadcastMessage(@RequestBody Map<String, Object> message) {
        log.info("Broadcasting message to all clients");
        
        String eventName = (String) message.getOrDefault("event", "broadcast");
        Object data = message.getOrDefault("data", message);
        
        sseService.broadcastToAll(eventName, data);
        
        return ResponseEntity.ok(Map.of(
            "status", "broadcasted",
            "event", eventName,
            "activeConnections", sseService.getActiveConnectionCount()
        ));
    }
    
    /**
     * Get server status and statistics
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
            "status", "running",
            "activeConnections", sseService.getActiveConnectionCount(),
            "connectedClients", sseService.getConnectedClientCount(),
            "availableTools", toolService.getToolCount(),
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
