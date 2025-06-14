package sg.edu.nus.iss.mcp.server.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Map;
import java.io.IOException;

/**
 * Service for managing Server-Sent Events (SSE) connections
 * Handles real-time streaming of MCP messages to clients
 */
@Service
@Slf4j
public class SseService {
    
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> clientConnections = new ConcurrentHashMap<>();
    private final Map<SseEmitter, String> emitterToClientId = new ConcurrentHashMap<>();
    
    /**
     * Create a new SSE connection for a client
     */
    public SseEmitter createConnection(String clientId) {
        log.info("Creating SSE connection for client: {}", clientId);
        
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // No timeout
        
        // Add client connection
        clientConnections.computeIfAbsent(clientId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitterToClientId.put(emitter, clientId);
        
        // Handle connection cleanup
        emitter.onCompletion(() -> {
            log.info("SSE connection completed for client: {}", clientId);
            removeConnection(emitter, clientId);
        });
        
        emitter.onTimeout(() -> {
            log.info("SSE connection timeout for client: {}", clientId);
            removeConnection(emitter, clientId);
        });
        
        emitter.onError((ex) -> {
            log.error("SSE connection error for client: {}", clientId, ex);
            removeConnection(emitter, clientId);
        });
        
        // Send initial connection message
        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data(Map.of(
                    "message", "Connected to MCP Server",
                    "clientId", clientId,
                    "timestamp", System.currentTimeMillis()
                )));
        } catch (IOException e) {
            log.error("Failed to send initial message to client: {}", clientId, e);
            removeConnection(emitter, clientId);
        }
        
        return emitter;
    }
    
    /**
     * Send a message to a specific client
     */
    public void sendToClient(String clientId, String eventName, Object data) {
        CopyOnWriteArrayList<SseEmitter> emitters = clientConnections.get(clientId);
        if (emitters != null) {
            emitters.removeIf(emitter -> {
                try {
                    emitter.send(SseEmitter.event().name(eventName).data(data));
                    return false; // Keep this emitter
                } catch (IOException e) {
                    log.error("Failed to send message to client: {}", clientId, e);
                    return true; // Remove this emitter
                }
            });
        }
    }
    
    /**
     * Send a message to all connected clients
     */
    public void broadcastToAll(String eventName, Object data) {
        log.debug("Broadcasting {} event to all clients", eventName);
        
        clientConnections.forEach((clientId, emitters) -> {
            emitters.removeIf(emitter -> {
                try {
                    emitter.send(SseEmitter.event().name(eventName).data(data));
                    return false; // Keep this emitter
                } catch (IOException e) {
                    log.error("Failed to broadcast to client: {}", clientId, e);
                    return true; // Remove this emitter
                }
            });
        });
    }
    
    /**
     * Send MCP notification to client
     */
    public void sendMcpNotification(String clientId, Object notification) {
        sendToClient(clientId, "mcp-notification", notification);
    }
    
    /**
     * Send MCP response to client
     */
    public void sendMcpResponse(String clientId, Object response) {
        sendToClient(clientId, "mcp-response", response);
    }
    
    /**
     * Remove a connection
     */
    private void removeConnection(SseEmitter emitter, String clientId) {
        emitterToClientId.remove(emitter);
        CopyOnWriteArrayList<SseEmitter> emitters = clientConnections.get(clientId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                clientConnections.remove(clientId);
                log.info("Removed all connections for client: {}", clientId);
            }
        }
    }
    
    /**
     * Get the number of active connections
     */
    public int getActiveConnectionCount() {
        return clientConnections.values().stream()
            .mapToInt(CopyOnWriteArrayList::size)
            .sum();
    }
    
    /**
     * Get the number of connected clients
     */
    public int getConnectedClientCount() {
        return clientConnections.size();
    }
}
