package sg.edu.nus.iss.misoto.cli.fileops;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Options for copy operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CopyOptions {
    
    @Builder.Default
    private boolean overwrite = false;
    
    @Builder.Default
    private boolean createDir = true;
}
