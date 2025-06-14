package sg.edu.nus.iss.mcp.server.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Map;

/**
 * MCP Tool definition
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpTool {
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("inputSchema")
    private Map<String, Object> inputSchema;
    
    /**
     * Create a simple tool with basic schema
     */
    public static McpTool createSimple(String name, String description) {
        return McpTool.builder()
            .name(name)
            .description(description)
            .inputSchema(Map.of(
                "type", "object",
                "properties", Map.of(),
                "required", new String[0]
            ))
            .build();
    }
    
    /**
     * Create a tool with parameters
     */
    public static McpTool createWithParams(String name, String description, Map<String, Object> properties, String[] required) {
        return McpTool.builder()
            .name(name)
            .description(description)
            .inputSchema(Map.of(
                "type", "object",
                "properties", properties != null ? properties : Map.of(),
                "required", required != null ? required : new String[0]
            ))
            .build();
    }
}
