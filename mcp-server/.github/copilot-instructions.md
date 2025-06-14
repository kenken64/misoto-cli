<!-- Use this file to provide workspace-specific custom instructions to Copilot. For more details, visit https://code.visualstudio.com/docs/copilot/copilot-customization#_use-a-githubcopilotinstructionsmd-file -->

# MCP Server Project Instructions

This is a **Model Context Protocol (MCP) Server** Spring Boot project. 

## Key Guidelines:

1. **MCP Protocol**: Implement JSON-RPC 2.0 based communication for MCP
2. **Server-Sent Events**: Use SSE for real-time streaming responses
3. **WebSocket Support**: Implement bidirectional communication channels
4. **Tool Management**: Create extensible tool registration and execution system
5. **Resource Management**: Implement resource discovery and access patterns

## Architecture:
- **Spring Boot 3.5.0** with Java 17
- **WebFlux** for reactive programming
- **WebSocket** for bidirectional communication
- **SSE** for streaming responses
- **JSON-RPC** for MCP protocol compliance

## Resources:
You can find more info and examples at https://modelcontextprotocol.io/llms-full.txt

## Code Style:
- Use **Lombok** for reducing boilerplate
- Follow **Spring Boot best practices**
- Implement **proper error handling** with MCP error codes
- Use **reactive patterns** where appropriate
- Create **comprehensive logging** for debugging MCP interactions
