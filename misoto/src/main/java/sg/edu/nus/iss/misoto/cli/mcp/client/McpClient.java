package sg.edu.nus.iss.misoto.cli.mcp.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.misoto.cli.mcp.model.*;
import sg.edu.nus.iss.misoto.cli.mcp.protocol.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * MCP Client for connecting to Model Context Protocol servers
 * Supports REST API, Server-Sent Events (SSE), and WebSocket connections
 */
@Slf4j
public class McpClient {
    
    private final String serverUrl;
    private final String clientName;
    private final String clientVersion;
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AtomicLong requestIdCounter = new AtomicLong(1);
    private final Map<Long, CompletableFuture<McpResponse>> pendingRequests = new ConcurrentHashMap<>();
    
    // SSE connection
    private EventSource sseEventSource;
    private Consumer<String> sseMessageHandler;
    
    // WebSocket connection
    private WebSocket webSocket;
    private Consumer<String> wsMessageHandler;
      // Connection state
    private boolean initialized = false;
    private Map<String, Object> serverCapabilities;
    
    /**
     * Constructor for MCP client with specific server configuration
     */
    public McpClient(String serverUrl, String clientName, String clientVersion) {
        this.serverUrl = serverUrl;
        this.clientName = clientName;
        this.clientVersion = clientVersion;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(30))
            .readTimeout(Duration.ofSeconds(60))
            .writeTimeout(Duration.ofSeconds(30))
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Default constructor for backward compatibility
     */
    public McpClient() {
        this("http://localhost:8080", "misoto-cli", "1.0.0");
    }
      @PreDestroy
    public void cleanup() {
        disconnect();
    }
    
    /**
     * Initialize connection with the MCP server
     */
    public CompletableFuture<Boolean> initialize() {
        log.info("Initializing MCP connection with server: {}", serverUrl);
        
        Map<String, Object> clientInfo = Map.of(
            "name", clientName,
            "version", clientVersion
        );
        
        Map<String, Object> capabilities = Map.of(
            "tools", Map.of(),
            "resources", Map.of()
        );
        
        Map<String, Object> params = Map.of(
            "protocolVersion", "2024-11-05",
            "capabilities", capabilities,
            "clientInfo", clientInfo
        );
        
        return sendRequest("initialize", params)
            .thenApply(response -> {
                if (response.hasError()) {
                    log.error("Failed to initialize MCP connection: {}", response.getError().getMessage());
                    return false;
                }
                
                try {
                    Map<String, Object> result = (Map<String, Object>) response.getResult();
                    serverCapabilities = (Map<String, Object>) result.get("capabilities");
                    initialized = true;
                    log.info("MCP connection initialized successfully");
                    log.debug("Server capabilities: {}", serverCapabilities);
                    return true;
                } catch (Exception e) {
                    log.error("Error processing initialization response", e);
                    return false;
                }
            });
    }
    
    /**
     * List available tools from the server
     */
    public CompletableFuture<List<McpTool>> listTools() {
        if (!initialized) {
            return CompletableFuture.failedFuture(new IllegalStateException("MCP client not initialized"));
        }
        
        return sendRequest("tools/list", Map.of())
            .thenApply(response -> {
                if (response.hasError()) {
                    throw new RuntimeException("Failed to list tools: " + response.getError().getMessage());
                }
                
                try {
                    Map<String, Object> result = (Map<String, Object>) response.getResult();
                    List<Map<String, Object>> toolsData = (List<Map<String, Object>>) result.get("tools");
                    
                    return toolsData.stream()
                        .map(this::mapToTool)
                        .toList();
                } catch (Exception e) {
                    log.error("Error parsing tools response", e);
                    throw new RuntimeException("Failed to parse tools response", e);
                }
            });
    }
    
    /**
     * Execute a tool on the server
     */
    public CompletableFuture<McpToolResult> callTool(String toolName, Map<String, Object> arguments) {
        if (!initialized) {
            return CompletableFuture.failedFuture(new IllegalStateException("MCP client not initialized"));
        }
        
        Map<String, Object> params = Map.of(
            "name", toolName,
            "arguments", arguments != null ? arguments : Map.of()
        );
        
        return sendRequest("tools/call", params)
            .thenApply(response -> {
                if (response.hasError()) {
                    throw new RuntimeException("Tool execution failed: " + response.getError().getMessage());
                }
                
                try {
                    return mapToToolResult((Map<String, Object>) response.getResult());
                } catch (Exception e) {
                    log.error("Error parsing tool result", e);
                    throw new RuntimeException("Failed to parse tool result", e);
                }
            });
    }
    
    /**
     * Test server connectivity
     */
    public CompletableFuture<Boolean> ping() {
        return sendRequest("ping", Map.of())
            .thenApply(response -> !response.hasError())
            .exceptionally(throwable -> {
                log.warn("Ping failed: {}", throwable.getMessage());
                return false;
            });
    }
    
    /**
     * Send a JSON-RPC request to the server
     */
    private CompletableFuture<McpResponse> sendRequest(String method, Object params) {
        long requestId = requestIdCounter.getAndIncrement();
        McpRequest request = new McpRequest(requestId, method, params);
        
        try {
            String requestJson = objectMapper.writeValueAsString(request);
            log.debug("Sending MCP request: {} - {}", method, requestJson);
            
            RequestBody body = RequestBody.create(requestJson, MediaType.get("application/json"));
            String endpoint = method.equals("initialize") ? "/mcp/initialize" : 
                            method.equals("tools/list") ? "/mcp/tools/list" :
                            method.equals("tools/call") ? "/mcp/tools/call" :
                            method.equals("ping") ? "/mcp/ping" :
                            "/mcp/" + method;
            
            Request httpRequest = new Request.Builder()
                .url(serverUrl + endpoint)
                .post(body)
                .header("Content-Type", "application/json")
                .build();
            
            CompletableFuture<McpResponse> future = new CompletableFuture<>();
            pendingRequests.put(requestId, future);
            
            httpClient.newCall(httpRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    log.error("HTTP request failed for method: {}", method, e);
                    pendingRequests.remove(requestId);
                    future.completeExceptionally(e);
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    pendingRequests.remove(requestId);
                    
                    try {
                        if (!response.isSuccessful()) {
                            future.completeExceptionally(new IOException("HTTP error: " + response.code()));
                            return;
                        }                        String responseJson = response.body().string();
                        log.debug("Received MCP response: {}", responseJson);
                        
                        McpResponse mcpResponse = objectMapper.readValue(responseJson, McpResponse.class);
                        future.complete(mcpResponse);
                        
                    } catch (Exception e) {
                        log.error("Error processing response", e);
                        future.completeExceptionally(e);
                    }
                }
            });
            
            return future;
            
        } catch (Exception e) {
            log.error("Error sending request", e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Connect to SSE stream for real-time updates
     */
    public void connectSSE(Consumer<String> messageHandler) {
        this.sseMessageHandler = messageHandler;
        
        String sseUrl = serverUrl + "/mcp/sse?clientId=" + clientName;
        Request request = new Request.Builder()
            .url(sseUrl)
            .header("Accept", "text/event-stream")
            .build();
        
        EventSourceListener listener = new EventSourceListener() {
            @Override
            public void onOpen(EventSource eventSource, Response response) {
                log.info("SSE connection opened to: {}", sseUrl);
            }
            
            @Override
            public void onEvent(EventSource eventSource, String id, String type, String data) {
                log.debug("SSE event received - ID: {}, Type: {}, Data: {}", id, type, data);
                if (sseMessageHandler != null) {
                    sseMessageHandler.accept(data);
                }
            }
            
            @Override
            public void onClosed(EventSource eventSource) {
                log.info("SSE connection closed");
            }
            
            @Override
            public void onFailure(EventSource eventSource, Throwable t, Response response) {
                log.error("SSE connection failed", t);
            }
        };
        
        this.sseEventSource = EventSources.createFactory(httpClient).newEventSource(request, listener);
    }
    
    /**
     * Connect to WebSocket for bidirectional communication
     */
    public void connectWebSocket(Consumer<String> messageHandler) {
        this.wsMessageHandler = messageHandler;
        
        String wsUrl = serverUrl.replace("http://", "ws://").replace("https://", "wss://") + "/mcp/ws";
        Request request = new Request.Builder()
            .url(wsUrl)
            .build();
        
        WebSocketListener listener = new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                log.info("WebSocket connection opened to: {}", wsUrl);
            }
            
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                log.debug("WebSocket message received: {}", text);
                if (wsMessageHandler != null) {
                    wsMessageHandler.accept(text);
                }
            }
            
            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                log.info("WebSocket connection closing: {} - {}", code, reason);
                webSocket.close(1000, null);
            }
            
            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                log.error("WebSocket connection failed", t);
            }
        };
        
        this.webSocket = httpClient.newWebSocket(request, listener);
    }
    
    /**
     * Send message via WebSocket
     */
    public void sendWebSocketMessage(String message) {
        if (webSocket != null) {
            webSocket.send(message);
        } else {
            log.warn("WebSocket not connected, cannot send message");
        }
    }
    
    /**
     * Disconnect all connections
     */
    public void disconnect() {
        if (sseEventSource != null) {
            sseEventSource.cancel();
            sseEventSource = null;
        }
        
        if (webSocket != null) {
            webSocket.close(1000, "Client disconnect");
            webSocket = null;
        }
        
        initialized = false;
        log.info("MCP client disconnected");    }
    
    // Getter methods for server information
    public String getServerUrl() {
        return serverUrl;
    }
    
    public String getClientName() {
        return clientName;
    }
    
    public String getClientVersion() {
        return clientVersion;
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    public Map<String, Object> getServerCapabilities() {
        return serverCapabilities;
    }
    
    /**
     * Map JSON to McpTool
     */
    private McpTool mapToTool(Map<String, Object> data) {
        return McpTool.builder()
            .name((String) data.get("name"))
            .description((String) data.get("description"))
            .inputSchema((Map<String, Object>) data.get("inputSchema"))
            .build();
    }
    
    /**
     * Map JSON to McpResource
     */
    private McpResource mapToResource(Map<String, Object> data) {
        return McpResource.builder()
            .uri((String) data.get("uri"))
            .name((String) data.get("name"))
            .description((String) data.get("description"))
            .mimeType((String) data.get("mimeType"))
            .annotations((Map<String, Object>) data.get("annotations"))
            .build();
    }
    
    /**
     * Map JSON to McpToolResult
     */
    private McpToolResult mapToToolResult(Map<String, Object> data) {
        List<Map<String, Object>> contentList = (List<Map<String, Object>>) data.get("content");
        Boolean isError = (Boolean) data.get("isError");
        
        List<McpToolResult.ContentItem> content = contentList != null ? 
            contentList.stream()
                .map(item -> McpToolResult.ContentItem.builder()
                    .type((String) item.get("type"))
                    .text((String) item.get("text"))
                    .data((String) item.get("data"))
                    .mimeType((String) item.get("mimeType"))
                    .annotations((Map<String, Object>) item.get("annotations"))
                    .build())
                .toList() : List.of();
        
        return McpToolResult.builder()
            .content(content)
            .isError(isError != null ? isError : false)
            .build();
    }
}
