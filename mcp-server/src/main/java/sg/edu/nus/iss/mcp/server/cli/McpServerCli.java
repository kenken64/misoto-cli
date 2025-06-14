package sg.edu.nus.iss.mcp.server.cli;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import sg.edu.nus.iss.mcp.server.example.SseClientExample;

/**
 * CLI runner for demonstration purposes
 */
@Component
@Slf4j
public class McpServerCli implements CommandLineRunner {
    
    @Autowired
    private SseClientExample sseClient;
    
    @Override
    public void run(String... args) throws Exception {
        log.info("MCP Server started successfully!");
        log.info("Available endpoints:");
        log.info("  REST API: http://localhost:8080/mcp/");
        log.info("  SSE Stream: http://localhost:8080/mcp/sse");
        log.info("  WebSocket: ws://localhost:8080/mcp/ws");
        log.info("  Health Check: http://localhost:8080/actuator/health");
        
        // Check if we should run in demo mode
        boolean demoMode = args.length > 0 && "demo".equals(args[0]);
        
        if (demoMode) {
            log.info("Running in demo mode - will test SSE client");
            // Wait a bit for server to fully start
            Thread.sleep(2000);
            
            // Test SSE client connection
            try {
                sseClient.connectToMcpServer("http://localhost:8080");
            } catch (Exception e) {
                log.error("Demo mode failed", e);
            }
        } else {
            log.info("Server is running. Use 'java -jar mcp-server.jar demo' to run demo mode.");
            log.info("Press Ctrl+C to stop the server.");
        }
    }
}
