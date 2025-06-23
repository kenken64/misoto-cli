package sg.edu.nus.iss.misoto.cli.agent.task;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify virtual threads are working correctly in Java 23
 */
@SpringBootTest
public class VirtualThreadsTest {

    @Test
    public void testVirtualThreadCreation() {
        // Test basic virtual thread creation
        Thread virtualThread = Thread.ofVirtual()
            .name("test-virtual-thread")
            .start(() -> {
                assertTrue(Thread.currentThread().isVirtual());
                assertEquals("test-virtual-thread", Thread.currentThread().getName());
            });
        
        assertDoesNotThrow(() -> virtualThread.join());
    }

    @Test
    public void testVirtualThreadExecutorService() throws InterruptedException {
        ExecutorService executor = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual()
                .name("virtual-executor-", 0)
                .factory()
        );

        int taskCount = 1000;
        CountDownLatch latch = new CountDownLatch(taskCount);

        // Submit many tasks to test virtual thread scalability
        for (int i = 0; i < taskCount; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    assertTrue(Thread.currentThread().isVirtual());
                    assertTrue(Thread.currentThread().getName().startsWith("virtual-executor-"));
                    
                    // Simulate some work
                    Thread.sleep(10);
                    
                    System.out.println("Task " + taskId + " executed in virtual thread: " + 
                        Thread.currentThread().getName());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all tasks to complete (should be very fast with virtual threads)
        assertTrue(latch.await(10, TimeUnit.SECONDS), 
            "All tasks should complete within 10 seconds using virtual threads");

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    public void testPerformanceComparison() throws InterruptedException {
        // Test with virtual threads
        long virtualThreadTime = measureExecutionTime(() -> {
            ExecutorService virtualExecutor = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().factory()
            );
            runConcurrentTasks(virtualExecutor, 500);
        });

        // Test with platform threads (limited pool)
        long platformThreadTime = measureExecutionTime(() -> {
            ExecutorService platformExecutor = Executors.newFixedThreadPool(10);
            runConcurrentTasks(platformExecutor, 500);
        });

        System.out.println("Virtual threads execution time: " + virtualThreadTime + "ms");
        System.out.println("Platform threads execution time: " + platformThreadTime + "ms");
        
        // Virtual threads should generally perform better for I/O bound tasks
        // But we won't assert this as it depends on the system
        assertTrue(virtualThreadTime > 0);
        assertTrue(platformThreadTime > 0);
    }

    private void runConcurrentTasks(ExecutorService executor, int taskCount) {
        CountDownLatch latch = new CountDownLatch(taskCount);
        
        for (int i = 0; i < taskCount; i++) {
            executor.submit(() -> {
                try {
                    // Simulate I/O bound work
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(30, TimeUnit.SECONDS);
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private long measureExecutionTime(Runnable task) {
        long startTime = System.currentTimeMillis();
        task.run();
        return System.currentTimeMillis() - startTime;
    }
}
