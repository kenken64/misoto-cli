package sg.edu.nus.iss.misoto.cli.mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Map;

/**
 * MCP Tool call request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpToolCall {
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("arguments")
    private Map<String, Object> arguments;
    
    public static McpToolCall create(String name) {
        return McpToolCall.builder()
            .name(name)
            .arguments(Map.of())
            .build();
    }
    
    public static McpToolCall create(String name, Map<String, Object> arguments) {
        return McpToolCall.builder()
            .name(name)
            .arguments(arguments != null ? arguments : Map.of())
            .build();
    }
}
