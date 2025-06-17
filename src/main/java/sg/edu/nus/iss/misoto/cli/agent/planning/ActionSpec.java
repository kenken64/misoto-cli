package sg.edu.nus.iss.misoto.cli.agent.planning;

import lombok.Builder;
import lombok.Data;
import sg.edu.nus.iss.misoto.cli.agent.task.AgentTask;

import java.util.Map;

/**
 * Specification for an action to be executed
 */
@Data
@Builder
public class ActionSpec {
    private AgentTask.TaskType type;
    private String description;
    private Map<String, Object> parameters;
    private String expectedOutcome;
}