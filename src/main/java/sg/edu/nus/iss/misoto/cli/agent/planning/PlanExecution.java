package sg.edu.nus.iss.misoto.cli.agent.planning;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Represents the execution state of a plan
 */
@Data
@Builder
public class PlanExecution {
    private String planId;
    private ExecutionStatus status;
    private Instant startedAt;
    private Instant completedAt;
    private List<ExecutionStep> steps;
    private Map<String, Object> workingMemory; // ReAct working memory
    private Map<String, Object> episodicMemory; // Long-term interaction memory
    private String currentReasoning;
    private int currentStepIndex;
    private String failureReason;
    
    public enum ExecutionStatus {
        RUNNING,
        COMPLETED,
        FAILED,
        PAUSED,
        CANCELLED
    }
}