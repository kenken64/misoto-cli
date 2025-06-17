package sg.edu.nus.iss.misoto.cli.codebase;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Project structure information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectStructure {
    
    /**
     * Root directory
     */
    private String root;
    
    /**
     * Total file count
     */
    private int totalFiles;
    
    /**
     * Files by language
     */
    private Map<String, Integer> filesByLanguage = new HashMap<>();
    
    /**
     * Total lines of code
     */
    private long totalLinesOfCode;
    
    /**
     * Files organized by directory
     */
    private Map<String, List<String>> directories = new HashMap<>();
    
    /**
     * Dependencies identified in the project
     */
    private List<DependencyInfo> dependencies;
}
