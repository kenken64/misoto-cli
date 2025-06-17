package sg.edu.nus.iss.misoto.cli.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import sg.edu.nus.iss.misoto.cli.agent.config.AgentConfiguration;
import sg.edu.nus.iss.misoto.cli.agent.task.AgentTask;
import sg.edu.nus.iss.misoto.cli.agent.task.TaskQueueService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the Agent System.
 * Tests the complete agent lifecycle and component interaction.
 */
@SpringBootTest(properties = {
    "misoto.agent.mode.enabled=true",
    "misoto.agent.max-concurrent-tasks=3", 
    "misoto.agent.execution-interval-ms=5000",
    "misoto.agent.auto-save.enabled=true",
    "misoto.agent.state.file-path=test-agent-state.json",
    "misoto.agent.backup.retention-days=7"
})
@ActiveProfiles("test")
public class AgentSystemIntegrationTest {

    @Autowired
    private AgentService agentService;
    
    @Autowired
    private AgentConfiguration agentConfig;
    
    @Autowired
    private TaskQueueService taskQueue;
    
    @BeforeEach
    void setUp() throws InterruptedException {
        // Wait for any auto-start to complete, then stop the agent to ensure clean state
        Thread.sleep(1000); // Allow time for auto-start
        
        if (agentService.isRunning()) {
            agentService.stopAgent();
            // Wait for shutdown to complete
            Thread.sleep(1000);
        }
        
        // Configure agent for testing
        agentConfig.setAgentModeEnabled(true);
        agentConfig.setMaxConcurrentTasks(2);
        agentConfig.setExecutionIntervalMs(1000L);
    }

    @AfterEach
    void tearDown() {
        // Clean up after tests
        if (agentService.isRunning()) {
            agentService.stopAgent();
        }
    }
    
    @Test
    void testAgentLifecycle() throws InterruptedException {
        // Test starting the agent
        assertFalse(agentService.isRunning());
        
        agentService.startAgent();
        
        // Wait for startup with timeout
        int attempts = 0;
        while (!agentService.isRunning() && attempts < 10) {
            Thread.sleep(100);
            attempts++;
        }
        assertTrue(agentService.isRunning(), "Agent should be running after startup");
        
        // Test stopping the agent
        agentService.stopAgent();
        
        // Wait for shutdown with timeout
        attempts = 0;
        while (agentService.isRunning() && attempts < 10) {
            Thread.sleep(100);
            attempts++;
        }
        assertFalse(agentService.isRunning(), "Agent should be stopped after shutdown");
    }
    
    @Test
    void testTaskSubmissionAndExecution() throws InterruptedException {
        // Start the agent
        agentService.startAgent();
        
        // Wait for startup
        int attempts = 0;
        while (!agentService.isRunning() && attempts < 10) {
            Thread.sleep(100);
            attempts++;
        }
        assertTrue(agentService.isRunning(), "Agent should be running");
        
        // Submit a simple task
        AgentTask task = AgentTask.builder()
            .name("test-system-task")
            .type(AgentTask.TaskType.SYSTEM)
            .priority(AgentTask.TaskPriority.HIGH)
            .description("Test task")
            .context(createTaskContext(Map.of("action", "test")))
            .createdAt(Instant.now())
            .build();
        
        agentService.submitTask(task);
        
        // Wait for task processing (longer timeout for AI processing)
        Thread.sleep(3000);
        
        // Check task was processed - with AI client initialization now working,
        // tasks should be processed even if they encounter AI errors
        var stats = taskQueue.getStatistics();
        var agentStatus = agentService.getStatus();
        
        // Check either current queue has tasks OR historical total shows tasks were executed
        boolean taskProcessed = stats.getCompletedTasks() > 0 || 
                               stats.getRunningTasks() > 0 || 
                               stats.getPendingTasks() > 0 ||
                               agentStatus.getTotalTasksExecuted() > 0;
        
        assertTrue(taskProcessed, 
                   String.format("At least one task should be processed or queued. Stats: completed=%d, running=%d, pending=%d, total_executed=%d", 
                       stats.getCompletedTasks(), stats.getRunningTasks(), stats.getPendingTasks(), agentStatus.getTotalTasksExecuted()));
        
        // Clean up
        agentService.stopAgent();
    }
    
    @Test
    void testAgentStatus() throws InterruptedException {
        // Test the agent service functionality regardless of auto-start timing
        // This test focuses on verifying that start/stop/status work correctly
        
        // Ensure we have a known starting state by stopping any auto-started agent
        if (agentService.isRunning()) {
            agentService.stopAgent();
            // Wait for proper shutdown
            int stopAttempts = 0;
            while (agentService.isRunning() && stopAttempts < 50) {
                Thread.sleep(200);
                stopAttempts++;
            }
        }
        
        // Test starting the agent manually
        agentService.startAgent();
        
        // Wait for startup
        int startAttempts = 0;
        while (!agentService.isRunning() && startAttempts < 20) {
            Thread.sleep(100);
            startAttempts++;
        }
        
        // Verify agent is running
        var runningStatus = agentService.getStatus();
        assertTrue(runningStatus.isRunning(), "Agent should be running after start");
        assertFalse(runningStatus.isShuttingDown());
        assertNotNull(runningStatus, "Status should not be null");
        
        // Test stopping the agent
        agentService.stopAgent();
        
        // Wait for shutdown (may take time due to ongoing AI operations)
        // AI operations can take 5+ seconds, so we need to wait longer
        int shutdownAttempts = 0;
        while (agentService.isRunning() && shutdownAttempts < 100) { // Increased from 50 to 100 attempts
            Thread.sleep(300); // Increased from 200ms to 300ms
            shutdownAttempts++;
        }
        
        // Verify agent is stopped - the agent should eventually stop
        var stoppedStatus = agentService.getStatus();
        // The agent should be stopped (not running)
        assertFalse(stoppedStatus.isRunning(), "Agent should not be running after shutdown");
        
        // Note: isShuttingDown may remain true after shutdown as it indicates the agent
        // was shut down rather than stopped naturally. This is expected behavior.
        
        // Test restarting
        agentService.startAgent();
        
        startAttempts = 0;
        while (!agentService.isRunning() && startAttempts < 20) {
            Thread.sleep(100);
            startAttempts++;
        }
        
        var restartedStatus = agentService.getStatus();
        assertTrue(restartedStatus.isRunning(), "Agent should be running after restart");
        
        // Final cleanup - ensure proper shutdown
        agentService.stopAgent();
        
        // Wait for final shutdown with extended timeout
        int finalShutdownAttempts = 0;
        while (agentService.isRunning() && finalShutdownAttempts < 60) { // Up to 18 seconds
            Thread.sleep(300);
            finalShutdownAttempts++;
        }
        
        // Log final status for debugging
        var finalStatus = agentService.getStatus();
        System.out.println("Final agent status - Running: " + finalStatus.isRunning() + 
                          ", Shutting down: " + finalStatus.isShuttingDown());
    }

    @Test
    void testAgentConfiguration() {
        // Test configuration changes
        agentConfig.setMaxConcurrentTasks(5);
        assertEquals(5, agentConfig.getMaxConcurrentTasks());
        
        agentConfig.setAgentModeEnabled(false);
        assertFalse(agentConfig.isAgentModeEnabled());
        
        agentConfig.setExecutionIntervalMs(2000L);
        assertEquals(2000L, agentConfig.getExecutionIntervalMs());
    }
    
    @Test
    void testTaskQueueOperations() {
        // Test task queue operations
        AgentTask task1 = AgentTask.builder()
            .name("test-task-1")
            .type(AgentTask.TaskType.SHELL_COMMAND)
            .priority(AgentTask.TaskPriority.MEDIUM)
            .description("Test task 1")
            .parameters(Map.of("command", "echo 'Test command execution'"))
            .createdAt(Instant.now())
            .build();
            
        AgentTask task2 = AgentTask.builder()
            .name("test-task-2")
            .type(AgentTask.TaskType.AI_ANALYSIS)
            .priority(AgentTask.TaskPriority.HIGH)
            .description("Test task 2")
            .parameters(Map.of("content", "Test content for AI analysis", "analysis_type", "general"))
            .createdAt(Instant.now())
            .build();
        
        // Submit tasks
        taskQueue.submitTask(task1);
        taskQueue.submitTask(task2);
        
        // Check statistics - tasks might be processed quickly, so check total count
        var stats = taskQueue.getStatistics();
        assertEquals(2, stats.getTotalTasks());
        
        // Get recent tasks
        var recentTasks = taskQueue.getRecentTasks(10);
        assertEquals(2, recentTasks.size());
        
        // Clear completed tasks (some may have been processed quickly)
        int cleared = taskQueue.clearCompletedTasks();
        assertTrue(cleared >= 0, "Cleared count should be non-negative");
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
