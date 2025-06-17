package sg.edu.nus.iss.misoto.cli.codebase;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * File info with language detection and stats
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo {
    
    /**
     * File path relative to project root
     */
    private String path;
    
    /**
     * File extension
     */
    private String extension;
    
    /**
     * Detected language
     */
    private String language;
    
    /**
     * File size in bytes
     */
    private long size;
    
    /**
     * Line count
     */
    private int lineCount;
    
    /**
     * Last modified timestamp
     */
    private LocalDateTime lastModified;
}
