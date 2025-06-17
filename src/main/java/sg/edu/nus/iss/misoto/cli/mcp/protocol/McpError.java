package sg.edu.nus.iss.misoto.cli.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * MCP Error object following JSON-RPC 2.0 specification
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpError {
    
    @JsonProperty("code")
    private int code;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("data")
    private Object data;
    
    public McpError(int code, String message) {
        this.code = code;
        this.message = message;
    }
    
    // Standard JSON-RPC error codes
    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;
    
    // MCP-specific error codes
    public static final int INITIALIZATION_FAILED = -32000;
    public static final int TOOL_NOT_FOUND = -32001;
    public static final int RESOURCE_NOT_FOUND = -32002;
    public static final int PERMISSION_DENIED = -32003;
    
    public static McpError parseError() {
        return new McpError(PARSE_ERROR, "Parse error");
    }
    
    public static McpError invalidRequest() {
        return new McpError(INVALID_REQUEST, "Invalid Request");
    }
    
    public static McpError methodNotFound() {
        return new McpError(METHOD_NOT_FOUND, "Method not found");
    }
    
    public static McpError invalidParams() {
        return new McpError(INVALID_PARAMS, "Invalid params");
    }
    
    public static McpError internalError() {
        return new McpError(INTERNAL_ERROR, "Internal error");
    }
    
    public static McpError toolNotFound(String toolName) {
        return new McpError(TOOL_NOT_FOUND, "Tool not found: " + toolName);
    }
    
    public static McpError resourceNotFound(String resourceUri) {
        return new McpError(RESOURCE_NOT_FOUND, "Resource not found: " + resourceUri);
    }
    
    public static McpError permissionDenied() {
        return new McpError(PERMISSION_DENIED, "Permission denied");
    }
}
