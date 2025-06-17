package sg.edu.nus.iss.misoto.cli.codebase;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * File search result
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileSearchResult {
    
    /**
     * File path relative to search directory
     */
    private String path;
    
    /**
     * Line number (1-indexed)
     */
    private int line;
    
    /**
     * Content of the matching line
     */
    private String content;
}
