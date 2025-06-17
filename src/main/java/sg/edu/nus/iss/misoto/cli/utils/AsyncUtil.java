package sg.edu.nus.iss.misoto.cli.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Async Utilities
 * 
 * Provides utilities for handling asynchronous operations,
 * timeouts, retries, and other async patterns.
 */
@Slf4j
public class AsyncUtil {
    
    /**
     * Options for retry operations
     */
    public static class RetryOptions {
        private int maxRetries = 3;
        private long initialDelayMs = 1000;
        private long maxDelayMs = 10000;
        private boolean backoff = true;
        private java.util.function.Predicate<Exception> isRetryable;
        private java.util.function.BiConsumer<Exception, Integer> onRetry;
        
        public RetryOptions() {}
        
        public RetryOptions(int maxRetries, long initialDelayMs, long maxDelayMs) {
            this.maxRetries = maxRetries;
            this.initialDelayMs = initialDelayMs;
            this.maxDelayMs = maxDelayMs;
        }
        
        // Getters and setters
        public int getMaxRetries() { return maxRetries; }
        public RetryOptions setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; return this; }
        
        public long getInitialDelayMs() { return initialDelayMs; }
        public RetryOptions setInitialDelayMs(long initialDelayMs) { this.initialDelayMs = initialDelayMs; return this; }
        
        public long getMaxDelayMs() { return maxDelayMs; }
        public RetryOptions setMaxDelayMs(long maxDelayMs) { this.maxDelayMs = maxDelayMs; return this; }
        
        public boolean isBackoff() { return backoff; }
        public RetryOptions setBackoff(boolean backoff) { this.backoff = backoff; return this; }
        
        public java.util.function.Predicate<Exception> getIsRetryable() { return isRetryable; }
        public RetryOptions setIsRetryable(java.util.function.Predicate<Exception> isRetryable) { this.isRetryable = isRetryable; return this; }
        
        public java.util.function.BiConsumer<Exception, Integer> getOnRetry() { return onRetry; }
        public RetryOptions setOnRetry(java.util.function.BiConsumer<Exception, Integer> onRetry) { this.onRetry = onRetry; return this; }
    }
    
    /**
     * Deferred result holder
     */
    public static class Deferred<T> {
        private final CompletableFuture<T> future = new CompletableFuture<>();
        
        public void resolve(T value) {
            future.complete(value);
        }
        
        public void reject(Throwable error) {
            future.completeExceptionally(error);
        }
        
        public CompletableFuture<T> getPromise() {
            return future;
        }
    }
      /**
     * Sleep for the specified number of milliseconds
     */
    public static CompletableFuture<Void> delay(long ms) {
        if (ms <= 0) {
            return CompletableFuture.completedFuture(null);
        }
        
        CompletableFuture<Void> future = new CompletableFuture<>();
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        
        executor.schedule(() -> {
            future.complete(null);
            executor.shutdown();
        }, ms, TimeUnit.MILLISECONDS);
        
        return future;
    }
    
    /**
     * Execute a function with a timeout
     */
    public static <T> CompletableFuture<T> withTimeout(Supplier<CompletableFuture<T>> supplier, long timeoutMs) {
        CompletableFuture<T> future = supplier.get();
        CompletableFuture<T> timeoutFuture = new CompletableFuture<>();
        
        ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
        
        // Set up timeout
        ScheduledFuture<?> timeoutTask = timeoutExecutor.schedule(() -> {
            timeoutFuture.completeExceptionally(new TimeoutException("Operation timed out after " + timeoutMs + "ms"));
        }, timeoutMs, TimeUnit.MILLISECONDS);
        
        // Complete when either the original future completes or timeout occurs
        future.whenComplete((result, throwable) -> {
            timeoutTask.cancel(false);
            timeoutExecutor.shutdown();
            
            if (throwable != null) {
                timeoutFuture.completeExceptionally(throwable);
            } else {
                timeoutFuture.complete(result);
            }
        });
        
        return timeoutFuture;
    }
    
    /**
     * Execute a function with retry logic
     */
    public static <T> CompletableFuture<T> withRetry(Supplier<CompletableFuture<T>> supplier) {
        return withRetry(supplier, new RetryOptions());
    }
    
    /**
     * Execute a function with retry logic and custom options
     */
    public static <T> CompletableFuture<T> withRetry(Supplier<CompletableFuture<T>> supplier, RetryOptions options) {
        return withRetryInternal(supplier, options, 0);
    }
    
    private static <T> CompletableFuture<T> withRetryInternal(Supplier<CompletableFuture<T>> supplier, RetryOptions options, int attempt) {
        return supplier.get().handle((result, throwable) -> {
            if (throwable == null) {
                return CompletableFuture.completedFuture(result);
            }
            
            // Check if we should retry
            if (attempt >= options.getMaxRetries()) {
                return CompletableFuture.<T>failedFuture(throwable);
            }
            
            // Check if error is retryable
            if (options.getIsRetryable() != null && throwable instanceof Exception) {
                Exception exception = (Exception) throwable;
                if (!options.getIsRetryable().test(exception)) {
                    return CompletableFuture.<T>failedFuture(throwable);
                }
            }
            
            // Calculate delay
            long delayMs = calculateDelay(options, attempt);
            
            // Call onRetry callback if provided
            if (options.getOnRetry() != null && throwable instanceof Exception) {
                try {
                    options.getOnRetry().accept((Exception) throwable, attempt + 1);
                } catch (Exception callbackError) {
                    log.warn("Error in retry callback: {}", callbackError.getMessage());
                }
            }
            
            log.debug("Retrying operation after {}ms (attempt {} of {})", delayMs, attempt + 1, options.getMaxRetries());
            
            // Retry after delay
            return delay(delayMs).thenCompose(ignored -> withRetryInternal(supplier, options, attempt + 1));
        }).thenCompose(future -> future);
    }
    
    /**
     * Calculate delay for retry with optional exponential backoff
     */
    private static long calculateDelay(RetryOptions options, int attempt) {
        if (!options.isBackoff()) {
            return options.getInitialDelayMs();
        }
        
        // Exponential backoff: delay = initialDelayMs * (2 ^ attempt)
        long delay = options.getInitialDelayMs() * (1L << attempt);
        
        // Cap at maximum delay
        return Math.min(delay, options.getMaxDelayMs());
    }
    
    /**
     * Create a deferred object for manual promise control
     */
    public static <T> Deferred<T> createDeferred() {
        return new Deferred<>();
    }
    
    /**
     * Wait for all futures to complete
     */
    @SafeVarargs
    public static CompletableFuture<Void> allOf(CompletableFuture<?>... futures) {
        return CompletableFuture.allOf(futures);
    }
    
    /**
     * Wait for any future to complete
     */
    @SafeVarargs
    public static CompletableFuture<Object> anyOf(CompletableFuture<?>... futures) {
        return CompletableFuture.anyOf(futures);
    }
    
    /**
     * Execute multiple tasks in parallel
     */
    public static <T> CompletableFuture<java.util.List<T>> parallel(java.util.List<Supplier<CompletableFuture<T>>> suppliers) {
        java.util.List<CompletableFuture<T>> futures = suppliers.stream()
                .map(Supplier::get)
                .collect(java.util.stream.Collectors.toList());
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(ignored -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(java.util.stream.Collectors.toList()));
    }
    
    /**
     * Execute tasks with limited concurrency
     */
    public static <T> CompletableFuture<java.util.List<T>> parallelLimit(
            java.util.List<Supplier<CompletableFuture<T>>> suppliers, 
            int concurrency) {
        
        if (suppliers.isEmpty()) {
            return CompletableFuture.completedFuture(java.util.Collections.emptyList());
        }
        
        if (concurrency <= 0) {
            throw new IllegalArgumentException("Concurrency must be positive");
        }
        
        java.util.List<T> results = new java.util.concurrent.CopyOnWriteArrayList<>();
        Semaphore semaphore = new Semaphore(concurrency);
        
        java.util.List<CompletableFuture<Void>> futures = suppliers.stream()
                .map(supplier -> CompletableFuture.runAsync(() -> {
                    try {
                        semaphore.acquire();
                        T result = supplier.get().join();
                        results.add(result);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    } finally {
                        semaphore.release();
                    }
                }))
                .collect(java.util.stream.Collectors.toList());
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(ignored -> results);
    }
}
