package sg.edu.nus.iss.misoto.cli.fileops;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Options for write operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WriteOptions {
    
    @Builder.Default
    private Charset encoding = StandardCharsets.UTF_8;
    
    @Builder.Default
    private boolean createDir = true;
    
    @Builder.Default
    private boolean overwrite = true;
}
