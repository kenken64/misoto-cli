package sg.edu.nus.iss.misoto.cli.ai.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response from AI provider
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiResponse {
    
    private String text;
    private String model;
    private String provider;
    private AiUsage usage;
    private long responseTimeMs;
    private boolean success;
    private String errorMessage;
    
    public static AiResponse success(String text, String model, String provider, AiUsage usage, long responseTimeMs) {
        return AiResponse.builder()
            .text(text)
            .model(model)
            .provider(provider)
            .usage(usage)
            .responseTimeMs(responseTimeMs)
            .success(true)
            .build();
    }
    
    public static AiResponse error(String errorMessage, String provider) {
        return AiResponse.builder()
            .success(false)
            .errorMessage(errorMessage)
            .provider(provider)
            .build();
    }
}
