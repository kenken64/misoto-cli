package sg.edu.nus.iss.mcp.server.config;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import sg.edu.nus.iss.mcp.server.protocol.*;
import sg.edu.nus.iss.mcp.server.service.ToolService;
import sg.edu.nus.iss.mcp.server.service.SseService;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for MCP protocol
 * Handles bidirectional communication with MCP clients
 */
@Component
@Slf4j
public class McpWebSocketHandler extends TextWebSocketHandler {
    
    @Autowired
    private ToolService toolService;
    
    @Autowired
    private SseService sseService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        log.info("WebSocket connection established: {}", sessionId);
        
        // Send welcome message
        McpNotification welcome = new McpNotification("connected");
        welcome.setParams(Map.of(
            "message", "Connected to MCP Server via WebSocket",
            "sessionId", sessionId,
            "timestamp", System.currentTimeMillis()
        ));
        
        sendMessage(session, welcome);
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();
        String payload = message.getPayload();
        
        log.info("Received WebSocket message from {}: {}", sessionId, payload);
        
        try {
            // Parse the message as MCP request
            McpMessage mcpMessage = objectMapper.readValue(payload, McpMessage.class);
            
            if (mcpMessage instanceof McpRequest) {
                handleMcpRequest(session, (McpRequest) mcpMessage);
            } else if (mcpMessage instanceof McpNotification) {
                handleMcpNotification(session, (McpNotification) mcpMessage);
            } else {
                log.warn("Unknown message type received from {}", sessionId);
            }
            
        } catch (Exception e) {
            log.error("Failed to process WebSocket message from {}: {}", sessionId, e.getMessage(), e);
            
            // Send error response
            McpResponse errorResponse = new McpResponse(null, McpError.parseError());
            sendMessage(session, errorResponse);
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        log.info("WebSocket connection closed: {} with status: {}", sessionId, status);
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        log.error("WebSocket transport error for {}: {}", sessionId, exception.getMessage(), exception);
        sessions.remove(sessionId);
    }
    
    /**
     * Handle MCP request messages
     */
    private void handleMcpRequest(WebSocketSession session, McpRequest request) throws Exception {
        String method = request.getMethod();
        Object id = request.getId();
        
        log.info("Handling MCP request: {} with ID: {}", method, id);
        
        McpResponse response;
        
        switch (method) {
            case "initialize":
                response = handleInitialize(id);
                break;
                
            case "tools/list":
                response = handleToolsList(id);
                break;
                
            case "tools/call":
                response = handleToolsCall(id, request.getParams());
                break;
                
            case "ping":
                response = new McpResponse(id, Map.of("status", "pong"));
                break;
                
            default:
                response = new McpResponse(id, McpError.methodNotFound());
                break;
        }
        
        sendMessage(session, response);
    }
    
    /**
     * Handle MCP notification messages
     */
    private void handleMcpNotification(WebSocketSession session, McpNotification notification) {
        String method = notification.getMethod();
        log.info("Handling MCP notification: {}", method);
        
        // Handle notifications (they don't require responses)
        switch (method) {
            case "initialized":
                log.info("Client initialized");
                break;
                
            default:
                log.warn("Unknown notification method: {}", method);
                break;
        }
    }
    
    /**
     * Handle initialize request
     */
    private McpResponse handleInitialize(Object id) {
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
        
        return new McpResponse(id, Map.of(
            "protocolVersion", "2024-11-05",
            "serverInfo", serverInfo
        ));
    }
    
    /**
     * Handle tools/list request
     */
    private McpResponse handleToolsList(Object id) {
        return new McpResponse(id, Map.of("tools", toolService.getAllTools()));
    }
    
    /**
     * Handle tools/call request
     */
    private McpResponse handleToolsCall(Object id, Object params) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> paramsMap = (Map<String, Object>) params;
            String toolName = (String) paramsMap.get("name");
            @SuppressWarnings("unchecked")
            Map<String, Object> arguments = (Map<String, Object>) paramsMap.getOrDefault("arguments", Map.of());
            
            if (toolName == null) {
                return new McpResponse(id, McpError.invalidParams());
            }
            
            if (!toolService.toolExists(toolName)) {
                return new McpResponse(id, McpError.toolNotFound(toolName));
            }
            
            return new McpResponse(id, toolService.executeTool(toolName, arguments));
            
        } catch (Exception e) {
            log.error("Error handling tools/call: {}", e.getMessage(), e);
            return new McpResponse(id, McpError.internalError());
        }
    }
    
    /**
     * Send a message through WebSocket
     */
    private void sendMessage(WebSocketSession session, Object message) throws Exception {
        if (session.isOpen()) {
            String jsonMessage = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(jsonMessage));
            log.debug("Sent WebSocket message: {}", jsonMessage);
        }
    }
    
    /**
     * Broadcast a message to all connected WebSocket sessions
     */
    public void broadcastMessage(Object message) {
        sessions.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    sendMessage(session, message);
                }
            } catch (Exception e) {
                log.error("Failed to broadcast message to session {}: {}", session.getId(), e.getMessage());
            }
        });
    }
    
    /**
     * Get the number of active WebSocket connections
     */
    public int getActiveConnectionCount() {
        return sessions.size();
    }
}
