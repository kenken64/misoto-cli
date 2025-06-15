package sg.edu.nus.iss.misoto.cli.auth;

import org.springframework.stereotype.Service;
import sg.edu.nus.iss.misoto.cli.config.DotenvLoader;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Service for managing authentication
 */
@Service
@Slf4j
public class AuthManager {
    
    private static final String CONFIG_DIR = ".claude-code";
    private static final String TOKEN_FILE = "auth.token";
    
    private String authToken;
    private boolean initialized = false;
      /**
     * Initialize the authentication manager
     */
    public void initialize() throws IOException {
        if (initialized) {
            return;
        }
        
        // Ensure dotenv is loaded
        DotenvLoader.initialize();
        
        // Try to load existing token
        loadToken();
        initialized = true;
        
        log.debug("AuthManager initialized, authenticated: {}", isAuthenticated());
    }
      /**
     * Check if user is authenticated
     * User is considered authenticated if:
     * 1. They have a stored auth token, OR
     * 2. They have a valid ANTHROPIC_API_KEY environment variable
     */
    public boolean isAuthenticated() {
        // Check stored token first
        if (authToken != null && !authToken.trim().isEmpty()) {
            log.debug("User authenticated via stored token");
            return true;
        }
        
        // Check environment variable as fallback
        String apiKey = DotenvLoader.getEnv("ANTHROPIC_API_KEY");
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            log.debug("User authenticated via ANTHROPIC_API_KEY environment variable");
            return true;
        }
        
        log.debug("User not authenticated - no stored token or API key found");
        return false;
    }
      /**
     * Get the current authentication token
     * Returns stored token if available, otherwise returns API key from environment
     */
    public Optional<String> getToken() {
        // Return stored token if available
        if (authToken != null && !authToken.trim().isEmpty()) {
            return Optional.of(authToken);
        }
        
        // Fallback to environment variable
        String apiKey = DotenvLoader.getEnv("ANTHROPIC_API_KEY");
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            return Optional.of(apiKey);
        }
        
        return Optional.empty();
    }
      /**
     * Set the authentication token
     */
    public void setToken(String token) throws IOException {
        this.authToken = token;
        if (token != null) {
            saveToken(token);
        } else {
            clearToken();
        }
        log.debug("Authentication token updated");
    }
    
    /**
     * Clear the authentication token
     */
    public void clearToken() throws IOException {
        this.authToken = null;
        
        Path tokenPath = getTokenPath();
        if (Files.exists(tokenPath)) {
            Files.delete(tokenPath);
        }
        
        log.debug("Authentication token cleared");
    }
    
    /**
     * Load token from file
     */
    private void loadToken() {
        try {
            Path tokenPath = getTokenPath();
            if (Files.exists(tokenPath)) {
                authToken = Files.readString(tokenPath).trim();
                log.debug("Loaded authentication token from file");
            }
        } catch (IOException e) {
            log.warn("Failed to load authentication token: {}", e.getMessage());
        }
    }
    
    /**
     * Save token to file
     */
    private void saveToken(String token) throws IOException {
        Path configDir = getConfigDir();
        if (!Files.exists(configDir)) {
            Files.createDirectories(configDir);
        }
        
        Path tokenPath = getTokenPath();
        Files.writeString(tokenPath, token);
        
        // Set restrictive permissions on Unix-like systems
        try {
            Files.setPosixFilePermissions(tokenPath, 
                java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException e) {
            // Windows doesn't support POSIX permissions, ignore
        }
    }
    
    /**
     * Get the configuration directory path
     */
    private Path getConfigDir() {
        return Paths.get(System.getProperty("user.home"), CONFIG_DIR);
    }
    
    /**
     * Get the token file path
     */
    private Path getTokenPath() {
        return getConfigDir().resolve(TOKEN_FILE);
    }
      /**
     * Get the current user (simplified - in a real implementation this might decode from JWT)
     */
    public Optional<String> getCurrentUser() {
        if (!isAuthenticated()) {
            return Optional.empty();
        }
        
        // For now, return a generic user identifier
        // In a real implementation, you might decode this from the JWT token
        return Optional.of("authenticated_user");
    }
}
