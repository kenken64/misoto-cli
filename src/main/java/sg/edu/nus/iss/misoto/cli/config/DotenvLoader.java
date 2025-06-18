package sg.edu.nus.iss.misoto.cli.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for loading environment variables from .env files
 */
@Slf4j
public class DotenvLoader {
    
    private static Dotenv dotenv;
    private static boolean initialized = false;
    
    /**
     * Initialize the dotenv loader
     * Looks for .env file in current directory and parent directories
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        
        try {
            // Try to find .env file starting from current directory
            Path currentPath = Paths.get("").toAbsolutePath();
            Path envFile = findEnvFile(currentPath);
            
            if (envFile != null) {
                log.debug("Loading .env file from: {}", envFile.toAbsolutePath());
                
                // Load the .env file
                dotenv = Dotenv.configure()
                    .directory(envFile.getParent().toString())
                    .filename(".env")
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();
                
                log.debug("Successfully loaded .env file with {} entries", dotenv.entries().size());
                
                // Set all dotenv entries as system properties so Spring can resolve them
                dotenv.entries().forEach(entry -> {
                    if (System.getProperty(entry.getKey()) == null) {
                        System.setProperty(entry.getKey(), entry.getValue());
                        log.debug("Set system property: {} = {}", entry.getKey(), 
                            entry.getKey().contains("KEY") || entry.getKey().contains("TOKEN") ? "***" : entry.getValue());
                    }
                });
                
                // Log which important keys were found (without values)
                logKeyPresence("ANTHROPIC_API_KEY");
                logKeyPresence("ANTHROPIC_MODEL");
                logKeyPresence("CLAUDE_API_URL");
                logKeyPresence("CLAUDE_LOG_LEVEL");
                
            } else {
                log.debug("No .env file found, using system environment variables only");
                
                // Create empty dotenv to avoid null checks
                dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .ignoreIfMalformed()
                    .load();
            }
            
        } catch (DotenvException e) {
            log.warn("Failed to load .env file: {}", e.getMessage());
            
            // Create empty dotenv as fallback
            dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .ignoreIfMalformed()
                .load();
        }
        
        initialized = true;
    }
    
    /**
     * Get environment variable value, checking .env file first, then system environment
     */
    public static String getEnv(String key) {
        initialize();
        
        // First try to get from .env file
        String value = dotenv.get(key);
        if (value != null) {
            log.debug("Environment variable '{}' loaded from .env file (length: {} characters)", key, value.length());
            return value;
        }
        
        // Fallback to system environment
        value = System.getenv(key);
        if (value != null) {
            log.debug("Environment variable '{}' loaded from system environment (length: {} characters)", key, value.length());
        } else {
            log.debug("Environment variable '{}' not found", key);
        }
        
        return value;
    }
    
    /**
     * Get environment variable value with default
     */
    public static String getEnv(String key, String defaultValue) {
        String value = getEnv(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Check if environment variable exists
     */
    public static boolean hasEnv(String key) {
        return getEnv(key) != null;
    }
    
    /**
     * Find .env file by searching current directory and parent directories
     */
    private static Path findEnvFile(Path startPath) {
        Path currentPath = startPath;
        
        // Search up to 5 levels up the directory tree
        for (int i = 0; i < 5; i++) {
            Path envFile = currentPath.resolve(".env");
            if (Files.exists(envFile) && Files.isRegularFile(envFile)) {
                return envFile;
            }
            
            Path parent = currentPath.getParent();
            if (parent == null) {
                break;
            }
            currentPath = parent;
        }
        
        return null;
    }
    
    /**
     * Log presence of a key without revealing its value
     */
    private static void logKeyPresence(String key) {
        if (dotenv.get(key) != null) {
            log.debug("Found '{}' in .env file", key);
        }
    }
}
