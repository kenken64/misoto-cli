package sg.edu.nus.iss.misoto.cli.agent.planning;

import lombok.Builder;
import lombok.Data;
import sg.edu.nus.iss.misoto.cli.agent.task.AgentTask;

import java.util.Map;

/**
 * Result of executing an action in the ReAct cycle
 */
@Data
@Builder
public class ActionResult {
    private String actionDescription;
    private String taskId;
    private boolean success;
    private AgentTask.TaskResult result;
    private Map<String, Object> memoryUpdates;
    private String errorMessage;
}