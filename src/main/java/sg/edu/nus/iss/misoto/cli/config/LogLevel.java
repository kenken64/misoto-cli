package sg.edu.nus.iss.misoto.cli.config;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Log Level Enumeration
 */
public enum LogLevel {
    ERROR("error"),
    WARN("warn"),
    INFO("info"),
    DEBUG("debug"),
    TRACE("trace");
    
    private final String value;
    
    LogLevel(String value) {
        this.value = value;
    }
    
    @JsonValue
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
    
    /**
     * Parse log level from string
     */
    public static LogLevel fromString(String value) {
        if (value == null) return INFO;
        
        for (LogLevel level : values()) {
            if (level.value.equalsIgnoreCase(value)) {
                return level;
            }
        }
        
        return INFO; // Default fallback
    }
    
    /**
     * Check if this level is enabled for the given target level
     */
    public boolean isEnabledFor(LogLevel targetLevel) {
        return this.ordinal() <= targetLevel.ordinal();
    }
}
