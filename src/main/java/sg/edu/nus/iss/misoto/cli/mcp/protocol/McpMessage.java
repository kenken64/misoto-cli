package sg.edu.nus.iss.misoto.cli.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
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
