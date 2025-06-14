package sg.edu.nus.iss.misoto.cli.codebase;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Options for file search operations
 */
@Data
public class FileSearchOptions {
    
    /**
     * Whether the search is case sensitive
     */
    private boolean caseSensitive = false;
    
    /**
     * File extensions to include in search (empty means all files)
     */
    private List<String> fileExtensions = new ArrayList<>();
    
    /**
     * Maximum number of results to return
     */
    private int maxResults = 100;
    
    /**
     * Patterns to ignore during search
     */
    private List<String> ignorePatterns = new ArrayList<>();
    
    /**
     * Whether to include hidden files/directories
     */
    private boolean includeHidden = false;
    
    /**
     * Maximum file size to search in bytes
     */
    private long maxFileSize = 1024 * 1024; // 1MB
}
