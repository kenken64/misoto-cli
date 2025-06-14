package sg.edu.nus.iss.misoto.cli.telemetry;

/**
 * Telemetry event types
 * 
 * Defines the types of events that can be recorded by the telemetry system.
 */
public enum TelemetryEventType {
    
    // Session events
    SESSION_START("session_start"),
    SESSION_END("session_end"),
    
    // CLI events
    CLI_START("cli_start"),
    CLI_EXIT("cli_exit"),
    
    // Command events
    COMMAND_EXECUTE("command_execute"),
    COMMAND_SUCCESS("command_success"),
    COMMAND_ERROR("command_error"),
    
    // AI events
    AI_REQUEST("ai_request"),
    AI_RESPONSE("ai_response"),
    AI_ERROR("ai_error"),
    
    // Authentication events
    AUTH_SUCCESS("auth_success"),
    AUTH_FAILURE("auth_failure"),
    AUTH_LOGOUT("auth_logout"),
    
    // File operation events
    FILE_OPERATION("file_operation"),
    FILE_READ("file_read"),
    FILE_WRITE("file_write"),
    FILE_DELETE("file_delete"),
    
    // Codebase analysis events
    CODEBASE_ANALYSIS("codebase_analysis"),
    DEPENDENCY_ANALYSIS("dependency_analysis"),
    FILE_SEARCH("file_search"),
    
    // Configuration events
    CONFIG_LOAD("config_load"),
    CONFIG_SAVE("config_save"),
    CONFIG_CHANGE("config_change"),
    
    // Error events
    ERROR_OCCURRED("error_occurred"),
    EXCEPTION_THROWN("exception_thrown"),
    
    // Performance events
    PERFORMANCE_METRIC("performance_metric"),
    MEMORY_USAGE("memory_usage"),
    EXECUTION_TIME("execution_time"),
    
    // Feature usage events
    FEATURE_USAGE("feature_usage"),
    FEATURE_DISCOVERY("feature_discovery"),
    
    // User interaction events
    USER_INPUT("user_input"),
    USER_PROMPT("user_prompt"),
    
    // System events
    SYSTEM_INFO("system_info"),
    ENVIRONMENT_INFO("environment_info");
    
    private final String eventName;
    
    TelemetryEventType(String eventName) {
        this.eventName = eventName;
    }
    
    public String getEventName() {
        return eventName;
    }
    
    @Override
    public String toString() {
        return eventName;
    }
    
    /**
     * Get event type from string name
     */
    public static TelemetryEventType fromString(String eventName) {
        for (TelemetryEventType type : values()) {
            if (type.eventName.equals(eventName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown telemetry event type: " + eventName);
    }
}
