package sg.edu.nus.iss.misoto.cli.agent.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.misoto.cli.agent.config.AgentConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages persistent state for the agent system
 */
@Service
@Slf4j
public class AgentStateManager {
    
    private final ObjectMapper objectMapper;
    private final Map<String, Object> state = new ConcurrentHashMap<>();
    private final Map<String, Object> memory = new ConcurrentHashMap<>();
    private final List<AgentStateEntry> history = Collections.synchronizedList(new ArrayList<>());
    
    private AgentConfiguration.StatePersistence config;
    private ScheduledExecutorService backupScheduler;
    private String stateFilePath;
    
    public AgentStateManager() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    /**
     * Initialize the state manager with configuration
     */
    public void initialize(AgentConfiguration.StatePersistence config) {
        this.config = config;
        this.stateFilePath = resolveStateFilePath(config.getFilePath());
        
        // Load existing state
        loadState();
        
        // Setup automatic backup if enabled
        if (config.isEnabled()) {
            setupBackupScheduler();
        }
        
        log.info("Agent state manager initialized with persistence: {}", config.isEnabled());
    }
    
    /**
     * Store a value in the agent state
     */
    public void setState(String key, Object value) {
        Object oldValue = state.put(key, value);
        
        // Add to history
        addHistoryEntry(AgentStateEntry.StateChangeType.STATE_UPDATE, key, oldValue, value);
        
        log.debug("Agent state updated: {} = {}", key, value);
    }
    
    /**
     * Get a value from the agent state
     */
    @SuppressWarnings("unchecked")
    public <T> T getState(String key, Class<T> type) {
        Object value = state.get(key);
        if (value == null) {
            return null;
        }
        
        if (type.isInstance(value)) {
            return (T) value;
        }
        
        // Try to convert using Jackson
        try {
            return objectMapper.convertValue(value, type);
        } catch (Exception e) {
            log.warn("Failed to convert state value for key '{}' to type {}: {}", key, type.getSimpleName(), e.getMessage());
            return null;
        }
    }
    
    /**
     * Get a value from the agent state with default
     */
    public <T> T getState(String key, Class<T> type, T defaultValue) {
        T value = getState(key, type);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Remove a value from the agent state
     */
    public void removeState(String key) {
        Object oldValue = state.remove(key);
        if (oldValue != null) {
            addHistoryEntry(AgentStateEntry.StateChangeType.STATE_REMOVED, key, oldValue, null);
        }
    }
    
    /**
     * Store a value in agent memory (temporary, not persisted)
     */
    public void setMemory(String key, Object value) {
        memory.put(key, value);
        log.debug("Agent memory updated: {} = {}", key, value);
    }
    
    /**
     * Get a value from agent memory
     */
    @SuppressWarnings("unchecked")
    public <T> T getMemory(String key, Class<T> type) {
        Object value = memory.get(key);
        if (value == null) {
            return null;
        }
        
        if (type.isInstance(value)) {
            return (T) value;
        }
        
        try {
            return objectMapper.convertValue(value, type);
        } catch (Exception e) {
            log.warn("Failed to convert memory value for key '{}' to type {}: {}", key, type.getSimpleName(), e.getMessage());
            return null;
        }
    }
    
    /**
     * Get agent context (combination of state and memory)
     */
    public Map<String, Object> getContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("state", new HashMap<>(state));
        context.put("memory", new HashMap<>(memory));
        context.put("history_count", history.size());
        context.put("last_updated", getState("last_updated", String.class));
        return context;
    }
    
    /**
     * Get recent history entries
     */
    public List<AgentStateEntry> getRecentHistory(int limit) {
        synchronized (history) {
            int size = history.size();
            int start = Math.max(0, size - limit);
            return new ArrayList<>(history.subList(start, size));
        }
    }
    
    /**
     * Clear all state and memory
     */
    public void clearAll() {
        state.clear();
        memory.clear();
        addHistoryEntry(AgentStateEntry.StateChangeType.STATE_CLEARED, "all", null, null);
        log.info("Agent state and memory cleared");
    }
    
    /**
     * Save state to file
     */
    public synchronized void saveState() {
        if (!config.isEnabled()) {
            return;
        }
        
        try {
            Path statePath = Paths.get(stateFilePath);
            Files.createDirectories(statePath.getParent());
            
            // Update last saved timestamp
            setState("last_saved", Instant.now().toString());
            
            // Create state snapshot
            AgentStateSnapshot snapshot = AgentStateSnapshot.builder()
                .timestamp(Instant.now())
                .state(new HashMap<>(state))
                .recentHistory(getRecentHistory(100)) // Save last 100 history entries
                .build();
            
            // Write to file
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(statePath.toFile(), snapshot);
            
            log.debug("Agent state saved to: {}", stateFilePath);
            
        } catch (IOException e) {
            log.error("Failed to save agent state: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Load state from file
     */
    public synchronized void loadState() {
        if (!config.isEnabled()) {
            return;
        }
        
        try {
            Path statePath = Paths.get(stateFilePath);
            if (!Files.exists(statePath)) {
                log.debug("No existing state file found at: {}", stateFilePath);
                return;
            }
            
            AgentStateSnapshot snapshot = objectMapper.readValue(statePath.toFile(), AgentStateSnapshot.class);
            
            // Restore state
            state.clear();
            state.putAll(snapshot.getState());
            
            // Restore recent history
            history.clear();
            history.addAll(snapshot.getRecentHistory());
            
            // Cleanup old history if needed
            cleanupHistory();
            
            log.info("Agent state loaded from: {} (timestamp: {})", stateFilePath, snapshot.getTimestamp());
            
        } catch (IOException e) {
            log.error("Failed to load agent state: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Backup current state
     */
    public void backupState() {
        if (!config.isEnabled()) {
            return;
        }
        
        try {
            String backupPath = stateFilePath + ".backup." + Instant.now().getEpochSecond();
            Path backupFile = Paths.get(backupPath);
            
            AgentStateSnapshot snapshot = AgentStateSnapshot.builder()
                .timestamp(Instant.now())
                .state(new HashMap<>(state))
                .recentHistory(getRecentHistory(config.getMaxHistoryEntries()))
                .build();
            
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(backupFile.toFile(), snapshot);
            
            log.debug("Agent state backed up to: {}", backupPath);
            
        } catch (IOException e) {
            log.error("Failed to backup agent state: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Shutdown the state manager
     */
    public void shutdown() {
        if (backupScheduler != null && !backupScheduler.isShutdown()) {
            backupScheduler.shutdown();
            try {
                if (!backupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    backupScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                backupScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // Final save
        saveState();
        log.info("Agent state manager shutdown");
    }
    
    private void addHistoryEntry(AgentStateEntry.StateChangeType type, String key, Object oldValue, Object newValue) {
        AgentStateEntry entry = AgentStateEntry.builder()
            .timestamp(Instant.now())
            .changeType(type)
            .key(key)
            .oldValue(oldValue)
            .newValue(newValue)
            .build();
            
        history.add(entry);
        
        // Cleanup if too many entries
        cleanupHistory();
    }
    
    private void cleanupHistory() {
        synchronized (history) {
            while (history.size() > config.getMaxHistoryEntries()) {
                history.remove(0);
            }
        }
    }
    
    private String resolveStateFilePath(String configPath) {
        if (configPath.startsWith("/") || configPath.contains(":")) {
            // Absolute path
            return configPath;
        } else {
            // Relative path - resolve from user home
            return System.getProperty("user.home") + "/" + configPath;
        }
    }
    
    private void setupBackupScheduler() {
        if (backupScheduler != null) {
            return;
        }
        
        backupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "agent-state-backup");
            t.setDaemon(true);
            return t;
        });
        
        long intervalSeconds = config.getBackupInterval().getSeconds();
        backupScheduler.scheduleAtFixedRate(
            this::saveState,
            intervalSeconds,
            intervalSeconds,
            TimeUnit.SECONDS
        );
        
        log.debug("State backup scheduler started with interval: {}s", intervalSeconds);
    }
    
    /**
     * Initialize the agent state
     */
    public void initializeState() {
        log.info("Initializing agent state manager");
        loadState();
        
        // Set initial state if not already set
        if (state.isEmpty()) {
            state.put("agent_id", UUID.randomUUID().toString());
            state.put("start_time", Instant.now().toString());
            state.put("total_tasks_executed", "0");
            state.put("successful_tasks", "0");
            state.put("failed_tasks", "0");
            state.put("cycle_count", "0");
        }
        
        // Update last activity
        updateLastActivity(LocalDateTime.now());
    }
    
    /**
     * Update last activity timestamp
     */
    public void updateLastActivity(LocalDateTime timestamp) {
        state.put("last_activity", timestamp.toString());
    }
      /**
     * Get current state snapshot
     */    public AgentStateSnapshot getCurrentState() {
        return AgentStateSnapshot.builder()
            .timestamp(Instant.now())
            .state(new HashMap<>(state))
            .totalTasksExecuted(parseLong(state.get("total_tasks_executed")))
            .failedTasks(parseLong(state.get("failed_tasks")))
            .lastActivity(parseInstant(state.get("last_activity")))
            .recentHistory(new ArrayList<>(history))
            .build();
    }
    
    /**
     * Get agent uptime in seconds
     */
    public long getUptime() {
        String startTimeStr = (String) state.get("start_time");
        if (startTimeStr == null) {
            return 0;
        }
        
        Instant startTime = parseInstant(startTimeStr);
        return Duration.between(startTime, Instant.now()).getSeconds();
    }
    
    /**
     * Get cycle count
     */
    public int getCycleCount() {
        return parseInt(state.get("cycle_count"));
    }
    
    /**
     * Update statistics
     */
    public void updateStatistics(int completedTasks, int failedTasks, int pendingTasks) {
        // Don't overwrite total_tasks_executed with current queue count since 
        // completed tasks are cleaned up from the queue immediately
        state.put("failed_tasks", String.valueOf(failedTasks));
        state.put("pending_tasks", String.valueOf(pendingTasks));
        
        // Increment cycle count
        int currentCycles = parseInt(state.get("cycle_count"));
        state.put("cycle_count", String.valueOf(currentCycles + 1));
    }
    
    /**
     * Increment total tasks executed counter
     */
    public void incrementTotalTasksExecuted() {
        int currentTotal = parseInt(state.get("total_tasks_executed"));
        state.put("total_tasks_executed", String.valueOf(currentTotal + 1));
    }
    
    // Helper methods
    private Instant parseInstant(Object obj) {
        if (obj == null) return Instant.now();
        if (obj instanceof Instant) return (Instant) obj;
        if (obj instanceof String) return parseInstant((String) obj);
        return Instant.now();
    }
    
    private Instant parseInstant(String str) {
        try {
            return str != null ? Instant.parse(str) : Instant.now();
        } catch (Exception e) {
            return Instant.now();
        }
    }
    
    private LocalDateTime parseLocalDateTime(String str) {
        try {
            return str != null ? LocalDateTime.parse(str) : LocalDateTime.now();
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
    
    private int parseInt(Object obj) {
        try {
            if (obj == null) return 0;
            if (obj instanceof Integer) return (Integer) obj;
            if (obj instanceof String) return parseInt((String) obj);
            return 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    private int parseInt(String str) {
        try {
            return str != null ? Integer.parseInt(str) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    private long parseLong(Object obj) {
        try {
            if (obj == null) return 0L;
            if (obj instanceof Long) return (Long) obj;
            if (obj instanceof Integer) return ((Integer) obj).longValue();
            if (obj instanceof String) return Long.parseLong((String) obj);
            return 0L;
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
