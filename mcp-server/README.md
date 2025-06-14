# MCP Server - Model Context Protocol Server

A Spring Boot implementation of the Model Context Protocol (MCP) server providing tool execution, resource management, and real-time communication via Server-Sent Events (SSE) and WebSocket.

## Features

- **JSON-RPC 2.0 Protocol**: Full MCP protocol compliance
- **Server-Sent Events (SSE)**: Real-time streaming to clients
- **WebSocket Support**: Bidirectional communication
- **Tool Management**: Extensible tool registration and execution
- **Resource Management**: Resource discovery and access
- **Health Monitoring**: Built-in health checks and metrics

## Architecture

- **Spring Boot 3.5.0** with Java 17
- **WebFlux** for reactive programming
- **WebSocket** for bidirectional communication
- **SSE** for streaming responses
- **JSON-RPC** for MCP protocol compliance

## API Endpoints

### MCP Protocol Endpoints

- `POST /mcp/initialize` - Initialize MCP connection
- `POST /mcp/tools/list` - List available tools
- `POST /mcp/tools/call` - Execute a tool
- `POST /mcp/ping` - Health check ping

### Streaming Endpoints

- `GET /mcp/stream` - Create SSE connection
- `POST /mcp/stream/{clientId}/send` - Send message to specific client
- `POST /mcp/stream/broadcast` - Broadcast to all clients

### WebSocket Endpoints

- `ws://localhost:8080/mcp/ws` - WebSocket connection for MCP protocol

### Monitoring Endpoints

- `GET /mcp/status` - Server status and statistics
- `GET /mcp/health` - Health check
- `GET /actuator/health` - Spring Boot health endpoint

## Built-in Tools

The server comes with several example tools:

1. **echo** - Echo back provided text
2. **current_time** - Get current date and time
3. **calculate** - Perform basic mathematical calculations
4. **system_info** - Get basic system information

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6+

### Running the Server

```bash
# Clone and navigate to the project
cd mcp-server

# Run with Maven
mvn spring-boot:run

# Or build and run JAR
mvn clean package
java -jar target/mcp-server-1.0.0-SNAPSHOT.jar
```

The server will start on `http://localhost:8080`

### Testing the Server

#### Test with curl

```bash
# Health check
curl http://localhost:8080/mcp/health

# Initialize MCP connection
curl -X POST http://localhost:8080/mcp/initialize \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"clientInfo":{"name":"test-client","version":"1.0.0"}}}'

# List available tools
curl -X POST http://localhost:8080/mcp/tools/list \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/list"}'

# Execute a tool
curl -X POST http://localhost:8080/mcp/tools/call \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"echo","arguments":{"text":"Hello MCP!"}}}'
```

#### Test SSE Connection

```bash
# Connect to SSE stream
curl -N http://localhost:8080/mcp/stream?clientId=test-client

# Send message to client (in another terminal)
curl -X POST http://localhost:8080/mcp/stream/test-client/send \
  -H "Content-Type: application/json" \
  -d '{"event":"test","data":{"message":"Hello via SSE!"}}'
```

#### Test WebSocket Connection

Use a WebSocket client or browser console:

```javascript
const ws = new WebSocket('ws://localhost:8080/mcp/ws');

ws.onopen = () => {
    console.log('Connected to MCP Server');
    
    // Send initialize request
    ws.send(JSON.stringify({
        "jsonrpc": "2.0",
        "id": 1,
        "method": "initialize",
        "params": {
            "clientInfo": {
                "name": "browser-client",
                "version": "1.0.0"
            }
        }
    }));
};

ws.onmessage = (event) => {
    console.log('Received:', JSON.parse(event.data));
};
```

## Development

### Project Structure

```
src/main/java/sg/edu/nus/iss/mcp/server/
├── McpServerApplication.java          # Main application
├── controller/
│   └── McpController.java            # REST endpoints
├── config/
│   ├── WebSocketConfig.java          # WebSocket configuration
│   └── McpWebSocketHandler.java      # WebSocket message handler
├── service/
│   ├── SseService.java               # Server-Sent Events service
│   └── ToolService.java              # Tool management service
├── protocol/
│   ├── McpMessage.java               # Base message class
│   ├── McpRequest.java               # Request message
│   ├── McpResponse.java              # Response message
│   ├── McpNotification.java          # Notification message
│   └── McpError.java                 # Error definitions
└── model/
    ├── McpTool.java                  # Tool definition
    ├── McpToolResult.java            # Tool execution result
    └── McpResource.java              # Resource definition
```

### Adding Custom Tools

```java
@Component
public class CustomToolProvider {
    
    @Autowired
    private ToolService toolService;
    
    @PostConstruct
    public void registerCustomTools() {
        toolService.registerTool(
            McpTool.createWithParams(
                "my_tool",
                "Description of my tool",
                Map.of("param1", Map.of("type", "string", "description", "Parameter description")),
                new String[]{"param1"}
            ),
            args -> {
                // Tool implementation
                String param1 = (String) args.get("param1");
                return McpToolResult.success("Result: " + param1);
            }
        );
    }
}
```

## Configuration

Key configuration properties in `application.properties`:

```properties
# Server port
server.port=8080

# Logging levels
logging.level.sg.edu.nus.iss.mcp.server=INFO

# WebSocket heartbeat
spring.websocket.heartbeat.interval=30000
spring.websocket.heartbeat.timeout=300000
```

## Monitoring

- Server status: `GET /mcp/status`
- Health check: `GET /mcp/health`
- Metrics: `GET /actuator/metrics`
- Active connections and tool statistics available via status endpoint

## License

This project is part of the Claude Code CLI ecosystem and follows the same licensing terms.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## Resources

- [Model Context Protocol Specification](https://modelcontextprotocol.io/llms-full.txt)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [WebSocket Support](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#websocket)
