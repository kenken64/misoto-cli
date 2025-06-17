package sg.edu.nus.iss.misoto.cli.ai.provider;

import java.util.List;
import java.util.Map;

/**
 * Common interface for all AI providers
 */
public interface AiProvider {
    
    /**
     * Get the provider name (e.g., "anthropic", "ollama", "openai")
     */
    String getProviderName();
    
    /**
     * Get the display name for the provider
     */
    String getDisplayName();
    
    /**
     * Check if the provider is available and properly configured
     */
    boolean isAvailable();
    
    /**
     * Initialize the provider with configuration
     */
    void initialize(Map<String, Object> config) throws Exception;
    
    /**
     * Send a simple message to the AI
     */
    AiResponse sendMessage(String message) throws Exception;
    
    /**
     * Send a message with system prompt
     */
    AiResponse sendMessage(String systemPrompt, String userMessage) throws Exception;
    
    /**
     * Send a message with conversation history
     */
    AiResponse sendMessage(String systemPrompt, String userMessage, List<ChatMessage> history) throws Exception;
    
    /**
     * Get list of available models for this provider
     */
    List<String> getAvailableModels();
    
    /**
     * Get the currently selected model
     */
    String getCurrentModel();
    
    /**
     * Set the model to use
     */
    void setModel(String model);
    
    /**
     * Get provider-specific configuration schema
     */
    Map<String, Object> getConfigurationSchema();
    
    /**
     * Validate configuration for this provider
     */
    boolean validateConfiguration(Map<String, Object> config);
    
    /**
     * Get usage information (tokens, cost, etc.) from last response
     */
    AiUsage getLastUsage();
    
    /**
     * Check if provider supports streaming responses
     */
    default boolean supportsStreaming() {
        return false;
    }
    
    /**
     * Check if provider supports function calling
     */
    default boolean supportsFunctionCalling() {
        return false;
    }
    
    /**
     * Get provider capabilities
     */
    default ProviderCapabilities getCapabilities() {
        return ProviderCapabilities.builder()
            .supportsChat(true)
            .supportsStreaming(supportsStreaming())
            .supportsFunctionCalling(supportsFunctionCalling())
            .supportsSystemPrompts(true)
            .supportsHistory(true)
            .build();
    }
}
