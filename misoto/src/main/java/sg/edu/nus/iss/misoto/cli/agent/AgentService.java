package sg.edu.nus.iss.misoto.cli.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.misoto.cli.agent.config.AgentConfiguration;
import sg.edu.nus.iss.misoto.cli.agent.decision.DecisionEngine;
import sg.edu.nus.iss.misoto.cli.agent.monitoring.MonitoringService;
import sg.edu.nus.iss.misoto.cli.agent.state.AgentStateManager;
import sg.edu.nus.iss.misoto.cli.agent.task.AgentTask;
import sg.edu.nus.iss.misoto.cli.agent.task.TaskExecutorService;
import sg.edu.nus.iss.misoto.cli.agent.task.TaskQueueService;

import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main Agent Service that orchestrates autonomous behavior.
 * This is the central controller that manages the agent lifecycle,
 * coordinates all subsystems, and handles the main execution loop.
 */
@Slf4j
@Service
@ConditionalOnProperty(
    name = "misoto.agent.mode.enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class AgentService {

    private final AgentConfiguration agentConfig;
    private final AgentStateManager stateManager;
    private final TaskQueueService taskQueue;
    private final TaskExecutorService taskExecutor;
    private final DecisionEngine decisionEngine;
    private final MonitoringService monitoringService;
    
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);
    private ScheduledExecutorService agentExecutor;
    private CompletableFuture<Void> agentLoop;

    @Autowired
    public AgentService(
            AgentConfiguration agentConfig,
            AgentStateManager stateManager,
            TaskQueueService taskQueue,
            TaskExecutorService taskExecutor,
            DecisionEngine decisionEngine,
            MonitoringService monitoringService) {
        this.agentConfig = agentConfig;
        this.stateManager = stateManager;
        this.taskQueue = taskQueue;
        this.taskExecutor = taskExecutor;
        this.decisionEngine = decisionEngine;
        this.monitoringService = monitoringService;
    }

    /**
     * Start the agent when the application is ready
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void onApplicationReady() {
        if (agentConfig.isAgentModeEnabled()) {
            log.info("Agent mode is enabled, starting agent service...");
            startAgent();
        } else {
            log.info("Agent mode is disabled");
        }
    }

    /**
     * Start the autonomous agent
     */
    public synchronized void startAgent() {
        if (isRunning.get()) {
            log.warn("Agent is already running");
            return;
        }

        try {
            log.info("Starting Misoto Agent Service...");
            
            // Initialize executor service
            agentExecutor = new ScheduledThreadPoolExecutor(
                agentConfig.getMaxConcurrentTasks(),
                r -> {
                    Thread t = new Thread(r, "agent-worker");
                    t.setDaemon(true);
                    return t;
                }
            );            // Initialize state manager with configuration
            stateManager.initialize(agentConfig.getStatePersistence());
            stateManager.initializeState();
            
            // Initialize task queue service
            taskQueue.initialize(agentConfig);
            
            // Start monitoring service
            monitoringService.startMonitoring();
            
            // Mark as running
            isRunning.set(true);
            isShuttingDown.set(false);
            
            // Start main agent loop
            agentLoop = runAgentLoop();
            
            log.info("Misoto Agent Service started successfully");
            
        } catch (Exception e) {
            log.error("Failed to start agent service", e);
            stopAgent();
            throw new RuntimeException("Failed to start agent service", e);
        }
    }

    /**
     * Stop the autonomous agent
     */
    public synchronized void stopAgent() {
        if (!isRunning.get()) {
            log.warn("Agent is not running");
            return;
        }

        try {
            log.info("Stopping Misoto Agent Service...");
            isShuttingDown.set(true);
            
            // Stop monitoring
            monitoringService.stopMonitoring();
            
            // Cancel agent loop
            if (agentLoop != null && !agentLoop.isDone()) {
                agentLoop.cancel(true);
            }
              // Shutdown executor
            if (agentExecutor != null) {
                agentExecutor.shutdown();
                try {
                    long timeoutSeconds = agentConfig.getShutdownTimeout().getSeconds();
                    if (!agentExecutor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                        agentExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    agentExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            
            // Save final state
            stateManager.saveState();
            
            isRunning.set(false);
            log.info("Misoto Agent Service stopped");
            
        } catch (Exception e) {
            log.error("Error stopping agent service", e);
        }
    }

    /**
     * Main agent execution loop
     */
    private CompletableFuture<Void> runAgentLoop() {
        return CompletableFuture.runAsync(() -> {
            log.info("Agent main loop started");
            
            while (!isShuttingDown.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    // Execute one agent cycle
                    executeAgentCycle();
                    
                    // Sleep for the configured interval
                    Thread.sleep(agentConfig.getExecutionIntervalMs());
                    
                } catch (InterruptedException e) {
                    log.info("Agent loop interrupted");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Error in agent loop", e);
                    
                    // Use decision engine to determine how to handle the error
                    try {
                        var decision = decisionEngine.handleError(e, "agent_loop");
                        if (decision.shouldRetry()) {
                            log.info("Decision engine recommends retrying after error");
                            Thread.sleep(decision.getRetryDelayMs());
                        } else if (decision.shouldStop()) {
                            log.error("Decision engine recommends stopping agent due to error");
                            break;
                        }
                    } catch (Exception decisionError) {
                        log.error("Error in decision engine while handling agent loop error", decisionError);
                        // Fallback: sleep and continue
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
            
            log.info("Agent main loop ended");
        }, agentExecutor);
    }

    /**
     * Execute one cycle of agent behavior
     */
    private void executeAgentCycle() {
        try {
            log.debug("Executing agent cycle...");
            
            // 1. Update state with current cycle info
            stateManager.updateLastActivity(LocalDateTime.now());
            
            // 2. Task processing is handled by TaskQueueService internally
            
            // 3. Make decisions about new tasks
            makeDecisions();
            
            // 4. Clean up completed tasks
            taskQueue.cleanupCompletedTasks();
            
            // 5. Update statistics
            updateAgentStatistics();
            
            // 6. Periodic state save
            if (shouldSaveState()) {
                stateManager.saveState();
            }
            
        } catch (Exception e) {
            log.error("Error executing agent cycle", e);
            throw e;
        }
    }

    /**
     * Process pending tasks in the queue
     */
    private void processPendingTasks() {
        List<AgentTask> readyTasks = taskQueue.getReadyTasks(agentConfig.getMaxConcurrentTasks());
        
        for (AgentTask task : readyTasks) {
            try {
                log.debug("Processing task: {}", task.getId());
                
                // Execute task asynchronously
                CompletableFuture.runAsync(() -> {
                    try {
                        taskExecutor.executeTask(task);
                    } catch (Exception e) {
                        log.error("Error executing task: {}", task.getId(), e);
                        taskQueue.markTaskFailed(task.getId(), e.getMessage());
                    }
                }, agentExecutor);
                
            } catch (Exception e) {
                log.error("Error starting task execution: {}", task.getId(), e);
                taskQueue.markTaskFailed(task.getId(), e.getMessage());
            }
        }
    }

    /**
     * Make decisions about new tasks and strategies
     */
    private void makeDecisions() {
        try {
            // Get current context
            var context = buildDecisionContext();
            
            // Ask decision engine for recommendations
            var decision = decisionEngine.makeDecision(context);
            
            // Process decision recommendations
            processDecisionRecommendations(decision);
            
        } catch (Exception e) {
            log.error("Error making decisions", e);
        }
    }

    /**
     * Build context for decision making
     */
    private String buildDecisionContext() {
        StringBuilder context = new StringBuilder();
        
        // Add current state info
        var snapshot = stateManager.getCurrentState();
        context.append("Current State:\n");
        context.append("- Agent running: ").append(isRunning.get()).append("\n");
        context.append("- Total tasks executed: ").append(snapshot.getTotalTasksExecuted()).append("\n");
        context.append("- Failed tasks: ").append(snapshot.getFailedTasks()).append("\n");
        context.append("- Last activity: ").append(snapshot.getLastActivity()).append("\n");
        
        // Add queue statistics
        var queueStats = taskQueue.getStatistics();
        context.append("\nQueue Statistics:\n");
        context.append("- Pending tasks: ").append(queueStats.getPendingTasks()).append("\n");
        context.append("- Running tasks: ").append(queueStats.getRunningTasks()).append("\n");
        context.append("- Completed tasks: ").append(queueStats.getCompletedTasks()).append("\n");
        context.append("- Failed tasks: ").append(queueStats.getFailedTasks()).append("\n");
        
        // Add configuration info
        context.append("\nConfiguration:\n");
        context.append("- Max concurrent tasks: ").append(agentConfig.getMaxConcurrentTasks()).append("\n");
        context.append("- Execution interval: ").append(agentConfig.getExecutionIntervalMs()).append("ms\n");
        
        return context.toString();
    }

    /**
     * Process recommendations from decision engine
     */
    private void processDecisionRecommendations(Object decision) {
        // This would be enhanced based on the specific decision types
        log.debug("Processing decision recommendations: {}", decision);
        
        // For now, just log the decision
        // In a full implementation, this would translate decisions into actions
    }

    /**
     * Update agent statistics
     */
    private void updateAgentStatistics() {
        try {
            var stats = taskQueue.getStatistics();
            stateManager.updateStatistics(
                stats.getCompletedTasks(),
                stats.getFailedTasks(),
                stats.getPendingTasks()
            );
        } catch (Exception e) {
            log.error("Error updating agent statistics", e);
        }
    }

    /**
     * Determine if state should be saved this cycle
     */
    private boolean shouldSaveState() {
        // Save state periodically (every 50 cycles by default)
        return stateManager.getCycleCount() % 50 == 0;
    }

    /**
     * Submit a task to the agent
     */
    public void submitTask(AgentTask task) {
        if (!isRunning.get()) {
            throw new IllegalStateException("Agent is not running");
        }
        
        log.info("Submitting task to agent: {}", task.getId());
        taskQueue.submitTask(task);
    }

    /**
     * Get current agent status
     */
    public AgentStatus getStatus() {        return AgentStatus.builder()
            .running(isRunning.get())
            .shuttingDown(isShuttingDown.get())
            .uptime(stateManager.getUptime())
            .totalTasksExecuted((int) stateManager.getCurrentState().getTotalTasksExecuted())
            .queueStatistics(taskQueue.getStatistics())
            .lastActivity(stateManager.getCurrentState().getLastActivity())
            .build();
    }

    /**
     * Graceful shutdown hook
     */
    @PreDestroy
    public void shutdown() {
        log.info("Agent service shutdown requested");
        stopAgent();
    }

    /**
     * Check if agent is running
     */
    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * Agent status data class
     */    @lombok.Data
    @lombok.Builder
    public static class AgentStatus {
        private boolean running;
        private boolean shuttingDown;
        private long uptime;
        private int totalTasksExecuted;
        private Object queueStatistics;
        private Instant lastActivity;
    }
}
