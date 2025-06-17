package sg.edu.nus.iss.misoto.cli.mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Map;

/**
 * MCP Resource definition
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpResource {
    
    @JsonProperty("uri")
    private String uri;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("mimeType")
    private String mimeType;
    
    @JsonProperty("annotations")
    private Map<String, Object> annotations;
    
    /**
     * Create a simple text resource
     */
    public static McpResource createText(String uri, String name, String description) {
        return McpResource.builder()
            .uri(uri)
            .name(name)
            .description(description)
            .mimeType("text/plain")
            .build();
    }
    
    /**
     * Create a JSON resource
     */
    public static McpResource createJson(String uri, String name, String description) {
        return McpResource.builder()
            .uri(uri)
            .name(name)
            .description(description)
            .mimeType("application/json")
            .build();
    }
    
    /**
     * Create a markdown resource
     */
    public static McpResource createMarkdown(String uri, String name, String description) {
        return McpResource.builder()
            .uri(uri)
            .name(name)
            .description(description)
            .mimeType("text/markdown")
            .build();
    }
}
