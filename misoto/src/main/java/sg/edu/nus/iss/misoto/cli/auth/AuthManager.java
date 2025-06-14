package sg.edu.nus.iss.misoto.cli.auth;

import org.springframework.stereotype.Service;
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
        
        // Try to load existing token
        loadToken();
        initialized = true;
        
        log.debug("AuthManager initialized, authenticated: {}", isAuthenticated());
    }
    
    /**
     * Check if user is authenticated
     */
    public boolean isAuthenticated() {
        return authToken != null && !authToken.trim().isEmpty();
    }
    
    /**
     * Get the current authentication token
     */
    public Optional<String> getToken() {
        return Optional.ofNullable(authToken);
    }
    
    /**
     * Set the authentication token
     */
    public void setToken(String token) throws IOException {
        this.authToken = token;
        saveToken(token);
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
        return Optional.of("claude-user");
    }
}
