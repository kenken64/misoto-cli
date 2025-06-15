package sg.edu.nus.iss.misoto.cli.agent.state;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Represents a snapshot of agent state for persistence
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgentStateSnapshot {
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("state")
    private Map<String, Object> state;
    
    @JsonProperty("recent_history")
    private List<AgentStateEntry> recentHistory;
    
    @JsonProperty("version")
    @Builder.Default
    private String version = "1.0";
    
    @JsonProperty("total_tasks_executed")
    @Builder.Default
    private long totalTasksExecuted = 0;
    
    @JsonProperty("failed_tasks")
    @Builder.Default
    private long failedTasks = 0;
    
    @JsonProperty("last_activity")
    private Instant lastActivity;
    
    // Convenience methods for accessing state values
    public Object getValue(String key) {
        return state != null ? state.get(key) : null;
    }
    
    public void setValue(String key, Object value) {
        if (state != null) {
            state.put(key, value);
        }
    }
    
    // Getters for statistical data
    public long getTotalTasksExecuted() {
        return totalTasksExecuted;
    }
    
    public long getFailedTasks() {
        return failedTasks;
    }
    
    public Instant getLastActivity() {
        return lastActivity;
    }
}

/**
 * Represents a single entry in the agent state history
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
class AgentStateEntry {
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("change_type")
    private StateChangeType changeType;
    
    @JsonProperty("key")
    private String key;
    
    @JsonProperty("old_value")
    private Object oldValue;
    
    @JsonProperty("new_value")
    private Object newValue;
    
    public enum StateChangeType {
        STATE_UPDATE,
        STATE_REMOVED,
        STATE_CLEARED,
        MEMORY_UPDATE,
        SYSTEM_EVENT
    }
}
