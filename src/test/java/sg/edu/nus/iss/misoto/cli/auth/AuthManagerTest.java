package sg.edu.nus.iss.misoto.cli.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import sg.edu.nus.iss.misoto.cli.config.DotenvLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test cases for AuthManager
 */
@DisplayName("AuthManager Tests")
class AuthManagerTest {

    private AuthManager authManager;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        authManager = new AuthManager();
    }    @Test
    @DisplayName("initialize should load existing token")
    void testInitializeWithExistingToken() throws IOException {
        // Create a mock token file
        Path configDir = tempDir.resolve(".claude-code");
        Files.createDirectories(configDir);
        Path tokenFile = configDir.resolve("auth.token");
        Files.writeString(tokenFile, "existing-token");
        
        // Use system property to set temporary home directory
        String originalHome = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());
        
        try {
            try (MockedStatic<DotenvLoader> mockedDotenv = Mockito.mockStatic(DotenvLoader.class)) {
                mockedDotenv.when(DotenvLoader::initialize).thenAnswer(invocation -> null);
                
                authManager.initialize();
                
                assertTrue(authManager.isAuthenticated());
                assertEquals(Optional.of("existing-token"), authManager.getToken());
            }
        } finally {
            // Restore original home directory
            System.setProperty("user.home", originalHome);
        }
    }

    @Test
    @DisplayName("initialize should handle missing token file")
    void testInitializeWithoutToken() throws IOException {
        // Use system property to set temporary home directory
        String originalHome = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());
        
        try {
            try (MockedStatic<DotenvLoader> mockedDotenv = Mockito.mockStatic(DotenvLoader.class)) {
                mockedDotenv.when(DotenvLoader::initialize).thenAnswer(invocation -> null);
                mockedDotenv.when(() -> DotenvLoader.getEnv("ANTHROPIC_API_KEY"))
                    .thenReturn(null);
                
                authManager.initialize();
                
                assertFalse(authManager.isAuthenticated());
                assertEquals(Optional.empty(), authManager.getToken());
            }
        } finally {
            // Restore original home directory
            System.setProperty("user.home", originalHome);
        }
    }

    @Test
    @DisplayName("isAuthenticated should return true with stored token")
    void testIsAuthenticatedWithStoredToken() throws IOException {
        authManager.setToken("test-token");
        assertTrue(authManager.isAuthenticated());
    }

    @Test
    @DisplayName("isAuthenticated should return true with environment API key")
    void testIsAuthenticatedWithEnvironmentKey() {
        try (MockedStatic<DotenvLoader> mockedDotenv = Mockito.mockStatic(DotenvLoader.class)) {
            mockedDotenv.when(() -> DotenvLoader.getEnv("ANTHROPIC_API_KEY"))
                .thenReturn("env-api-key");
            
            assertTrue(authManager.isAuthenticated());
        }
    }

    @Test
    @DisplayName("isAuthenticated should return false without any authentication")
    void testIsAuthenticatedWithoutAuth() {
        try (MockedStatic<DotenvLoader> mockedDotenv = Mockito.mockStatic(DotenvLoader.class)) {
            mockedDotenv.when(() -> DotenvLoader.getEnv("ANTHROPIC_API_KEY"))
                .thenReturn(null);
            
            assertFalse(authManager.isAuthenticated());
        }
    }

    @Test
    @DisplayName("getToken should return stored token")
    void testGetTokenWithStoredToken() throws IOException {
        authManager.setToken("stored-token");
        assertEquals(Optional.of("stored-token"), authManager.getToken());
    }

    @Test
    @DisplayName("getToken should return environment API key as fallback")
    void testGetTokenWithEnvironmentFallback() {
        try (MockedStatic<DotenvLoader> mockedDotenv = Mockito.mockStatic(DotenvLoader.class)) {
            mockedDotenv.when(() -> DotenvLoader.getEnv("ANTHROPIC_API_KEY"))
                .thenReturn("env-fallback-key");
            
            assertEquals(Optional.of("env-fallback-key"), authManager.getToken());
        }
    }

    @Test
    @DisplayName("getToken should return empty when no authentication available")
    void testGetTokenWithoutAuth() {
        try (MockedStatic<DotenvLoader> mockedDotenv = Mockito.mockStatic(DotenvLoader.class)) {
            mockedDotenv.when(() -> DotenvLoader.getEnv("ANTHROPIC_API_KEY"))
                .thenReturn(null);
            
            assertEquals(Optional.empty(), authManager.getToken());
        }
    }    @Test
    @DisplayName("setToken should store token and create directories")
    void testSetToken() throws IOException {
        String originalHome = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());
        
        try {
            authManager.setToken("new-token");
            
            // Verify token is stored in memory
            assertEquals(Optional.of("new-token"), authManager.getToken());
            
            // Verify file is created
            Path expectedTokenFile = tempDir.resolve(".claude-code").resolve("auth.token");
            assertTrue(Files.exists(expectedTokenFile));
            assertEquals("new-token", Files.readString(expectedTokenFile));
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }    @Test
    @DisplayName("setToken should handle null token")
    void testSetTokenWithNull() throws IOException {
        String originalHome = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());
        
        try {
            // First set a token
            authManager.setToken("test-token");
            assertTrue(authManager.isAuthenticated());
            
            // Then clear it with null
            authManager.setToken(null);
            
            try (MockedStatic<DotenvLoader> mockedDotenv = Mockito.mockStatic(DotenvLoader.class)) {
                mockedDotenv.when(() -> DotenvLoader.getEnv("ANTHROPIC_API_KEY"))
                    .thenReturn(null);
                
                assertFalse(authManager.isAuthenticated());
            }
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }    @Test
    @DisplayName("clearToken should remove stored token and file")
    void testClearToken() throws IOException {
        String originalHome = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());
        
        try {
            // First set a token
            authManager.setToken("token-to-clear");
            assertTrue(authManager.isAuthenticated());
            
            Path tokenFile = tempDir.resolve(".claude-code").resolve("auth.token");
            assertTrue(Files.exists(tokenFile));
            
            // Clear the token
            authManager.clearToken();
            
            // Verify token is cleared from memory
            try (MockedStatic<DotenvLoader> mockedDotenv = Mockito.mockStatic(DotenvLoader.class)) {
                mockedDotenv.when(() -> DotenvLoader.getEnv("ANTHROPIC_API_KEY"))
                    .thenReturn(null);
                
                assertFalse(authManager.isAuthenticated());
            }
            
            // Verify file is deleted
            assertFalse(Files.exists(tokenFile));
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }    @Test
    @DisplayName("clearToken should handle missing token file gracefully")
    void testClearTokenWithoutFile() throws IOException {
        String originalHome = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());
        
        try {
            // Clear token when no file exists - should not throw
            assertDoesNotThrow(() -> authManager.clearToken());
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    @Test
    @DisplayName("getCurrentUser should return simplified user info")
    void testGetCurrentUser() throws IOException {
        authManager.setToken("test-token");
        
        Optional<String> user = authManager.getCurrentUser();
        
        // Since this is simplified implementation, it just returns a fixed string
        assertTrue(user.isPresent());
        assertEquals("authenticated_user", user.get());
    }

    @Test
    @DisplayName("getCurrentUser should return empty when not authenticated")
    void testGetCurrentUserNotAuthenticated() {
        try (MockedStatic<DotenvLoader> mockedDotenv = Mockito.mockStatic(DotenvLoader.class)) {
            mockedDotenv.when(() -> DotenvLoader.getEnv("ANTHROPIC_API_KEY"))
                .thenReturn(null);
            
            Optional<String> user = authManager.getCurrentUser();
            assertFalse(user.isPresent());
        }
    }    @Test
    @DisplayName("initialize should be idempotent")
    void testInitializeIdempotent() throws IOException {
        // Set user.home to a known test directory temporarily
        String originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());
        
        try {
            try (MockedStatic<DotenvLoader> mockedDotenv = Mockito.mockStatic(DotenvLoader.class)) {
                mockedDotenv.when(DotenvLoader::initialize).thenAnswer(invocation -> null);
                
                // Initialize multiple times - should not throw
                authManager.initialize();
                authManager.initialize();
                authManager.initialize();
                
                // Should still work correctly
                assertDoesNotThrow(() -> authManager.isAuthenticated());
            }
        } finally {
            // Restore original user.home
            System.setProperty("user.home", originalUserHome);
        }
    }@Test
    @DisplayName("token handling should handle whitespace correctly")
    void testTokenWhitespaceHandling() throws IOException {
        String originalHome = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());
        
        try {
            // Test empty string token
            authManager.setToken("   ");
            
            try (MockedStatic<DotenvLoader> mockedDotenv = Mockito.mockStatic(DotenvLoader.class)) {
                mockedDotenv.when(() -> DotenvLoader.getEnv("ANTHROPIC_API_KEY"))
                    .thenReturn(null);
                
                assertFalse(authManager.isAuthenticated());
            }
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    @Test
    @DisplayName("environment API key should handle whitespace correctly")
    void testEnvironmentKeyWhitespaceHandling() {
        try (MockedStatic<DotenvLoader> mockedDotenv = Mockito.mockStatic(DotenvLoader.class)) {
            mockedDotenv.when(() -> DotenvLoader.getEnv("ANTHROPIC_API_KEY"))
                .thenReturn("   ");
            
            assertFalse(authManager.isAuthenticated());
        }
    }
}
