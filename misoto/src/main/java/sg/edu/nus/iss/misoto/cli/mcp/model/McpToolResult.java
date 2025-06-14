package sg.edu.nus.iss.misoto.cli.mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * MCP Tool call result
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpToolResult {
    
    @JsonProperty("content")
    private List<ContentItem> content;
    
    @JsonProperty("isError")
    private Boolean isError;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ContentItem {
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("text")
        private String text;
        
        @JsonProperty("data")
        private String data;
        
        @JsonProperty("mimeType")
        private String mimeType;
        
        @JsonProperty("annotations")
        private Map<String, Object> annotations;
        
        public static ContentItem text(String text) {
            return ContentItem.builder()
                .type("text")
                .text(text)
                .build();
        }
        
        public static ContentItem json(String data) {
            return ContentItem.builder()
                .type("text")
                .text(data)
                .mimeType("application/json")
                .build();
        }
        
        public static ContentItem error(String errorMessage) {
            return ContentItem.builder()
                .type("text")
                .text(errorMessage)
                .mimeType("text/plain")
                .build();
        }
    }
    
    public static McpToolResult success(String text) {
        return McpToolResult.builder()
            .content(List.of(ContentItem.text(text)))
            .isError(false)
            .build();
    }
    
    public static McpToolResult success(List<ContentItem> content) {
        return McpToolResult.builder()
            .content(content)
            .isError(false)
            .build();
    }
    
    public static McpToolResult error(String errorMessage) {
        return McpToolResult.builder()
            .content(List.of(ContentItem.error(errorMessage)))
            .isError(true)
            .build();
    }
}
