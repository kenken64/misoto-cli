package sg.edu.nus.iss.misoto.cli.agent.task;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Statistics about the task queue
 */
@Data
@Builder
public class TaskQueueStats {
    private int totalTasks;
    private int queuedTasks;
    private int runningTasks;
    private int completedTasks;
    private int failedTasks;
    private int pendingTasks;
    private Map<AgentTask.TaskStatus, Long> statusCounts;
    
    /**
     * Get the number of pending tasks (alias for queuedTasks)
     */
    public int getPendingTasks() {
        return pendingTasks > 0 ? pendingTasks : queuedTasks;
    }
}
