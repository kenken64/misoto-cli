package sg.edu.nus.iss.misoto.cli.agent.planning;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Represents a comprehensive execution plan with subtasks and strategy
 */
@Data
@Builder
public class ExecutionPlan {
    private String id;
    private String goal;
    private List<SubTask> subTasks;
    private PlanningStrategy strategy;
    private Map<String, Object> context;
    private PlanStatus status;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
    
    public enum PlanStatus {
        CREATED,
        EXECUTING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}