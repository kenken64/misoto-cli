package sg.edu.nus.iss.misoto.cli.agent.planning;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Represents a single ReAct cycle step in plan execution
 */
@Data
@Builder
public class ExecutionStep {
    private String subTaskId;
    private String reasoning; // Thought process
    private String action; // Action taken
    private String observation; // Results observed
    private StepStatus status;
    private Instant startedAt;
    private Instant completedAt;
    private String errorMessage;
    private String taskId; // Associated agent task ID
    
    public enum StepStatus {
        RUNNING,
        COMPLETED,
        FAILED,
        SKIPPED
    }
}