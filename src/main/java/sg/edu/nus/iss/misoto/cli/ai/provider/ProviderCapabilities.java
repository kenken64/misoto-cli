package sg.edu.nus.iss.misoto.cli.ai.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Provider capabilities information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderCapabilities {
    
    private boolean supportsChat;
    private boolean supportsStreaming;
    private boolean supportsFunctionCalling;
    private boolean supportsSystemPrompts;
    private boolean supportsHistory;
    private boolean supportsImageInput;
    private boolean supportsFileInput;
    private Integer maxTokens;
    private Integer maxHistoryMessages;
    
}
