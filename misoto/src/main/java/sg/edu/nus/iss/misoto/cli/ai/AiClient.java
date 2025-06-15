package sg.edu.nus.iss.misoto.cli.ai;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.misoto.cli.auth.AuthManager;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Service for interacting with Claude AI
 */
@Service
@Slf4j
public class AiClient {
    
    @Autowired
    private AuthManager authManager;
    
    @Autowired(required = false)
    private AnthropicChatModel anthropicChatModel;
    
    @Value("${spring.ai.anthropic.chat.model:claude-3-haiku-20240307}")
    private String modelName;
    
    @Value("${spring.ai.anthropic.chat.temperature:0.7}")
    private Double temperature;
    
    @Value("${spring.ai.anthropic.chat.max-tokens:8000}")
    private Integer maxTokens;
    
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
        
        // The AnthropicChatModel should be auto-configured by Spring AI
        // We'll use the token from AuthManager to set the API key
        if (anthropicChatModel == null) {
            throw new IllegalStateException("AnthropicChatModel not available. Check Spring AI configuration.");
        }
        
        initialized = true;
        log.debug("AI client initialized");
    }
    
    /**
     * Check if the AI client is initialized and ready
     */
    public boolean isReady() {
        return initialized && anthropicChatModel != null && authManager.isAuthenticated();
    }
    
    /**
     * Send a message to Claude AI
     */
    public String sendMessage(String message) throws Exception {
        if (!isReady()) {
            throw new IllegalStateException("AI client not ready. Call initialize() first.");
        }
        
        log.debug("Sending message to Claude AI: {}", message.substring(0, Math.min(100, message.length())));
        
        try {
            // Create a user message and send to Claude
            UserMessage userMessage = new UserMessage(message);
            Prompt prompt = new Prompt(List.of(userMessage));
            
            ChatResponse result = anthropicChatModel.call(prompt);
            String response = result.getResult().getOutput().getText();
            
            log.debug("Received response from Claude AI");
            return response;
            
        } catch (Exception e) {
            log.error("Failed to send message to Claude AI: {}", e.getMessage());
            throw new RuntimeException("Failed to communicate with Claude AI: " + e.getMessage(), e);
        }
    }
    
    /**
     * Send a message with system prompt
     */
    public String sendMessage(String systemPrompt, String userMessage) throws Exception {
        if (!isReady()) {
            throw new IllegalStateException("AI client not ready. Call initialize() first.");
        }
        
        log.debug("Sending message with system prompt to Claude AI");
        
        try {
            // Create system and user messages
            SystemMessage sysMsg = new SystemMessage(systemPrompt);
            UserMessage userMsg = new UserMessage(userMessage);
            
            Prompt prompt = new Prompt(List.of(sysMsg, userMsg));
            
            ChatResponse result = anthropicChatModel.call(prompt);
            String response = result.getResult().getOutput().getText();
            
            log.debug("Received response from Claude AI");
            return response;
            
        } catch (Exception e) {
            log.error("Failed to send message to Claude AI: {}", e.getMessage());
            throw new RuntimeException("Failed to communicate with Claude AI: " + e.getMessage(), e);
        }
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
     * Send a message with detailed usage information
     */
    public AiResponse sendMessageWithUsage(String systemPrompt, String userMessage) throws Exception {
        if (!isReady()) {
            throw new IllegalStateException("AI client not ready. Call initialize() first.");
        }
        
        log.debug("Sending message with system prompt to Claude AI");
        
        try {
            // Create system and user messages
            SystemMessage sysMsg = new SystemMessage(systemPrompt);
            UserMessage userMsg = new UserMessage(userMessage);
            
            Prompt prompt = new Prompt(List.of(sysMsg, userMsg));
            
            ChatResponse result = anthropicChatModel.call(prompt);
            String responseText = result.getResult().getOutput().getText();            // Extract usage information
            Usage usage = result.getMetadata().getUsage();
            Integer inputTokens = usage != null ? usage.getPromptTokens() : null;
            Integer outputTokens = null;
            Integer totalTokens = usage != null ? usage.getTotalTokens() : null;
            
            // Try different method names for output tokens
            if (usage != null) {
                try {
                    // Try common method names
                    outputTokens = (Integer) usage.getClass().getMethod("getGenerationTokens").invoke(usage);
                } catch (Exception e1) {
                    try {
                        outputTokens = (Integer) usage.getClass().getMethod("getOutputTokens").invoke(usage);
                    } catch (Exception e2) {
                        try {
                            outputTokens = (Integer) usage.getClass().getMethod("getCompletionTokens").invoke(usage);
                        } catch (Exception e3) {
                            // If we can't get output tokens specifically, calculate from total - input
                            if (totalTokens != null && inputTokens != null) {
                                outputTokens = totalTokens - inputTokens;
                            }
                        }
                    }
                }
            }
            
            // Calculate estimated cost
            Double estimatedCost = calculateEstimatedCost(modelName, inputTokens, outputTokens);
            
            log.debug("Received response from Claude AI - Input tokens: {}, Output tokens: {}, Total: {}", 
                     inputTokens, outputTokens, totalTokens);
            
            return new AiResponse(responseText, inputTokens, outputTokens, totalTokens, estimatedCost);
            
        } catch (Exception e) {
            log.error("Failed to send message to Claude AI: {}", e.getMessage());
            throw new RuntimeException("Failed to communicate with Claude AI: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get information about the current AI model configuration
     */
    public String getModelInfo() {
        return String.format("Model: %s | Temperature: %.1f | Max Tokens: %d", 
                modelName, temperature, maxTokens);
    }
    
    /**
     * Get the current model name
     */
    public String getModelName() {
        return modelName;
    }
    
    /**
     * Get the current temperature setting
     */
    public Double getTemperature() {
        return temperature;
    }
    
    /**
     * Get the current max tokens setting
     */
    public Integer getMaxTokens() {
        return maxTokens;
    }
    
    /**
     * AI Response with usage information
     */
    public static class AiResponse {
        private final String text;
        private final Integer inputTokens;
        private final Integer outputTokens;
        private final Integer totalTokens;
        private final Double estimatedCost;
        
        public AiResponse(String text, Integer inputTokens, Integer outputTokens, Integer totalTokens, Double estimatedCost) {
            this.text = text;
            this.inputTokens = inputTokens;
            this.outputTokens = outputTokens;
            this.totalTokens = totalTokens;
            this.estimatedCost = estimatedCost;
        }
        
        public String getText() { return text; }
        public Integer getInputTokens() { return inputTokens; }
        public Integer getOutputTokens() { return outputTokens; }
        public Integer getTotalTokens() { return totalTokens; }
        public Double getEstimatedCost() { return estimatedCost; }
    }
}
