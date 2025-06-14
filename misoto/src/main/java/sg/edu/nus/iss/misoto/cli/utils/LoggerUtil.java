package sg.edu.nus.iss.misoto.cli.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Logger Utilities
 * 
 * Provides a consistent logging interface across the application.
 * Supports multiple log levels, formatting, and output destinations.
 */
@Component
public class LoggerUtil {
    
    private static final Logger log = LoggerFactory.getLogger(LoggerUtil.class);
    
    /**
     * Log levels
     */
    public enum LogLevel {
        DEBUG(0),
        INFO(1), 
        WARN(2),
        ERROR(3),
        SILENT(4);
        
        private final int value;
        
        LogLevel(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
        
        public boolean isGreaterOrEqualTo(LogLevel other) {
            return this.value >= other.value;
        }
    }
    
    /**
     * Logger configuration
     */
    public static class LoggerConfig {
        private LogLevel level = LogLevel.INFO;
        private boolean timestamps = true;
        private boolean colors = true;
        private boolean verbose = false;
        
        // Constructors
        public LoggerConfig() {}
        
        public LoggerConfig(LogLevel level, boolean timestamps, boolean colors, boolean verbose) {
            this.level = level;
            this.timestamps = timestamps;
            this.colors = colors;
            this.verbose = verbose;
        }
        
        // Getters and setters
        public LogLevel getLevel() { return level; }
        public void setLevel(LogLevel level) { this.level = level; }
        
        public boolean isTimestamps() { return timestamps; }
        public void setTimestamps(boolean timestamps) { this.timestamps = timestamps; }
        
        public boolean isColors() { return colors; }
        public void setColors(boolean colors) { this.colors = colors; }
        
        public boolean isVerbose() { return verbose; }
        public void setVerbose(boolean verbose) { this.verbose = verbose; }
    }
    
    private LoggerConfig config;
    
    public LoggerUtil() {
        this.config = new LoggerConfig();
    }
    
    public LoggerUtil(LoggerConfig config) {
        this.config = config != null ? config : new LoggerConfig();
    }
    
    /**
     * Configure the logger
     */
    public void configure(LoggerConfig config) {
        this.config = config;
    }
    
    /**
     * Set log level
     */
    public void setLevel(LogLevel level) {
        this.config.setLevel(level);
    }
    
    /**
     * Log a debug message
     */
    public void debug(String message) {
        debug(message, null);
    }
    
    public void debug(String message, Object context) {
        logMessage(message, LogLevel.DEBUG, context);
    }
    
    /**
     * Log an info message
     */
    public void info(String message) {
        info(message, null);
    }
    
    public void info(String message, Object context) {
        logMessage(message, LogLevel.INFO, context);
    }
    
    /**
     * Log a warning message
     */
    public void warn(String message) {
        warn(message, null);
    }
    
    public void warn(String message, Object context) {
        logMessage(message, LogLevel.WARN, context);
    }
    
    /**
     * Log an error message
     */
    public void error(String message) {
        error(message, null);
    }
    
    public void error(String message, Object context) {
        logMessage(message, LogLevel.ERROR, context);
    }
    
    /**
     * Log an error with exception
     */
    public void error(String message, Throwable throwable) {
        logMessage(message, LogLevel.ERROR, throwable);
    }
    
    /**
     * Log a message with level
     */
    private void logMessage(String message, LogLevel level, Object context) {
        // Check if this log level should be displayed
        if (!level.isGreaterOrEqualTo(config.getLevel())) {
            return;
        }
        
        // Format the message
        String formattedMessage = formatMessage(message, level, context);
        
        // Send to appropriate logger
        logToConsole(formattedMessage, level, context);
    }
    
    /**
     * Format a log message
     */
    private String formatMessage(String message, LogLevel level, Object context) {
        StringBuilder result = new StringBuilder();
        
        // Add timestamp if enabled
        if (config.isTimestamps()) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            result.append("[").append(timestamp).append("] ");
        }
        
        // Add log level
        result.append(getLevelName(level)).append(": ");
        
        // Add message
        result.append(message);
        
        // Add context if verbose and context is provided
        if (config.isVerbose() && context != null) {
            try {
                if (context instanceof Throwable) {
                    result.append(" ").append(((Throwable) context).getMessage());
                } else {
                    result.append(" ").append(context.toString());
                }
            } catch (Exception e) {
                result.append(" [Context serialization failed]");
            }
        }
        
        return result.toString();
    }
    
    /**
     * Get the name of a log level
     */
    private String getLevelName(LogLevel level) {
        switch (level) {
            case DEBUG:
                return colorize("DEBUG", "\u001b[36m"); // Cyan
            case INFO:
                return colorize("INFO", "\u001b[32m");  // Green
            case WARN:
                return colorize("WARN", "\u001b[33m");  // Yellow
            case ERROR:
                return colorize("ERROR", "\u001b[31m"); // Red
            default:
                return "UNKNOWN";
        }
    }
    
    /**
     * Colorize a string if colors are enabled
     */
    private String colorize(String text, String colorCode) {
        if (!config.isColors()) {
            return text;
        }
        
        return colorCode + text + "\u001b[0m";
    }
    
    /**
     * Log to console
     */
    private void logToConsole(String message, LogLevel level, Object context) {
        switch (level) {
            case DEBUG:
                if (context instanceof Throwable) {
                    log.debug(message, (Throwable) context);
                } else {
                    log.debug(message);
                }
                break;
            case INFO:
                if (context instanceof Throwable) {
                    log.info(message, (Throwable) context);
                } else {
                    log.info(message);
                }
                break;
            case WARN:
                if (context instanceof Throwable) {
                    log.warn(message, (Throwable) context);
                } else {
                    log.warn(message);
                }
                break;
            case ERROR:
                if (context instanceof Throwable) {
                    log.error(message, (Throwable) context);
                } else {
                    log.error(message);
                }
                break;
        }
    }
}
