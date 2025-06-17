package sg.edu.nus.iss.misoto.cli.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * MCP Response message following JSON-RPC 2.0 specification
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class McpResponse extends McpMessage {
    
    @JsonProperty("id")
    private Object id;
    
    @JsonProperty("result")
    private Object result;
    
    @JsonProperty("error")
    private McpError error;
    
    public McpResponse(Object id, Object result) {
        this.id = id;
        this.result = result;
    }
    
    public McpResponse(Object id, McpError error) {
        this.id = id;
        this.error = error;
    }
    
    @Override
    public boolean isRequest() {
        return false;
    }
    
    @Override
    public boolean isResponse() {
        return true;
    }
    
    @Override
    public boolean isNotification() {
        return false;
    }
    
    /**
     * Check if this response contains an error
     */
    public boolean hasError() {
        return error != null;
    }
}
