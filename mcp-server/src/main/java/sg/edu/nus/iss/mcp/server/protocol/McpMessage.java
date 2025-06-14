package sg.edu.nus.iss.mcp.server.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Base class for all MCP (Model Context Protocol) messages
 * Following the JSON-RPC 2.0 specification
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "method", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = McpRequest.class, name = "initialize"),
    @JsonSubTypes.Type(value = McpRequest.class, name = "tools/list"),
    @JsonSubTypes.Type(value = McpRequest.class, name = "tools/call"),
    @JsonSubTypes.Type(value = McpRequest.class, name = "resources/list"),
    @JsonSubTypes.Type(value = McpRequest.class, name = "resources/read"),
    @JsonSubTypes.Type(value = McpNotification.class, name = "initialized"),
    @JsonSubTypes.Type(value = McpNotification.class, name = "notifications/tools/list_changed"),
    @JsonSubTypes.Type(value = McpNotification.class, name = "notifications/resources/list_changed")
})
public abstract class McpMessage {
    
    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";
    
    /**
     * Check if this is a request message
     */
    public abstract boolean isRequest();
    
    /**
     * Check if this is a response message
     */
    public abstract boolean isResponse();
    
    /**
     * Check if this is a notification message
     */
    public abstract boolean isNotification();
}
