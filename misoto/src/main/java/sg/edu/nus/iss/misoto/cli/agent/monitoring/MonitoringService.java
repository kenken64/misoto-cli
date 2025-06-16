package sg.edu.nus.iss.misoto.cli.agent.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.misoto.cli.agent.config.AgentConfiguration;
import sg.edu.nus.iss.misoto.cli.agent.task.AgentTask;
import sg.edu.nus.iss.misoto.cli.agent.task.TaskQueueService;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Continuous Monitoring Service that watches for triggers and schedules tasks.
 * This service monitors file system changes, scheduled events, and other triggers
 * to autonomously create and schedule tasks for the agent.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "misoto.agent.mode.enabled", havingValue = "true", matchIfMissing = true)
public class MonitoringService {

    private final AgentConfiguration agentConfig;
    private final TaskQueueService taskQueue;
    
    private final AtomicBoolean isMonitoring = new AtomicBoolean(false);
    private ScheduledExecutorService monitoringExecutor;
    private WatchService fileWatcher;
    private final Map<WatchKey, Path> watchedDirectories = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    @Autowired
    public MonitoringService(AgentConfiguration agentConfig, TaskQueueService taskQueue) {
        this.agentConfig = agentConfig;
        this.taskQueue = taskQueue;
    }

    /**
     * Start the monitoring service
     */
    public synchronized void startMonitoring() {
        if (isMonitoring.get()) {
            log.warn("Monitoring service is already running");
            return;
        }

        try {
            log.info("Starting monitoring service...");
            
            // Initialize executor
            monitoringExecutor = Executors.newScheduledThreadPool(
                Math.max(2, agentConfig.getMonitoringTriggers().size()),
                r -> {
                    Thread t = new Thread(r, "monitoring-worker");
                    t.setDaemon(true);
                    return t;
                }
            );

            // Initialize file watcher if needed
            initializeFileWatcher();
            
            // Start monitoring triggers
            startTriggerMonitoring();
            
            // Start scheduled monitoring
            startScheduledMonitoring();
            
            isMonitoring.set(true);
            log.info("Monitoring service started successfully");
            
        } catch (Exception e) {
            log.error("Failed to start monitoring service", e);
            stopMonitoring();
            throw new RuntimeException("Failed to start monitoring service", e);
        }
    }

    /**
     * Stop the monitoring service
     */
    public synchronized void stopMonitoring() {
        if (!isMonitoring.get()) {
            log.warn("Monitoring service is not running");
            return;
        }

        try {
            log.info("Stopping monitoring service...");
            isMonitoring.set(false);
            
            // Cancel all scheduled tasks
            scheduledTasks.values().forEach(future -> future.cancel(true));
            scheduledTasks.clear();
            
            // Close file watcher
            if (fileWatcher != null) {
                fileWatcher.close();
                fileWatcher = null;
            }
            watchedDirectories.clear();
              // Shutdown executor
            if (monitoringExecutor != null) {
                monitoringExecutor.shutdown();
                try {
                    long timeoutSeconds = agentConfig.getMonitoringShutdownTimeout().getSeconds();
                    if (!monitoringExecutor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                        monitoringExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    monitoringExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            
            log.info("Monitoring service stopped");
            
        } catch (Exception e) {
            log.error("Error stopping monitoring service", e);
        }
    }

    /**
     * Initialize file system watcher
     */
    private void initializeFileWatcher() {
        try {
            fileWatcher = FileSystems.getDefault().newWatchService();
            
            // Register directories to watch based on configuration
            for (var trigger : agentConfig.getMonitoringTriggers()) {
                if ("file_change".equals(trigger.getType()) && trigger.getPath() != null) {
                    registerDirectoryForWatching(Paths.get(trigger.getPath()));
                }
            }
            
            // Start file watching thread
            if (!watchedDirectories.isEmpty()) {
                monitoringExecutor.submit(this::runFileWatcher);
            }
            
        } catch (IOException e) {
            log.error("Failed to initialize file watcher", e);
        }
    }

    /**
     * Register a directory for file system watching
     */
    private void registerDirectoryForWatching(Path directory) {
        try {
            if (Files.isDirectory(directory)) {
                WatchKey key = directory.register(fileWatcher,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);
                watchedDirectories.put(key, directory);
                log.info("Registered directory for watching: {}", directory);
            }
        } catch (IOException e) {
            log.error("Failed to register directory for watching: {}", directory, e);
        }
    }

    /**
     * File watcher thread
     */
    private void runFileWatcher() {
        log.info("File watcher thread started");
        
        while (isMonitoring.get() && !Thread.currentThread().isInterrupted()) {
            try {
                WatchKey key = fileWatcher.poll(1, TimeUnit.SECONDS);
                if (key == null) {
                    continue;
                }
                
                Path directory = watchedDirectories.get(key);
                if (directory == null) {
                    continue;
                }
                
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                    Path changedFile = directory.resolve(pathEvent.context());
                    
                    handleFileSystemEvent(event.kind(), changedFile);
                }
                
                boolean valid = key.reset();
                if (!valid) {
                    watchedDirectories.remove(key);
                    log.warn("Watch key became invalid for directory: {}", directory);
                }
                
            } catch (InterruptedException e) {
                log.info("File watcher interrupted");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error in file watcher", e);
            }
        }
        
        log.info("File watcher thread ended");
    }

    /**
     * Handle file system events
     */
    private void handleFileSystemEvent(WatchEvent.Kind<?> eventKind, Path filePath) {
        try {
            log.debug("File system event: {} on {}", eventKind.name(), filePath);
            
            // Find matching triggers
            for (var trigger : agentConfig.getMonitoringTriggers()) {
                if (isFileEventMatchingTrigger(trigger, eventKind, filePath)) {
                    createTaskFromTrigger(trigger, "File system event: " + eventKind.name() + " on " + filePath);
                }
            }
            
        } catch (Exception e) {
            log.error("Error handling file system event", e);
        }
    }

    /**
     * Check if file event matches trigger
     */
    private boolean isFileEventMatchingTrigger(AgentConfiguration.MonitoringTrigger trigger, 
                                             WatchEvent.Kind<?> eventKind, Path filePath) {
        if (!"file_change".equals(trigger.getType())) {
            return false;
        }
        
        // Check if path matches
        if (trigger.getPath() != null) {
            Path triggerPath = Paths.get(trigger.getPath());
            if (!filePath.startsWith(triggerPath)) {
                return false;
            }
        }
        
        // Check if pattern matches (if specified)
        if (trigger.getPattern() != null) {
            String fileName = filePath.getFileName().toString();
            if (!fileName.matches(trigger.getPattern())) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Start monitoring configured triggers
     */
    private void startTriggerMonitoring() {
        for (var trigger : agentConfig.getMonitoringTriggers()) {
            switch (trigger.getType()) {
                case "scheduled":
                    startScheduledTrigger(trigger);
                    break;
                case "interval":
                    startIntervalTrigger(trigger);
                    break;
                case "system_metric":
                    startSystemMetricTrigger(trigger);
                    break;
                case "log_pattern":
                    startLogPatternTrigger(trigger);
                    break;
                default:
                    log.debug("Trigger type {} handled elsewhere or not implemented", trigger.getType());
            }
        }
    }

    /**
     * Start scheduled trigger
     */
    private void startScheduledTrigger(AgentConfiguration.MonitoringTrigger trigger) {
        try {
            // Parse schedule (simple implementation - could be enhanced with cron expressions)
            long delayMs = parseScheduleDelay(trigger.getSchedule());
            
            ScheduledFuture<?> scheduledFuture = monitoringExecutor.scheduleAtFixedRate(
                () -> createTaskFromTrigger(trigger, "Scheduled trigger"),
                delayMs,
                delayMs,
                TimeUnit.MILLISECONDS
            );
            
            scheduledTasks.put(trigger.getName(), scheduledFuture);
            log.info("Started scheduled trigger: {} with delay: {}ms", trigger.getName(), delayMs);
            
        } catch (Exception e) {
            log.error("Failed to start scheduled trigger: {}", trigger.getName(), e);
        }
    }

    /**
     * Start interval trigger
     */
    private void startIntervalTrigger(AgentConfiguration.MonitoringTrigger trigger) {
        try {
            long intervalMs = Long.parseLong(trigger.getSchedule());
            
            ScheduledFuture<?> scheduledFuture = monitoringExecutor.scheduleAtFixedRate(
                () -> createTaskFromTrigger(trigger, "Interval trigger"),
                intervalMs,
                intervalMs,
                TimeUnit.MILLISECONDS
            );
            
            scheduledTasks.put(trigger.getName(), scheduledFuture);
            log.info("Started interval trigger: {} with interval: {}ms", trigger.getName(), intervalMs);
            
        } catch (Exception e) {
            log.error("Failed to start interval trigger: {}", trigger.getName(), e);
        }
    }

    /**
     * Start system metric monitoring trigger
     */
    private void startSystemMetricTrigger(AgentConfiguration.MonitoringTrigger trigger) {
        ScheduledFuture<?> scheduledFuture = monitoringExecutor.scheduleAtFixedRate(
            () -> checkSystemMetrics(trigger),
            5000, // Check every 5 seconds
            5000,
            TimeUnit.MILLISECONDS
        );
        
        scheduledTasks.put(trigger.getName(), scheduledFuture);
        log.info("Started system metric trigger: {}", trigger.getName());
    }

    /**
     * Start log pattern monitoring trigger
     */
    private void startLogPatternTrigger(AgentConfiguration.MonitoringTrigger trigger) {
        // This would monitor log files for specific patterns
        // For now, just log that it's not fully implemented
        log.info("Log pattern trigger registered but not fully implemented: {}", trigger.getName());
    }

    /**
     * Start general scheduled monitoring tasks
     */
    private void startScheduledMonitoring() {
        // Health check monitoring
        monitoringExecutor.scheduleAtFixedRate(
            this::performHealthCheck,
            30000, // Initial delay: 30 seconds
            60000, // Repeat every minute
            TimeUnit.MILLISECONDS
        );
        
        // Resource monitoring
        monitoringExecutor.scheduleAtFixedRate(
            this::monitorResources,
            10000, // Initial delay: 10 seconds
            30000, // Repeat every 30 seconds
            TimeUnit.MILLISECONDS
        );
    }

    /**
     * Create task from trigger
     */
    private void createTaskFromTrigger(AgentConfiguration.MonitoringTrigger trigger, String context) {
        try {            AgentTask task = AgentTask.builder()
                .type(AgentTask.TaskType.valueOf(trigger.getAction().toUpperCase()))
                .priority(AgentTask.TaskPriority.MEDIUM)
                .description("Triggered by: " + trigger.getName())                .context(createTaskContext(Map.of(
                    "trigger", trigger.getName(),
                    "trigger_type", trigger.getType(),
                    "context", context,
                    "timestamp", Instant.now().toString()
                )))
                .createdAt(Instant.now())
                .build();
            
            if (trigger.getCommand() != null) {
                task.getContext().put("command", trigger.getCommand());
            }
            
            taskQueue.submitTask(task);
            log.info("Created task from trigger: {} -> {}", trigger.getName(), task.getId());
            
        } catch (Exception e) {
            log.error("Failed to create task from trigger: {}", trigger.getName(), e);
        }
    }

    /**
     * Parse schedule delay from string (simple implementation)
     */
    private long parseScheduleDelay(String schedule) {
        if (schedule == null) {
            return 60000; // Default: 1 minute
        }
        
        try {
            // Simple parsing - could be enhanced with proper cron parsing
            if (schedule.endsWith("s")) {
                return Long.parseLong(schedule.substring(0, schedule.length() - 1)) * 1000;
            } else if (schedule.endsWith("m")) {
                return Long.parseLong(schedule.substring(0, schedule.length() - 1)) * 60 * 1000;
            } else if (schedule.endsWith("h")) {
                return Long.parseLong(schedule.substring(0, schedule.length() - 1)) * 60 * 60 * 1000;
            } else {
                return Long.parseLong(schedule); // Assume milliseconds
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid schedule format: {}, using default", schedule);
            return 60000; // Default: 1 minute
        }
    }

    /**
     * Check system metrics
     */
    private void checkSystemMetrics(AgentConfiguration.MonitoringTrigger trigger) {
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
            
            // Check if threshold is exceeded (example: memory usage > 80%)
            if (trigger.getThreshold() != null) {
                double threshold = Double.parseDouble(trigger.getThreshold());
                if (memoryUsagePercent > threshold) {
                    createTaskFromTrigger(trigger, 
                        String.format("Memory usage %.1f%% exceeds threshold %.1f%%", 
                            memoryUsagePercent, threshold));
                }
            }
            
        } catch (Exception e) {
            log.error("Error checking system metrics for trigger: {}", trigger.getName(), e);
        }
    }

    /**
     * Perform health check
     */
    private void performHealthCheck() {
        try {
            log.debug("Performing health check...");
            
            // Check if task queue is responsive
            var stats = taskQueue.getStatistics();
            
            // Check for stuck tasks
            if (stats.getRunningTasks() > agentConfig.getMaxConcurrentTasks() * 2) {
                log.warn("Potentially stuck tasks detected: {} running tasks", stats.getRunningTasks());
                  // Create a system task to investigate
                AgentTask healthTask = AgentTask.builder()                    .type(AgentTask.TaskType.SYSTEM)
                    .priority(AgentTask.TaskPriority.HIGH)
                    .description("Health check: investigate stuck tasks")
                    .context(createTaskContext(Map.of(
                        "action", "investigate_stuck_tasks",
                        "running_tasks", String.valueOf(stats.getRunningTasks())
                    )))
                    .createdAt(Instant.now())
                    .build();
                    
                taskQueue.submitTask(healthTask);
            }
            
        } catch (Exception e) {
            log.error("Error performing health check", e);
        }
    }

    /**
     * Monitor system resources
     */
    private void monitorResources() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
            
            log.debug("Resource monitoring - Memory usage: {:.1f}% ({} MB / {} MB)", 
                memoryUsagePercent, 
                usedMemory / (1024 * 1024), 
                maxMemory / (1024 * 1024));
            
            // Log warning if memory usage is high
            if (memoryUsagePercent > 85) {
                log.warn("High memory usage detected: {:.1f}%", memoryUsagePercent);
            }
            
        } catch (Exception e) {
            log.error("Error monitoring resources", e);
        }
    }

    /**
     * Check if monitoring is active
     */
    public boolean isMonitoring() {
        return isMonitoring.get();
    }

    /**
     * Get monitoring statistics
     */
    public MonitoringStats getStatistics() {
        return MonitoringStats.builder()
            .monitoring(isMonitoring.get())
            .activeTriggers(scheduledTasks.size())
            .watchedDirectories(watchedDirectories.size())
            .configuredTriggers(agentConfig.getMonitoringTriggers().size())
            .build();
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
    
    /**
     * Monitoring statistics data class
     */
    @lombok.Data
    @lombok.Builder
    public static class MonitoringStats {
        private boolean monitoring;
        private int activeTriggers;
        private int watchedDirectories;
        private int configuredTriggers;
    }
}
