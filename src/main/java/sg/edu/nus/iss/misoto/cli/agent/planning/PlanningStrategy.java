package sg.edu.nus.iss.misoto.cli.agent.planning;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Represents the execution strategy for a plan
 */
@Data
@Builder
public class PlanningStrategy {
    private String description;
    private List<SubTask> executionOrder;
    private List<List<String>> parallelGroups; // Groups of subtask IDs that can run in parallel
    private Map<String, String> riskMitigation; // Subtask ID -> mitigation strategy
    private Map<String, String> toolSelection; // Subtask ID -> preferred tool
    private List<String> checkpoints; // Key milestones to monitor
}