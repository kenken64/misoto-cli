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
    private boolean enabled = false;
    
    @JsonProperty("agent_name")
    private String agentName = "misoto-agent";
    
    @JsonProperty("mode")
    private AgentMode mode = AgentMode.INTERACTIVE;
    
    @JsonProperty("auto_start")
    private boolean autoStart = false;
    
    @JsonProperty("max_concurrent_tasks")
    private int maxConcurrentTasks = 3;
    
    @JsonProperty("task_timeout")
    private Duration taskTimeout = Duration.ofMinutes(30);
    
    @JsonProperty("monitoring_interval")
    private Duration monitoringInterval = Duration.ofSeconds(30);
    
    @JsonProperty("decision_model")
    private String decisionModel = "claude-3-haiku-20240307";
    
    @JsonProperty("decision_temperature")
    private double decisionTemperature = 0.3;
    
    @JsonProperty("state_persistence")
    private StatePersistence statePersistence = new StatePersistence();
    
    @JsonProperty("monitoring")
    private MonitoringConfig monitoringConfig = new MonitoringConfig();
      @JsonProperty("triggers")
    private List<TriggerConfig> triggers = List.of();
    
    @JsonProperty("monitoring_triggers")
    private List<MonitoringTrigger> monitoringTriggers = List.of();
    
    @JsonProperty("capabilities")
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
    public static class StatePersistence {
        @JsonProperty("enabled")
        private boolean enabled = true;
        
        @JsonProperty("file_path")
        private String filePath = ".misoto/agent-state.json";
        
        @JsonProperty("backup_interval")
        private Duration backupInterval = Duration.ofMinutes(5);
        
        @JsonProperty("max_history_entries")
        private int maxHistoryEntries = 1000;
        
        @JsonProperty("compress_old_data")
        private boolean compressOldData = true;
    }
    
    @Data
    public static class MonitoringConfig {
        @JsonProperty("file_system")
        private boolean fileSystemMonitoring = true;
        
        @JsonProperty("process_monitoring")
        private boolean processMonitoring = true;
        
        @JsonProperty("network_monitoring")
        private boolean networkMonitoring = false;
        
        @JsonProperty("log_monitoring")
        private boolean logMonitoring = true;
        
        @JsonProperty("watched_directories")
        private List<String> watchedDirectories = List.of(".", "src", "target");
        
        @JsonProperty("watched_file_patterns")
        private List<String> watchedFilePatterns = List.of("*.java", "*.properties", "*.yml", "*.json");
        
        @JsonProperty("ignored_patterns")
        private List<String> ignoredPatterns = List.of("target/**", ".git/**", "*.class", "*.jar");
    }
    
    @Data
    public static class TriggerConfig {
        @JsonProperty("type")
        private TriggerType type;
        
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("description")
        private String description;
        
        @JsonProperty("enabled")
        private boolean enabled = true;
        
        @JsonProperty("conditions")
        private Map<String, Object> conditions;
        
        @JsonProperty("actions")
        private List<String> actions;
        
        @JsonProperty("cooldown")
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
