package sg.edu.nus.iss.misoto.cli.ai.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Usage information from AI response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiUsage {
    
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private Double estimatedCost;
    private String currency;
    
    public static AiUsage of(Integer inputTokens, Integer outputTokens, Double estimatedCost) {
        return AiUsage.builder()
            .inputTokens(inputTokens)
            .outputTokens(outputTokens)
            .totalTokens((inputTokens != null && outputTokens != null) ? inputTokens + outputTokens : null)
            .estimatedCost(estimatedCost)
            .currency("USD")
            .build();
    }
}
