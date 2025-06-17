package sg.edu.nus.iss.misoto.cli.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.misoto.cli.ai.provider.*;
import sg.edu.nus.iss.misoto.cli.auth.AuthManager;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Service for interacting with AI providers (backward compatibility wrapper)
 */
@Service
@Slf4j
public class AiClient {
    
    @Autowired
    private AuthManager authManager;
    
    @Autowired
    private AiProviderManager providerManager;
    
    private boolean initialized = false;
    
    /**
     * Initialize the AI client
     */
    public void initialize() throws Exception {
        if (initialized) {
            return;
        }
        
        if (!authManager.isAuthenticated()) {
            throw new IllegalStateException("Authentication required to initialize AI client");
        }
        
        if (!providerManager.isReady()) {
            throw new IllegalStateException("AI Provider Manager not ready. Check provider configuration.");
        }
        
        initialized = true;
        log.debug("AI client initialized with provider: {}", providerManager.getCurrentProviderName());
    }
    
    /**
     * Check if the AI client is initialized and ready
     */
    public boolean isReady() {
        return initialized && providerManager.isReady() && authManager.isAuthenticated();
    }
    
    /**
     * Send a message to current AI provider
     */
    public String sendMessage(String message) throws Exception {
        if (!isReady()) {
            throw new IllegalStateException("AI client not ready. Call initialize() first.");
        }
        
        log.debug("Sending message to AI provider: {}", message.substring(0, Math.min(100, message.length())));
        
        try {
            AiResponse response = providerManager.sendMessage(message);
            
            if (response.isSuccess()) {
                log.debug("Received response from AI provider");
                return response.getText();
            } else {
                throw new RuntimeException("AI provider error: " + response.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("Failed to send message to AI provider: {}", e.getMessage());
            throw new RuntimeException("Failed to communicate with AI provider: " + e.getMessage(), e);
        }
    }
      /**
     * Send a message with system prompt
     */
    public String sendMessage(String systemPrompt, String userMessage) throws Exception {
        if (!isReady()) {
            throw new IllegalStateException("AI client not ready. Call initialize() first.");
        }
        
        log.debug("Sending message with system prompt to AI provider");
        
        try {
            AiResponse response = providerManager.sendMessage(systemPrompt, userMessage);
            
            if (response.isSuccess()) {
                log.debug("Received response from AI provider");
                return response.getText();
            } else {
                throw new RuntimeException("AI provider error: " + response.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("Failed to send message to AI provider: {}", e.getMessage());
            
            // Try fallback provider
            if (providerManager.tryFallbackProvider()) {
                log.info("Retrying with fallback provider");
                try {
                    AiResponse response = providerManager.sendMessage(systemPrompt, userMessage);
                    if (response.isSuccess()) {
                        log.debug("Received response from fallback AI provider");
                        return response.getText();
                    }
                } catch (Exception fallbackException) {
                    log.error("Fallback provider also failed: {}", fallbackException.getMessage());
                }
            }
            
            throw new RuntimeException("Failed to communicate with AI provider: " + e.getMessage(), e);
        }
    }
    
    /**
     * Send a message with conversation history
     */
    public String sendMessage(String systemPrompt, String userMessage, List<ChatMessage> history) throws Exception {
        if (!isReady()) {
            throw new IllegalStateException("AI client not ready. Call initialize() first.");
        }
        
        log.debug("Sending message with history to AI provider");
        
        try {
            AiResponse response = providerManager.sendMessage(systemPrompt, userMessage, history);
            
            if (response.isSuccess()) {
                log.debug("Received response from AI provider");
                return response.getText();
            } else {
                throw new RuntimeException("AI provider error: " + response.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("Failed to send message to AI provider: {}", e.getMessage());
            throw new RuntimeException("Failed to communicate with AI provider: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get usage information from last AI response
     */
    public AiUsage getLastUsage() {
        return providerManager.getLastUsage();
    }
    
    /**
     * Get current AI provider name
     */
    public String getCurrentProvider() {
        return providerManager.getCurrentProviderName();
    }
    
    /**
     * Get current model name
     */
    public String getCurrentModel() {
        AiProvider provider = providerManager.getCurrentProvider();
        return provider != null ? provider.getCurrentModel() : "unknown";
    }
    
    /**
     * Calculate estimated cost based on model and token usage
     */
    private Double calculateEstimatedCost(String modelName, Integer inputTokens, Integer outputTokens) {
        // Claude pricing (as of 2024) - these are example rates, adjust as needed
        double inputCostPerMToken = 0.0; // Cost per million input tokens
        double outputCostPerMToken = 0.0; // Cost per million output tokens
        
        // Set pricing based on model
        switch (modelName.toLowerCase()) {
            case "claude-3-haiku-20240307":
            case "claude-haiku-3":
                inputCostPerMToken = 0.25;  // $0.25 per million input tokens
                outputCostPerMToken = 1.25; // $1.25 per million output tokens
                break;
            case "claude-3-sonnet-20240229":
            case "claude-sonnet-3":
                inputCostPerMToken = 3.0;   // $3.00 per million input tokens
                outputCostPerMToken = 15.0; // $15.00 per million output tokens
                break;
            case "claude-3-opus-20240229":
            case "claude-opus-3":
                inputCostPerMToken = 15.0;  // $15.00 per million input tokens
                outputCostPerMToken = 75.0; // $75.00 per million output tokens
                break;
            case "claude-sonnet-4-20250514":
            default:
                // Default to Sonnet 3.5 pricing
                inputCostPerMToken = 3.0;
                outputCostPerMToken = 15.0;
                break;
        }
        
        if (inputTokens == null || outputTokens == null) {
            return null;
        }
        
        double inputCost = (inputTokens / 1_000_000.0) * inputCostPerMToken;
        double outputCost = (outputTokens / 1_000_000.0) * outputCostPerMToken;
        
        return inputCost + outputCost;
    }

    
    /**
     * Get information about the current AI model configuration
     */
    public String getModelInfo() {
        AiProvider provider = providerManager.getCurrentProvider();
        if (provider != null) {
            return String.format("Model: %s | Provider: %s", 
                    provider.getCurrentModel(), provider.getClass().getSimpleName());
        }
        return "No AI provider available";
    }
    
    /**
     * Get the current model name
     */
    public String getModelName() {
        return getCurrentModel();
    }
    
    
    /**
     * Send a message with detailed usage information
     * Uses the provider manager to get usage stats
     */
    public sg.edu.nus.iss.misoto.cli.ai.provider.AiResponse sendMessageWithUsage(String systemPrompt, String userMessage) throws Exception {
        if (!isReady()) {
            throw new IllegalStateException("AI client not ready. Call initialize() first.");
        }
        
        log.debug("Sending message with system prompt and tracking usage");
        
        try {
            sg.edu.nus.iss.misoto.cli.ai.provider.AiResponse response = providerManager.sendMessage(systemPrompt, userMessage);
            
            if (response.isSuccess()) {
                log.debug("Received response from AI provider with usage data");
                return response;
            } else {
                throw new RuntimeException("AI provider error: " + response.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("Failed to send message to AI provider: {}", e.getMessage());
            
            // Try fallback provider
            if (providerManager.tryFallbackProvider()) {
                log.info("Retrying with fallback provider");
                try {
                    sg.edu.nus.iss.misoto.cli.ai.provider.AiResponse response = providerManager.sendMessage(systemPrompt, userMessage);
                    if (response.isSuccess()) {
                        log.debug("Received response from fallback AI provider with usage data");
                        return response;
                    }
                } catch (Exception fallbackException) {
                    log.error("Fallback provider also failed: {}", fallbackException.getMessage());
                }
            }
            
            throw new RuntimeException("Failed to communicate with AI provider: " + e.getMessage(), e);
        }
    }
}
