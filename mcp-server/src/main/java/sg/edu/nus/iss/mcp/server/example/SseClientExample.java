package sg.edu.nus.iss.mcp.server.example;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Example SSE client to demonstrate MCP server integration
 */
@Component
@Slf4j
public class SseClientExample {
    
    private final OkHttpClient client;
    
    public SseClientExample() {
        this.client = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(30))
            .readTimeout(Duration.ofSeconds(30))
            .build();
    }
    
    /**
     * Connect to MCP server SSE endpoint
     */
    public void connectToMcpServer(String serverUrl) {
        Request request = new Request.Builder()
            .url(serverUrl + "/mcp/sse")
            .addHeader("Accept", "text/event-stream")
            .addHeader("Cache-Control", "no-cache")
            .build();
        
        EventSourceListener listener = new EventSourceListener() {
            @Override
            public void onOpen(EventSource eventSource, Response response) {
                log.info("SSE connection opened to MCP server");
            }
            
            @Override
            public void onEvent(EventSource eventSource, String id, String type, String data) {
                log.info("Received SSE event - ID: {}, Type: {}, Data: {}", id, type, data);
                
                // Handle different MCP message types
                switch (type) {
                    case "mcp-response":
                        handleMcpResponse(data);
                        break;
                    case "mcp-notification":
                        handleMcpNotification(data);
                        break;
                    case "tool-result":
                        handleToolResult(data);
                        break;
                    default:
                        log.debug("Unknown message type: {}", type);
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
        
        EventSource eventSource = EventSources.createFactory(client)
            .newEventSource(request, listener);
        
        // Keep connection alive for demo purposes
        try {
            Thread.sleep(30000); // 30 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            eventSource.cancel();
            log.info("SSE client disconnected");
        }
    }
    
    private void handleMcpResponse(String data) {
        log.info("Handling MCP response: {}", data);
        // Parse and handle MCP response
    }
    
    private void handleMcpNotification(String data) {
        log.info("Handling MCP notification: {}", data);
        // Parse and handle MCP notification
    }
    
    private void handleToolResult(String data) {
        log.info("Handling tool result: {}", data);
        // Parse and handle tool execution result
    }
}
