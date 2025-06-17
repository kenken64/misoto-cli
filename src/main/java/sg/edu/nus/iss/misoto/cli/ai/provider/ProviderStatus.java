package sg.edu.nus.iss.misoto.cli.ai.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Status information for an AI provider
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderStatus {
    
    private String name;
    private String displayName;
    private boolean available;
    private boolean current;
    private String currentModel;
    private ProviderCapabilities capabilities;
    
}
