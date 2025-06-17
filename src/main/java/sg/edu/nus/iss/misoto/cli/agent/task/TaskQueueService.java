package sg.edu.nus.iss.misoto.cli.agent.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.misoto.cli.agent.config.AgentConfiguration;
import sg.edu.nus.iss.misoto.cli.agent.state.AgentStateManager;

import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Manages and executes tasks in the agent system
 */
@Service
@Slf4j
public class TaskQueueService {
    
    @Autowired
    private AgentStateManager stateManager;
    
    @Autowired
    private TaskExecutorService taskExecutor;
    
    private final Map<String, AgentTask> tasks = new ConcurrentHashMap<>();
    private final PriorityBlockingQueue<AgentTask> taskQueue = new PriorityBlockingQueue<>(
        100, Comparator.comparing((AgentTask t) -> t.getPriority().getLevel())
                      .thenComparing(AgentTask::getCreatedAt)
    );
    
    private ExecutorService executorService;
    private ScheduledExecutorService scheduledExecutor;
    private volatile boolean running = false;
    
    private AgentConfiguration config;
    private int maxConcurrentTasks = 3;
    private final Set<String> runningTasks = ConcurrentHashMap.newKeySet();
    
    /**
     * Initialize the task queue service
     */
    public void initialize(AgentConfiguration config) {
        this.config = config;
        this.maxConcurrentTasks = config.getMaxConcurrentTasks();
        
        // Create thread pools
        this.executorService = Executors.newFixedThreadPool(
            maxConcurrentTasks,
            r -> {
                Thread t = new Thread(r, "agent-task-executor");
                t.setDaemon(true);
                return t;
            }
        );
        
        this.scheduledExecutor = Executors.newScheduledThreadPool(
            2,
            r -> {
                Thread t = new Thread(r, "agent-task-scheduler");
                t.setDaemon(true);
                return t;
            }
        );
        
        // Start task processing
        startTaskProcessing();
        
        // Schedule periodic cleanup
        schedulePeriodicCleanup();
        
        log.info("Task queue service initialized with max concurrent tasks: {}", maxConcurrentTasks);
    }
    
    /**
     * Submit a new task to the queue
     */
    public String submitTask(AgentTask task) {
        if (task.getId() == null) {
            task.setId(UUID.randomUUID().toString());
        }
        
        // Validate task
        if (!isValidTask(task)) {
            throw new IllegalArgumentException("Invalid task: " + task.getName());
        }
        
        // Store task
        tasks.put(task.getId(), task);
        
        // Check dependencies
        if (areDependenciesMet(task)) {
            task.setStatus(AgentTask.TaskStatus.QUEUED);
            taskQueue.offer(task);
            log.debug("Task queued: {} [{}]", task.getName(), task.getId());
        } else {
            task.setStatus(AgentTask.TaskStatus.WAITING_FOR_DEPENDENCIES);
            log.debug("Task waiting for dependencies: {} [{}]", task.getName(), task.getId());
        }
        
        // Update state
        stateManager.setState("task_count", tasks.size());
        stateManager.setState("queued_task_count", taskQueue.size());
        
        return task.getId();
    }
    
    /**
     * Cancel a task
     */
    public boolean cancelTask(String taskId) {
        AgentTask task = tasks.get(taskId);
        if (task == null) {
            return false;
        }
        
        if (task.getStatus() == AgentTask.TaskStatus.RUNNING) {
            // Try to interrupt running task
            taskExecutor.cancelTask(taskId);
        }
        
        task.setStatus(AgentTask.TaskStatus.CANCELLED);
        task.setCompletedAt(Instant.now());
        
        // Remove from queue if present
        taskQueue.remove(task);
        runningTasks.remove(taskId);
        
        log.info("Task cancelled: {} [{}]", task.getName(), taskId);
        return true;
    }
    
    /**
     * Get task by ID
     */
    public AgentTask getTask(String taskId) {
        return tasks.get(taskId);
    }
    
    /**
     * Get all tasks
     */
    public Collection<AgentTask> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }
    
    /**
     * Get tasks by status
     */
    public List<AgentTask> getTasksByStatus(AgentTask.TaskStatus status) {
        return tasks.values().stream()
            .filter(task -> task.getStatus() == status)
            .collect(Collectors.toList());
    }
    
    /**
     * Get running tasks
     */
    public List<AgentTask> getRunningTasks() {
        return getTasksByStatus(AgentTask.TaskStatus.RUNNING);
    }
    
    /**
     * Get queue statistics
     */
    public TaskQueueStats getStats() {
        Map<AgentTask.TaskStatus, Long> statusCounts = tasks.values().stream()
            .collect(Collectors.groupingBy(AgentTask::getStatus, Collectors.counting()));
            
        return TaskQueueStats.builder()
            .totalTasks(tasks.size())
            .queuedTasks(taskQueue.size())
            .runningTasks(runningTasks.size())
            .completedTasks(statusCounts.getOrDefault(AgentTask.TaskStatus.COMPLETED, 0L).intValue())
            .failedTasks(statusCounts.getOrDefault(AgentTask.TaskStatus.FAILED, 0L).intValue())
            .statusCounts(statusCounts)
            .build();
    }
    
    /**
     * Pause task processing
     */
    public void pauseProcessing() {
        running = false;
        log.info("Task processing paused");
    }
    
    /**
     * Resume task processing
     */
    public void resumeProcessing() {
        if (!running) {
            running = true;
            startTaskProcessing();
            log.info("Task processing resumed");
        }
    }
    
    /**
     * Clear completed tasks
     */
    public int clearCompletedTasks() {
        List<String> completedTaskIds = tasks.values().stream()
            .filter(AgentTask::isCompleted)
            .map(AgentTask::getId)
            .collect(Collectors.toList());
            
        completedTaskIds.forEach(tasks::remove);
        
        log.info("Cleared {} completed tasks", completedTaskIds.size());
        return completedTaskIds.size();
    }
    
    @PreDestroy
    public void shutdown() {
        running = false;
        
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        if (scheduledExecutor != null) {
            scheduledExecutor.shutdown();
        }
        
        log.info("Task queue service shutdown");
    }
    
    private void startTaskProcessing() {
        running = true;
        
        // Start task processor threads
        for (int i = 0; i < maxConcurrentTasks; i++) {
            executorService.submit(this::processTaskQueue);
        }
        
        // Start dependency checker
        scheduledExecutor.scheduleAtFixedRate(
            this::checkDependencies,
            5, 5, TimeUnit.SECONDS
        );
    }
    
    private void processTaskQueue() {
        while (running) {
            try {
                // Wait for available slot
                while (runningTasks.size() >= maxConcurrentTasks && running) {
                    Thread.sleep(100);
                }
                
                if (!running) break;
                
                // Get next task
                AgentTask task = taskQueue.poll(1, TimeUnit.SECONDS);
                if (task == null) continue;
                
                // Check if task is still valid
                if (!task.canExecute()) {
                    continue;
                }
                
                // Execute task
                runningTasks.add(task.getId());
                try {
                    executeTask(task);
                } finally {
                    runningTasks.remove(task.getId());
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error in task processing loop", e);
            }
        }
    }
    
    private void executeTask(AgentTask task) {
        log.info("Executing task: {} [{}]", task.getName(), task.getId());
        
        task.markStarted();
        stateManager.setState("current_task", task.getId());
        
        try {
            AgentTask.TaskResult result = taskExecutor.executeTask(task);
            task.markCompleted(result);
            
            // Increment total tasks executed counter in state manager
            stateManager.incrementTotalTasksExecuted();
            
            log.info("Task completed successfully: {} [{}]", task.getName(), task.getId());
            
            // Trigger dependent tasks
            triggerDependentTasks(task.getId());
            
        } catch (Exception e) {
            task.markFailed(e.getMessage());
            log.error("Task failed: {} [{}] - {}", task.getName(), task.getId(), e.getMessage());
            
            // Schedule retry if appropriate
            if (task.shouldRetry()) {
                scheduleRetry(task);
            }
        } finally {
            stateManager.removeState("current_task");
        }
    }
    
    private void checkDependencies() {
        List<AgentTask> waitingTasks = getTasksByStatus(AgentTask.TaskStatus.WAITING_FOR_DEPENDENCIES);
        
        for (AgentTask task : waitingTasks) {
            if (areDependenciesMet(task)) {
                task.setStatus(AgentTask.TaskStatus.QUEUED);
                taskQueue.offer(task);
                log.debug("Dependencies met, task queued: {} [{}]", task.getName(), task.getId());
            }
        }
    }
    
    private boolean areDependenciesMet(AgentTask task) {
        for (String dependencyId : task.getDependencies()) {
            AgentTask dependency = tasks.get(dependencyId);
            if (dependency == null || dependency.getStatus() != AgentTask.TaskStatus.COMPLETED) {
                return false;
            }
        }
        return true;
    }
    
    private void triggerDependentTasks(String completedTaskId) {
        tasks.values().stream()
            .filter(task -> task.getStatus() == AgentTask.TaskStatus.WAITING_FOR_DEPENDENCIES)
            .filter(task -> task.getDependencies().contains(completedTaskId))
            .forEach(task -> {
                if (areDependenciesMet(task)) {
                    task.setStatus(AgentTask.TaskStatus.QUEUED);
                    taskQueue.offer(task);
                }
            });
    }
    
    private void scheduleRetry(AgentTask task) {
        long delay = Math.min(60, task.getRetryCount() * 10); // Exponential backoff, max 60s
        
        scheduledExecutor.schedule(() -> {
            if (task.shouldRetry()) {
                task.setStatus(AgentTask.TaskStatus.QUEUED);
                taskQueue.offer(task);
                log.info("Task retry scheduled: {} [{}] (attempt {})", 
                    task.getName(), task.getId(), task.getRetryCount() + 1);
            }
        }, delay, TimeUnit.SECONDS);
    }
    
    private boolean isValidTask(AgentTask task) {
        return task != null && 
               task.getType() != null && 
               task.getName() != null && 
               !task.getName().trim().isEmpty();
    }
    
    private void schedulePeriodicCleanup() {
        scheduledExecutor.scheduleAtFixedRate(
            this::performCleanup,
            1, 1, TimeUnit.HOURS
        );
    }
    
    private void performCleanup() {
        // Remove old completed tasks
        long cutoff = Instant.now().minusSeconds(24 * 3600).getEpochSecond(); // 24 hours ago
        
        List<String> oldTaskIds = tasks.values().stream()
            .filter(AgentTask::isCompleted)
            .filter(task -> task.getCompletedAt().getEpochSecond() < cutoff)
            .map(AgentTask::getId)
            .collect(Collectors.toList());
            
        oldTaskIds.forEach(tasks::remove);        if (!oldTaskIds.isEmpty()) {
            log.info("Cleaned up {} old completed tasks", oldTaskIds.size());
        }
    }
    
    /**
     * Get recent tasks (for CLI display)
     */
    public List<AgentTask> getRecentTasks(int limit) {
        return tasks.values().stream()
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Get ready tasks for execution
     */
    public List<AgentTask> getReadyTasks(int maxTasks) {
        return tasks.values().stream()
            .filter(task -> task.getStatus() == AgentTask.TaskStatus.QUEUED || task.getStatus() == AgentTask.TaskStatus.PENDING)
            .filter(AgentTask::canExecute)
            .sorted((a, b) -> b.getPriority().ordinal() - a.getPriority().ordinal())
            .limit(maxTasks)
            .collect(Collectors.toList());
    }
    
    /**
     * Mark task as failed
     */
    public void markTaskFailed(String taskId, String errorMessage) {
        AgentTask task = tasks.get(taskId);
        if (task != null) {
            task.setStatus(AgentTask.TaskStatus.FAILED);
            task.setCompletedAt(Instant.now());
            task.setErrorMessage(errorMessage);
            log.warn("Task {} marked as failed: {}", taskId, errorMessage);
        }
    }
    
    /**
     * Cleanup completed tasks
     */
    public void cleanupCompletedTasks() {
        // Only clean up completed/failed tasks older than 30 minutes to allow users to see recent results
        long thirtyMinutesAgo = Instant.now().minusSeconds(30 * 60).getEpochSecond();
        List<String> oldCompletedTaskIds = new ArrayList<>();
        
        for (AgentTask task : tasks.values()) {
            if ((task.getStatus() == AgentTask.TaskStatus.COMPLETED || 
                 task.getStatus() == AgentTask.TaskStatus.FAILED) &&
                task.getCompletedAt() != null &&
                task.getCompletedAt().getEpochSecond() < thirtyMinutesAgo) {
                oldCompletedTaskIds.add(task.getId());
            }
        }
        
        for (String taskId : oldCompletedTaskIds) {
            tasks.remove(taskId);
        }
        
        if (!oldCompletedTaskIds.isEmpty()) {
            log.debug("Cleaned up {} old completed tasks (older than 30 minutes)", oldCompletedTaskIds.size());
        }
    }
    
    /**
     * Get task queue statistics
     */
    public TaskQueueStats getStatistics() {
        Map<AgentTask.TaskStatus, Long> statusCounts = tasks.values().stream()
            .collect(Collectors.groupingBy(AgentTask::getStatus, Collectors.counting()));
              // For backward compatibility, count QUEUED tasks as "pending" since they're waiting to be processed
        int pendingCount = statusCounts.getOrDefault(AgentTask.TaskStatus.PENDING, 0L).intValue() +
                          statusCounts.getOrDefault(AgentTask.TaskStatus.QUEUED, 0L).intValue();
              return TaskQueueStats.builder()
            .totalTasks(tasks.size())
            .queuedTasks(statusCounts.getOrDefault(AgentTask.TaskStatus.QUEUED, 0L).intValue())
            .pendingTasks(pendingCount)
            .runningTasks(statusCounts.getOrDefault(AgentTask.TaskStatus.RUNNING, 0L).intValue())
            .completedTasks(statusCounts.getOrDefault(AgentTask.TaskStatus.COMPLETED, 0L).intValue())
            .failedTasks(statusCounts.getOrDefault(AgentTask.TaskStatus.FAILED, 0L).intValue())
            .statusCounts(statusCounts)
            .build();
    }
}
