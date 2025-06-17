package sg.edu.nus.iss.misoto.cli.codebase;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Code dependency information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DependencyInfo {
    
    /**
     * Module/package name
     */
    private String name;
    
    /**
     * Type of dependency (import, require, etc.)
     */
    private String type;
    
    /**
     * Source file path
     */
    private String source;
    
    /**
     * Import path
     */
    private String importPath;
    
    /**
     * Whether it's an external dependency
     */
    private boolean isExternal;
}
