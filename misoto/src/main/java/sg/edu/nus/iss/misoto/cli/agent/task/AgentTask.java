package sg.edu.nus.iss.misoto.cli.agent.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;

/**
 * Represents a task in the agent system
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgentTask {
      @JsonProperty("id")
    @Builder.Default
    private String id = UUID.randomUUID().toString();
    
    @JsonProperty("type")
    private TaskType type;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("priority")
    @Builder.Default
    private TaskPriority priority = TaskPriority.MEDIUM;
    
    @JsonProperty("status")
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;
    
    @JsonProperty("created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    @JsonProperty("started_at")
    private Instant startedAt;
    
    @JsonProperty("completed_at")
    private Instant completedAt;
    
    @JsonProperty("timeout")
    @Builder.Default
    private Duration timeout = Duration.ofMinutes(30);
    
    @JsonProperty("retry_count")
    @Builder.Default
    private int retryCount = 0;
    
    @JsonProperty("max_retries")
    @Builder.Default
    private int maxRetries = 3;
    
    @JsonProperty("dependencies")
    @Builder.Default
    private List<String> dependencies = new ArrayList<>();
    
    @JsonProperty("parameters")
    @Builder.Default
    private Map<String, Object> parameters = new HashMap<>();
    
    @JsonProperty("context")
    @Builder.Default
    private TaskContext context = new TaskContext();
    
    @JsonProperty("result")
    private TaskResult result;
    
    @JsonProperty("error_message")
    private String errorMessage;
    
    @JsonProperty("execution_log")
    @Builder.Default
    private List<String> executionLog = new ArrayList<>();
    
    public enum TaskType {
        // File Operations
        FILE_READ,
        FILE_WRITE,
        FILE_COPY,
        FILE_DELETE,
        DIRECTORY_SCAN,
        
        // Command Execution
        SHELL_COMMAND,
        SCRIPT_EXECUTION,
        BACKGROUND_PROCESS,
        
        // AI Operations
        AI_ANALYSIS,
        CODE_GENERATION,
        DECISION_MAKING,
        TEXT_PROCESSING,
        
        // MCP Operations
        MCP_TOOL_CALL,
        MCP_RESOURCE_ACCESS,
        MCP_SERVER_MANAGEMENT,
          // System Operations
        SYSTEM_MONITORING,
        SYSTEM,
        LOG_ANALYSIS,
        HEALTH_CHECK,
        
        // Workflow Operations
        COMPOSITE_TASK,
        SCHEDULED_TASK,
        TRIGGERED_TASK,
        
        // Custom Operations
        CUSTOM_ACTION,
        USER_DEFINED
    }
      public enum TaskPriority {
        CRITICAL(1),
        HIGH(2),
        MEDIUM(3),
        LOW(4),
        BACKGROUND(5);
        
        private final int level;
        
        TaskPriority(int level) {
            this.level = level;
        }        
        public int getLevel() {
            return level;
        }
    }
    
    public enum TaskStatus {
        PENDING,
        QUEUED,
        RUNNING,
        PAUSED,
        COMPLETED,
        FAILED,
        CANCELLED,
        TIMEOUT,
        WAITING_FOR_DEPENDENCIES,
        WAITING_FOR_APPROVAL
    }
      @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TaskContext {
        @JsonProperty("working_directory")
        private String workingDirectory;
          @JsonProperty("environment_variables")
        @Builder.Default
        private Map<String, String> environmentVariables = new HashMap<>();
        
        @JsonProperty("user_id")
        private String userId;
        
        @JsonProperty("session_id")
        private String sessionId;
        
        @JsonProperty("trigger_source")
        private String triggerSource;
        
        @JsonProperty("parent_task_id")
        private String parentTaskId;
        
        @JsonProperty("metadata")
        @Builder.Default
        private Map<String, Object> metadata = new HashMap<>();
        
        // Convenience method for adding metadata
        public void put(String key, String value) {
            if (metadata == null) {
                metadata = new HashMap<>();
            }
            metadata.put(key, value);
        }
    }
      @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TaskResult {
        @JsonProperty("success")
        private boolean success;
        
        @JsonProperty("output")
        private String output;
        
        @JsonProperty("error")
        private String error;
        
        @JsonProperty("exit_code")
        private Integer exitCode;
        
        @JsonProperty("execution_time_ms")
        private long executionTimeMs;
          @JsonProperty("files_created")
        @Builder.Default
        private List<String> filesCreated = List.of();
        
        @JsonProperty("files_modified")
        @Builder.Default
        private List<String> filesModified = List.of();
        
        @JsonProperty("commands_executed")
        @Builder.Default
        private List<String> commandsExecuted = List.of();
        
        @JsonProperty("artifacts")
        @Builder.Default
        private Map<String, Object> artifacts = Map.of();
        
        @JsonProperty("metrics")
        @Builder.Default
        private Map<String, Object> metrics = Map.of();
        
        public void setError(String error) {
            this.error = error;
        }
        
        public String getError() {
            return error;
        }
    }
    
    /**
     * Check if task is completed (either successfully or failed)
     */
    public boolean isCompleted() {
        return status == TaskStatus.COMPLETED || 
               status == TaskStatus.FAILED || 
               status == TaskStatus.CANCELLED || 
               status == TaskStatus.TIMEOUT;
    }
    
    /**
     * Check if task can be executed (all dependencies met)
     */
    public boolean canExecute() {
        return status == TaskStatus.PENDING || status == TaskStatus.QUEUED;
    }
    
    /**
     * Check if task should be retried
     */
    public boolean shouldRetry() {
        return status == TaskStatus.FAILED && retryCount < maxRetries;
    }
    
    /**
     * Mark task as started
     */
    public void markStarted() {
        this.status = TaskStatus.RUNNING;
        this.startedAt = Instant.now();
    }
    
    /**
     * Mark task as completed with result
     */
    public void markCompleted(TaskResult result) {
        this.status = TaskStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.result = result;
    }
    
    /**
     * Mark task as failed with error
     */
    public void markFailed(String errorMessage) {
        this.status = TaskStatus.FAILED;
        this.completedAt = Instant.now();
        this.errorMessage = errorMessage;
        this.retryCount++;
    }
      /**
     * Add log entry
     */
    public void addLogEntry(String entry) {
        List<String> newLog = new ArrayList<>(this.executionLog);
        newLog.add("[" + Instant.now() + "] " + entry);
        this.executionLog = newLog;
    }
    
    // Additional convenience methods
    public void setErrorMessage(String errorMessage) {
        if (result == null) {
            result = new TaskResult();
        }
        result.setError(errorMessage);
    }
    
    public String getErrorMessage() {
        return result != null ? result.getError() : null;
    }
      public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
