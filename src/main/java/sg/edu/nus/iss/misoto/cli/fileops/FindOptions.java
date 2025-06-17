package sg.edu.nus.iss.misoto.cli.fileops;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Options for find operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindOptions {
    
    @Builder.Default
    private boolean recursive = true;
    
    @Builder.Default
    private boolean includeDirectories = false;
}
