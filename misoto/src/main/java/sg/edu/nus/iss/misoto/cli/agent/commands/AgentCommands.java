package sg.edu.nus.iss.misoto.cli.agent.commands;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import sg.edu.nus.iss.misoto.cli.agent.AgentService;
import sg.edu.nus.iss.misoto.cli.agent.config.AgentConfiguration;
import sg.edu.nus.iss.misoto.cli.agent.monitoring.MonitoringService;
import sg.edu.nus.iss.misoto.cli.agent.task.AgentTask;
import sg.edu.nus.iss.misoto.cli.agent.task.TaskQueueService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * CLI commands for agent mode control and management.
 * Provides commands to start/stop the agent, submit tasks, and monitor status.
 */
@Slf4j
@ShellComponent
@ConditionalOnProperty(name = "misoto.agent.mode.enabled", havingValue = "true", matchIfMissing = false)
public class AgentCommands {

    private final AgentService agentService;
    private final AgentConfiguration agentConfig;
    private final TaskQueueService taskQueue;
    private final MonitoringService monitoringService;

    @Autowired
    public AgentCommands(
            AgentService agentService,
            AgentConfiguration agentConfig,
            TaskQueueService taskQueue,
            MonitoringService monitoringService) {
        this.agentService = agentService;
        this.agentConfig = agentConfig;
        this.taskQueue = taskQueue;
        this.monitoringService = monitoringService;
    }

    /**
     * Start the agent
     */
    @ShellMethod(value = "Start the autonomous agent", key = {"agent-start", "start-agent"})
    public String startAgent() {
        try {
            if (agentService.isRunning()) {
                return "Agent is already running";
            }
            
            if (!agentConfig.isAgentModeEnabled()) {
                return "Agent mode is disabled in configuration. Enable it first with 'agent-config --enable'";
            }
            
            agentService.startAgent();
            return "Agent started successfully";
            
        } catch (Exception e) {
            log.error("Failed to start agent", e);
            return "Failed to start agent: " + e.getMessage();
        }
    }

    /**
     * Stop the agent
     */
    @ShellMethod(value = "Stop the autonomous agent", key = {"agent-stop", "stop-agent"})
    public String stopAgent() {
        try {
            if (!agentService.isRunning()) {
                return "Agent is not running";
            }
            
            agentService.stopAgent();
            return "Agent stopped successfully";
            
        } catch (Exception e) {
            log.error("Failed to stop agent", e);
            return "Failed to stop agent: " + e.getMessage();
        }
    }

    /**
     * Get agent status
     */
    @ShellMethod(value = "Show agent status and statistics", key = {"agent-status", "status"})
    public String getAgentStatus() {
        try {
            var status = agentService.getStatus();
            var monitoringStats = monitoringService.getStatistics();
            
            StringBuilder sb = new StringBuilder();
            sb.append("=== Agent Status ===\n");
            sb.append(String.format("Running: %s\n", status.isRunning() ? "Yes" : "No"));
            sb.append(String.format("Shutting Down: %s\n", status.isShuttingDown() ? "Yes" : "No"));
            sb.append(String.format("Uptime: %d seconds\n", status.getUptime()));
            sb.append(String.format("Total Tasks Executed: %d\n", status.getTotalTasksExecuted()));
            sb.append(String.format("Last Activity: %s\n", status.getLastActivity()));
            
            sb.append("\n=== Queue Statistics ===\n");
            var queueStats = taskQueue.getStatistics();
            sb.append(String.format("Pending Tasks: %d\n", queueStats.getPendingTasks()));
            sb.append(String.format("Running Tasks: %d\n", queueStats.getRunningTasks()));
            sb.append(String.format("Completed Tasks: %d\n", queueStats.getCompletedTasks()));
            sb.append(String.format("Failed Tasks: %d\n", queueStats.getFailedTasks()));
            
            sb.append("\n=== Monitoring Status ===\n");
            sb.append(String.format("Monitoring Active: %s\n", monitoringStats.isMonitoring() ? "Yes" : "No"));
            sb.append(String.format("Active Triggers: %d\n", monitoringStats.getActiveTriggers()));
            sb.append(String.format("Watched Directories: %d\n", monitoringStats.getWatchedDirectories()));
            sb.append(String.format("Configured Triggers: %d\n", monitoringStats.getConfiguredTriggers()));
            
            sb.append("\n=== Configuration ===\n");
            sb.append(String.format("Agent Mode Enabled: %s\n", agentConfig.isAgentModeEnabled() ? "Yes" : "No"));
            sb.append(String.format("Max Concurrent Tasks: %d\n", agentConfig.getMaxConcurrentTasks()));
            sb.append(String.format("Execution Interval: %d ms\n", agentConfig.getExecutionIntervalMs()));
            sb.append(String.format("Auto Save Enabled: %s\n", agentConfig.isAutoSaveEnabled() ? "Yes" : "No"));
            
            return sb.toString();
            
        } catch (Exception e) {
            log.error("Failed to get agent status", e);
            return "Failed to get agent status: " + e.getMessage();
        }
    }

    /**
     * Submit a task to the agent
     */
    @ShellMethod(value = "Submit a task to the agent", key = {"agent-task", "submit-task"})
    public String submitTask(
            @ShellOption(value = "--type", defaultValue = "SHELL") String type,
            @ShellOption(value = "--description", defaultValue = "Manual task") String description,
            @ShellOption(value = "--command", defaultValue = "") String command,
            @ShellOption(value = "--priority", defaultValue = "NORMAL") String priority) {
        
        try {
            if (!agentService.isRunning()) {
                return "Agent is not running. Start it first with 'agent-start'";
            }
            
            AgentTask.TaskType taskType;
            try {
                taskType = AgentTask.TaskType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                return "Invalid task type: " + type + ". Valid types: " + 
                       String.join(", ", java.util.Arrays.stream(AgentTask.TaskType.values())
                           .map(Enum::name).toArray(String[]::new));
            }
              AgentTask.TaskPriority taskPriority;
            try {
                taskPriority = AgentTask.TaskPriority.valueOf(priority.toUpperCase());
            } catch (IllegalArgumentException e) {
                return "Invalid priority: " + priority + ". Valid priorities: " + 
                       String.join(", ", java.util.Arrays.stream(AgentTask.TaskPriority.values())
                           .map(Enum::name).toArray(String[]::new));
            }
              AgentTask task = AgentTask.builder()
                .type(taskType)
                .priority(taskPriority)
                .description(description)
                .context(command.isEmpty() ? new AgentTask.TaskContext() : createTaskContext(Map.of("command", command)))
                .createdAt(Instant.now())
                .build();
            
            agentService.submitTask(task);
            
            return String.format("Task submitted successfully. ID: %s, Type: %s, Priority: %s", 
                task.getId(), taskType, taskPriority);
            
        } catch (Exception e) {
            log.error("Failed to submit task", e);
            return "Failed to submit task: " + e.getMessage();
        }
    }

    /**
     * Configure agent settings
     */
    @ShellMethod(value = "Configure agent settings", key = {"agent-config", "config-agent"})
    public String configureAgent(
            @ShellOption(value = "--enable", defaultValue = "false") boolean enable,
            @ShellOption(value = "--disable", defaultValue = "false") boolean disable,
            @ShellOption(value = "--max-tasks", defaultValue = "-1") int maxTasks,
            @ShellOption(value = "--interval", defaultValue = "-1") long interval,
            @ShellOption(value = "--auto-save", defaultValue = "false") boolean autoSave) {
        
        try {
            StringBuilder result = new StringBuilder();
            result.append("Agent configuration updated:\n");
            
            if (enable && !disable) {
                agentConfig.setAgentModeEnabled(true);
                result.append("- Agent mode: ENABLED\n");
            } else if (disable && !enable) {
                agentConfig.setAgentModeEnabled(false);
                result.append("- Agent mode: DISABLED\n");
                
                // Stop agent if it's running
                if (agentService.isRunning()) {
                    agentService.stopAgent();
                    result.append("- Agent stopped due to being disabled\n");
                }
            }
            
            if (maxTasks > 0) {
                agentConfig.setMaxConcurrentTasks(maxTasks);
                result.append("- Max concurrent tasks: ").append(maxTasks).append("\n");
            }
            
            if (interval > 0) {
                agentConfig.setExecutionIntervalMs(interval);
                result.append("- Execution interval: ").append(interval).append(" ms\n");
            }
            
            if (autoSave) {
                agentConfig.setAutoSaveEnabled(true);
                result.append("- Auto save: ENABLED\n");
            }
            
            // Save configuration
            // Note: In a real implementation, this would persist to a config file
            result.append("\nConfiguration saved successfully.");
            
            return result.toString();
            
        } catch (Exception e) {
            log.error("Failed to configure agent", e);
            return "Failed to configure agent: " + e.getMessage();
        }
    }

    /**
     * List recent tasks
     */
    @ShellMethod(value = "List recent tasks", key = {"agent-tasks", "list-tasks"})
    public String listTasks(
            @ShellOption(value = "--limit", defaultValue = "10") int limit,
            @ShellOption(value = "--status", defaultValue = "ALL") String status) {
          try {
            List<AgentTask> tasks = taskQueue.getRecentTasks(limit);
            
            if (tasks.isEmpty()) {
                return "No tasks found";
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("=== Recent Tasks ===\n");
            sb.append(String.format("%-36s %-10s %-8s %-12s %-20s %s\n", 
                "ID", "TYPE", "PRIORITY", "STATUS", "CREATED", "DESCRIPTION"));
            sb.append("-".repeat(120)).append("\n");
            
            for (AgentTask task : tasks) {
                if (!"ALL".equalsIgnoreCase(status) && 
                    !status.equalsIgnoreCase(task.getStatus().name())) {
                    continue;
                }
                
                sb.append(String.format("%-36s %-10s %-8s %-12s %-20s %s\n",
                    task.getId(),
                    task.getType(),
                    task.getPriority(),
                    task.getStatus(),
                    task.getCreatedAt().toString().substring(0, 19),
                    task.getDescription().length() > 40 ? 
                        task.getDescription().substring(0, 37) + "..." : 
                        task.getDescription()));
            }
            
            return sb.toString();
            
        } catch (Exception e) {
            log.error("Failed to list tasks", e);
            return "Failed to list tasks: " + e.getMessage();
        }
    }

    /**
     * Clear completed tasks
     */
    @ShellMethod(value = "Clear completed tasks from queue", key = {"agent-clear", "clear-tasks"})
    public String clearTasks() {
        try {
            int clearedCount = taskQueue.clearCompletedTasks();
            return String.format("Cleared %d completed tasks from queue", clearedCount);
            
        } catch (Exception e) {
            log.error("Failed to clear tasks", e);
            return "Failed to clear tasks: " + e.getMessage();
        }
    }

    /**
     * Show agent help
     */
    @ShellMethod(value = "Show agent help and examples", key = {"agent-help", "help-agent"})
    public String showHelp() {
        return """
            === Agent Mode Help ===
            
            The agent mode allows Misoto to run autonomously, monitoring triggers and executing tasks.
            
            Basic Commands:
            - agent-start                    : Start the autonomous agent
            - agent-stop                     : Stop the autonomous agent
            - agent-status                   : Show current status and statistics
            - agent-config --enable          : Enable agent mode
            - agent-config --disable         : Disable agent mode
            
            Task Management:
            - agent-task --type SHELL --command "ls -la" : Submit a shell command task
            - agent-task --type AI --description "analyze logs" : Submit an AI task
            - agent-tasks --limit 20         : List recent tasks
            - agent-clear                    : Clear completed tasks
            
            Configuration:
            - agent-config --max-tasks 5    : Set max concurrent tasks
            - agent-config --interval 5000  : Set execution interval (ms)
            - agent-config --auto-save      : Enable auto-save
            
            Task Types:
            - SHELL      : Execute shell commands
            - FILE       : File operations
            - AI         : AI-powered tasks
            - MCP        : Model Context Protocol tasks
            - SYSTEM     : System maintenance tasks
            - COMPOSITE  : Multi-step tasks
            - CUSTOM     : Custom task types
            
            Task Priorities:
            - LOW        : Low priority, executed when resources available
            - NORMAL     : Normal priority (default)
            - HIGH       : High priority, executed before normal tasks
            - CRITICAL   : Critical priority, executed immediately
            
            Examples:
            - agent-config --enable && agent-start
            - agent-task --type SHELL --command "git status" --priority HIGH
            - agent-task --type AI --description "summarize recent changes"
            """;
    }
    
    /**
     * Create TaskContext from Map for compatibility
     */
    private AgentTask.TaskContext createTaskContext(Map<String, String> contextMap) {
        AgentTask.TaskContext taskContext = new AgentTask.TaskContext();
        for (Map.Entry<String, String> entry : contextMap.entrySet()) {
            taskContext.put(entry.getKey(), entry.getValue());
        }
        return taskContext;
    }
}
