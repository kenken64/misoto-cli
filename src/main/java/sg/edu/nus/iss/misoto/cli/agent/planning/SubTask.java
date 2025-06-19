package sg.edu.nus.iss.misoto.cli.agent.planning;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Represents a decomposed subtask within an execution plan
 */
@Data
@Builder
public class SubTask {
    private String id;
    private String name;
    private String description;
    private String expectedOutcome;
    private Priority priority;
    private Complexity complexity;
    private List<String> dependencies;
    private Status status;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
    private String result;
    private String errorMessage;
    
    // Enhanced fields for executable content
    private List<String> commands;        // Shell commands to execute
    private String codeLanguage;         // Programming language
    private String codeContent;          // Code snippet content  
    private String filePath;             // File to create/modify
    private String fileContent;          // Content to write to file
    
    // File context preservation fields
    private String originalFileContent;  // Existing file content before modification
    private boolean fileExists;         // Whether the target file already exists
    private boolean preserveContext;     // Whether to preserve existing content when modifying
    private FileOperationMode operationMode; // How to handle the file operation
    
    public enum Priority {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW
    }
    
    public enum Complexity {
        SIMPLE,
        MODERATE,
        COMPLEX
    }
    
    public enum Status {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        BLOCKED
    }
    
    public enum FileOperationMode {
        CREATE,      // Create new file (fail if exists)
        REPLACE,     // Replace entire file content
        MODIFY,      // Modify existing file content intelligently
        APPEND,      // Append to existing file
        AUTO         // Let the system decide based on context
    }
}