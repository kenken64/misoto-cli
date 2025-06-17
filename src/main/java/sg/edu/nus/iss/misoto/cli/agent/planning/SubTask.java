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
}