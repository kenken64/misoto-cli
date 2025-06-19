package sg.edu.nus.iss.misoto.cli.ai.provider.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import sg.edu.nus.iss.misoto.cli.ai.provider.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Anthropic Claude AI provider implementation
 */
@Component
@Slf4j
public class AnthropicProvider implements AiProvider {
    
    @Autowired(required = false)
    private AnthropicChatModel anthropicChatModel;
    
    @Value("${spring.ai.anthropic.chat.model:claude-sonnet-4-20250514}")
    private String currentModel;
    
    @PostConstruct
    public void logCurrentModel() {
        log.info("AnthropicProvider initialized with model: {}", currentModel);
        log.info("System property ANTHROPIC_MODEL: {}", System.getProperty("ANTHROPIC_MODEL"));
        log.info("Environment variable ANTHROPIC_MODEL: {}", System.getenv("ANTHROPIC_MODEL"));
    }
    private AiUsage lastUsage;
    private boolean initialized = false;
    
    // Anthropic model pricing (per 1M tokens)
    private static final Map<String, Double[]> MODEL_PRICING = Map.of(
        "claude-3-haiku-20240307", new Double[]{0.25, 1.25},           // input, output
        "claude-3-sonnet-20240229", new Double[]{3.0, 15.0},
        "claude-3-opus-20240229", new Double[]{15.0, 75.0},
        "claude-sonnet-4-20250514", new Double[]{3.0, 15.0}            // Assuming similar to sonnet
    );
    
    @Override
    public String getProviderName() {
        return "anthropic";
    }
    
    @Override
    public String getDisplayName() {
        return "Anthropic Claude";
    }
    
    @Override
    public boolean isAvailable() {
        return anthropicChatModel != null && initialized;
    }
    
    @Override
    public void initialize(Map<String, Object> config) throws Exception {
        if (anthropicChatModel == null) {
            throw new IllegalStateException("AnthropicChatModel not available. Check Spring AI configuration and API key.");
        }
        
        // Set model if specified in config
        if (config.containsKey("model")) {
            setModel((String) config.get("model"));
        }
        
        initialized = true;
        log.info("Anthropic provider initialized with model: {}", currentModel);
    }
    
    @Override
    public AiResponse sendMessage(String message) throws Exception {
        return sendMessage(null, message, null);
    }
    
    @Override
    public AiResponse sendMessage(String systemPrompt, String userMessage) throws Exception {
        return sendMessage(systemPrompt, userMessage, null);
    }
    
    @Override
    public AiResponse sendMessage(String systemPrompt, String userMessage, List<ChatMessage> history) throws Exception {
        if (!isAvailable()) {
            return AiResponse.error("Anthropic provider not available", getProviderName());
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
            
            // Add system prompt if provided
            if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
                messages.add(new SystemMessage(systemPrompt));
            }
            
            // Add conversation history if provided
            if (history != null) {
                for (ChatMessage msg : history) {
                    switch (msg.getRole()) {
                        case SYSTEM:
                            messages.add(new SystemMessage(msg.getContent()));
                            break;
                        case USER:
                            messages.add(new UserMessage(msg.getContent()));
                            break;
                        case ASSISTANT:
                            messages.add(new AssistantMessage(msg.getContent()));
                            break;
                    }
                }
            }
            
            // Add current user message
            messages.add(new UserMessage(userMessage));
            
            Prompt prompt = new Prompt(messages);
            ChatResponse response = anthropicChatModel.call(prompt);
            
            String responseText = response.getResult().getOutput().getText();
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Extract usage information
            Usage usage = response.getMetadata().getUsage();
            AiUsage aiUsage = null;
            if (usage != null) {
                Integer outputTokens = usage.getTotalTokens() - usage.getPromptTokens();
                Double cost = calculateCost(usage.getPromptTokens(), outputTokens);
                aiUsage = AiUsage.of(usage.getPromptTokens(), outputTokens, cost);
                lastUsage = aiUsage;
            }
            
            log.debug("Anthropic response received in {}ms", responseTime);
            
            return AiResponse.success(responseText, currentModel, getProviderName(), aiUsage, responseTime);
            
        } catch (Exception e) {
            log.error("Failed to get response from Anthropic: {}", e.getMessage());
            return AiResponse.error("Failed to communicate with Anthropic: " + e.getMessage(), getProviderName());
        }
    }
    
    @Override
    public List<String> getAvailableModels() {
        return List.of(
            "claude-3-haiku-20240307",
            "claude-3-sonnet-20240229", 
            "claude-3-opus-20240229",
            "claude-sonnet-4-20250514",
            "claude-opus-4-20250514"
        );
    }
    
    @Override
    public String getCurrentModel() {
        return currentModel;
    }
    
    @Override
    public void setModel(String model) {
        if (getAvailableModels().contains(model)) {
            this.currentModel = model;
            log.info("Anthropic model changed to: {}", model);
        } else {
            throw new IllegalArgumentException("Unsupported Anthropic model: " + model);
        }
    }
    
    @Override
    public Map<String, Object> getConfigurationSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("api_key", Map.of("type", "string", "required", true, "description", "Anthropic API key"));
        schema.put("model", Map.of("type", "string", "required", false, "default", "claude-3-haiku-20240307", "enum", getAvailableModels()));
        schema.put("temperature", Map.of("type", "number", "required", false, "default", 0.7, "min", 0.0, "max", 1.0));
        schema.put("max_tokens", Map.of("type", "integer", "required", false, "default", 8000, "min", 1, "max", 100000));
        return schema;
    }
    
    @Override
    public boolean validateConfiguration(Map<String, Object> config) {
        return config.containsKey("api_key") && config.get("api_key") != null;
    }
    
    @Override
    public AiUsage getLastUsage() {
        return lastUsage;
    }
    
    @Override
    public boolean supportsStreaming() {
        return true;
    }
    
    @Override
    public boolean supportsFunctionCalling() {
        return true;
    }
    
    @Override
    public ProviderCapabilities getCapabilities() {
        return ProviderCapabilities.builder()
            .supportsChat(true)
            .supportsStreaming(true)
            .supportsFunctionCalling(true)
            .supportsSystemPrompts(true)
            .supportsHistory(true)
            .supportsImageInput(true)
            .supportsFileInput(false)
            .maxTokens(100000)
            .maxHistoryMessages(50)
            .build();
    }
    
    private Double calculateCost(Integer inputTokens, Integer outputTokens) {
        if (inputTokens == null || outputTokens == null) {
            return null;
        }
        
        Double[] pricing = MODEL_PRICING.get(currentModel);
        if (pricing == null) {
            return null;
        }
        
        double inputCost = (inputTokens / 1_000_000.0) * pricing[0];
        double outputCost = (outputTokens / 1_000_000.0) * pricing[1];
        
        return inputCost + outputCost;
    }
}
