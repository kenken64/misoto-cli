package sg.edu.nus.iss.misoto.cli.agent.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Configuration for the Agent System
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentConfiguration {
      @JsonProperty("enabled")
    @Builder.Default
    private boolean enabled = false;
    
    @JsonProperty("agent_name")
    @Builder.Default
    private String agentName = "misoto-agent";
    
    @JsonProperty("mode")
    @Builder.Default
    private AgentMode mode = AgentMode.INTERACTIVE;
    
    @JsonProperty("auto_start")
    @Builder.Default
    private boolean autoStart = false;
    
    @JsonProperty("max_concurrent_tasks")
    @Builder.Default
    private int maxConcurrentTasks = 3;
    
    @JsonProperty("task_timeout")
    @Builder.Default
    private Duration taskTimeout = Duration.ofMinutes(30);
    
    @JsonProperty("monitoring_interval")
    @Builder.Default
    private Duration monitoringInterval = Duration.ofSeconds(30);
    
    @JsonProperty("shutdown_timeout")
    @Builder.Default
    private Duration shutdownTimeout = Duration.ofSeconds(5);
    
    @JsonProperty("monitoring_shutdown_timeout")
    @Builder.Default
    private Duration monitoringShutdownTimeout = Duration.ofSeconds(3);
    
    @JsonProperty("decision_model")
    @Builder.Default
    private String decisionModel = "claude-3-haiku-20240307";
    
    @JsonProperty("decision_temperature")
    @Builder.Default
    private double decisionTemperature = 0.3;
    
    @JsonProperty("state_persistence")
    @Builder.Default
    private StatePersistence statePersistence = new StatePersistence();
    
    @JsonProperty("monitoring")
    @Builder.Default
    private MonitoringConfig monitoringConfig = new MonitoringConfig();
    
    @JsonProperty("triggers")
    @Builder.Default
    private List<TriggerConfig> triggers = List.of();
    
    @JsonProperty("monitoring_triggers")
    @Builder.Default
    private List<MonitoringTrigger> monitoringTriggers = List.of();
    
    @JsonProperty("capabilities")
    @Builder.Default
    private Map<String, Boolean> capabilities = Map.of(
        "file_operations", true,
        "command_execution", true,
        "mcp_tools", true,
        "web_access", false,
        "code_generation", true,
        "system_monitoring", true
    );
    
    public enum AgentMode {
        INTERACTIVE,    // Agent responds to user inputs and asks for confirmation
        AUTONOMOUS,     // Agent operates independently with minimal user interaction
        SUPERVISED,     // Agent operates autonomously but reports major decisions
        MANUAL         // Agent only acts when explicitly commanded
    }
      @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatePersistence {
        @JsonProperty("enabled")
        @Builder.Default
        private boolean enabled = true;
        
        @JsonProperty("file_path")
        @Builder.Default
        private String filePath = ".misoto/agent-state.json";
        
        @JsonProperty("backup_interval")
        @Builder.Default
        private Duration backupInterval = Duration.ofMinutes(5);
        
        @JsonProperty("max_history_entries")
        @Builder.Default
        private int maxHistoryEntries = 1000;
        
        @JsonProperty("compress_old_data")
        @Builder.Default
        private boolean compressOldData = true;
    }
      @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonitoringConfig {
        @JsonProperty("file_system")
        @Builder.Default
        private boolean fileSystemMonitoring = true;
        
        @JsonProperty("process_monitoring")
        @Builder.Default
        private boolean processMonitoring = true;
        
        @JsonProperty("network_monitoring")
        @Builder.Default
        private boolean networkMonitoring = false;
        
        @JsonProperty("log_monitoring")
        @Builder.Default
        private boolean logMonitoring = true;
        
        @JsonProperty("watched_directories")
        @Builder.Default
        private List<String> watchedDirectories = List.of(".", "src", "target");
        
        @JsonProperty("watched_file_patterns")
        @Builder.Default
        private List<String> watchedFilePatterns = List.of("*.java", "*.properties", "*.yml", "*.json");
        
        @JsonProperty("ignored_patterns")
        @Builder.Default
        private List<String> ignoredPatterns = List.of("target/**", ".git/**", "*.class", "*.jar");
    }
      @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TriggerConfig {
        @JsonProperty("type")
        private TriggerType type;
        
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("description")
        private String description;
        
        @JsonProperty("enabled")
        @Builder.Default
        private boolean enabled = true;
        
        @JsonProperty("conditions")
        private Map<String, Object> conditions;
        
        @JsonProperty("actions")
        private List<String> actions;
        
        @JsonProperty("cooldown")
        @Builder.Default
        private Duration cooldown = Duration.ofMinutes(1);
          public enum TriggerType {
            FILE_CHANGE,
            PROCESS_EVENT,
            TIME_BASED,
            ERROR_DETECTED,
            THRESHOLD_EXCEEDED,
            EXTERNAL_API,
            USER_DEFINED
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonitoringTrigger {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("description")
        private String description;
          @JsonProperty("enabled")
        @Builder.Default
        private boolean enabled = true;
        
        @JsonProperty("path")
        private String path;
        
        @JsonProperty("pattern")
        private String pattern;
        
        @JsonProperty("schedule")
        private String schedule;
        
        @JsonProperty("threshold")
        private String threshold;
        
        @JsonProperty("action")
        private String action;
        
        @JsonProperty("command")
        private String command;
        
        @JsonProperty("conditions")
        private Map<String, Object> conditions;
        
        @JsonProperty("cooldown_seconds")
        @Builder.Default
        private long cooldownSeconds = 60;
    }

    // Convenience methods for backward compatibility and cleaner API
    public boolean isAgentModeEnabled() {
        return enabled;
    }
    
    public void setAgentModeEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public long getExecutionIntervalMs() {
        return monitoringInterval.toMillis();
    }
    
    public void setExecutionIntervalMs(long intervalMs) {
        this.monitoringInterval = Duration.ofMillis(intervalMs);
    }
    
    public boolean isAutoSaveEnabled() {
        return statePersistence != null && statePersistence.isEnabled();
    }
    
    public void setAutoSaveEnabled(boolean enabled) {
        if (statePersistence == null) {
            statePersistence = new StatePersistence();
        }
        statePersistence.setEnabled(enabled);
    }
    
    public String getStateFilePath() {
        return statePersistence != null ? statePersistence.getFilePath() : ".misoto/agent-state.json";
    }
    
    public void setStateFilePath(String filePath) {
        if (statePersistence == null) {
            statePersistence = new StatePersistence();
        }
        statePersistence.setFilePath(filePath);
    }
    
    public int getBackupRetentionDays() {
        return statePersistence != null ? statePersistence.getMaxHistoryEntries() : 7;
    }
      public void setBackupRetentionDays(int days) {
        if (statePersistence == null) {
            statePersistence = new StatePersistence();
        }
        statePersistence.setMaxHistoryEntries(days);
    }
    
    /**
     * Get monitoring triggers, ensuring it's never null
     * @return List of monitoring triggers (empty list if null)
     */
    public List<MonitoringTrigger> getMonitoringTriggers() {
        return monitoringTriggers != null ? monitoringTriggers : List.of();
    }
}
