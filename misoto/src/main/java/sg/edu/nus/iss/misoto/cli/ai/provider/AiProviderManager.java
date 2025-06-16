package sg.edu.nus.iss.misoto.cli.ai.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.misoto.cli.ai.provider.impl.AnthropicProvider;
import sg.edu.nus.iss.misoto.cli.ai.provider.impl.OllamaProvider;

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * Manages multiple AI providers and handles switching between them
 */
@Service
@Slf4j
public class AiProviderManager {
    
    @Autowired
    private AnthropicProvider anthropicProvider;
    
    @Autowired
    private OllamaProvider ollamaProvider;
    
    @Value("${misoto.ai.default-provider:anthropic}")
    private String defaultProviderName;
    
    private final Map<String, AiProvider> providers = new HashMap<>();
    private AiProvider currentProvider;
    private boolean initialized = false;
    
    @PostConstruct
    public void initialize() {
        // Register all providers
        registerProvider(anthropicProvider);
        registerProvider(ollamaProvider);
        
        // Try to set default provider with fallback
        if (!setCurrentProvider(defaultProviderName)) {
            log.warn("Default provider '{}' is not available, trying fallback options", defaultProviderName);
            
            // Try fallback providers in order of preference
            String[] fallbackOrder = {"ollama", "anthropic"};
            boolean providerSet = false;
            
            for (String fallbackProvider : fallbackOrder) {
                if (!fallbackProvider.equals(defaultProviderName) && setCurrentProvider(fallbackProvider)) {
                    log.info("Successfully fell back to provider: {}", fallbackProvider);
                    providerSet = true;
                    break;
                }
            }
            
            if (!providerSet) {
                log.error("No AI providers are available!");
            }
        }
        
        initialized = true;
        log.info("AI Provider Manager initialized with {} providers. Current: {}", 
            providers.size(), getCurrentProviderName());
    }
    
    /**
     * Register a new AI provider
     */
    public void registerProvider(AiProvider provider) {
        providers.put(provider.getProviderName(), provider);
        log.debug("Registered AI provider: {}", provider.getDisplayName());
    }
    
    /**
     * Switch to a different provider
     */
    public boolean switchProvider(String providerName) {
        return setCurrentProvider(providerName);
    }
    
    /**
     * Set the current provider
     */
    private boolean setCurrentProvider(String providerName) {
        AiProvider provider = providers.get(providerName);
        if (provider == null) {
            log.warn("Provider '{}' not found. Available: {}", providerName, getAvailableProviders());
            return false;
        }
        
        try {
            // Initialize provider with default config
            Map<String, Object> config = getDefaultConfigForProvider(providerName);
            provider.initialize(config);
            
            if (provider.isAvailable()) {
                currentProvider = provider;
                log.info("Switched to provider: {}", provider.getDisplayName());
                return true;
            } else {
                log.warn("Provider '{}' is not available", provider.getDisplayName());
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to initialize provider '{}': {}", provider.getDisplayName(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Get the current provider
     */
    public AiProvider getCurrentProvider() {
        return currentProvider;
    }
    
    /**
     * Get current provider name
     */
    public String getCurrentProviderName() {
        return currentProvider != null ? currentProvider.getProviderName() : "none";
    }
    
    /**
     * Get list of available provider names
     */
    public List<String> getAvailableProviders() {
        return new ArrayList<>(providers.keySet());
    }
    
    /**
     * Get list of available providers with their status
     */
    public Map<String, ProviderStatus> getProviderStatuses() {
        Map<String, ProviderStatus> statuses = new HashMap<>();
        
        for (Map.Entry<String, AiProvider> entry : providers.entrySet()) {
            AiProvider provider = entry.getValue();
            boolean isAvailable = provider.isAvailable();
            boolean isCurrent = provider == currentProvider;
            
            ProviderStatus status = ProviderStatus.builder()
                .name(provider.getProviderName())
                .displayName(provider.getDisplayName())
                .available(isAvailable)
                .current(isCurrent)
                .currentModel(provider.getCurrentModel())
                .capabilities(provider.getCapabilities())
                .build();
            
            statuses.put(entry.getKey(), status);
        }
        
        return statuses;
    }
    
    /**
     * Get available models for current provider
     */
    public List<String> getAvailableModels() {
        if (currentProvider == null) {
            return Collections.emptyList();
        }
        return currentProvider.getAvailableModels();
    }
    
    /**
     * Set model for current provider
     */
    public boolean setModel(String model) {
        if (currentProvider == null) {
            log.warn("No provider selected");
            return false;
        }
        
        try {
            currentProvider.setModel(model);
            return true;
        } catch (Exception e) {
            log.error("Failed to set model '{}': {}", model, e.getMessage());
            return false;
        }
    }
    
    /**
     * Send message using current provider
     */
    public AiResponse sendMessage(String message) throws Exception {
        if (currentProvider == null) {
            throw new IllegalStateException("No AI provider selected");
        }
        return currentProvider.sendMessage(message);
    }
    
    /**
     * Send message with system prompt using current provider
     */
    public AiResponse sendMessage(String systemPrompt, String userMessage) throws Exception {
        if (currentProvider == null) {
            throw new IllegalStateException("No AI provider selected");
        }
        return currentProvider.sendMessage(systemPrompt, userMessage);
    }
    
    /**
     * Send message with history using current provider
     */
    public AiResponse sendMessage(String systemPrompt, String userMessage, List<ChatMessage> history) throws Exception {
        if (currentProvider == null) {
            throw new IllegalStateException("No AI provider selected");
        }
        return currentProvider.sendMessage(systemPrompt, userMessage, history);
    }
    
    /**
     * Get usage from last response
     */
    public AiUsage getLastUsage() {
        if (currentProvider == null) {
            return null;
        }
        return currentProvider.getLastUsage();
    }
    
    /**
     * Check if manager is initialized and ready
     */
    public boolean isReady() {
        return initialized && currentProvider != null && currentProvider.isAvailable();
    }
    
    /**
     * Get provider by name
     */
    public AiProvider getProvider(String name) {
        return providers.get(name);
    }
    
    private Map<String, Object> getDefaultConfigForProvider(String providerName) {
        Map<String, Object> config = new HashMap<>();
        
        switch (providerName) {
            case "anthropic":
                // Anthropic config comes from Spring AI auto-configuration
                break;
            case "ollama":
                config.put("base_url", "http://localhost:11434");
                config.put("model", "qwen2.5:0.5b");
                break;
        }
        
        return config;
    }
    
    /**
     * Try to automatically switch to an available provider when current provider fails
     */
    public boolean tryFallbackProvider() {
        if (currentProvider != null && currentProvider.isAvailable()) {
            return true; // Current provider is still working
        }
        
        log.warn("Current provider '{}' is not available, attempting automatic fallback", 
            currentProvider != null ? currentProvider.getProviderName() : "none");
        
        // Try all other providers
        String[] fallbackOrder = {"ollama", "anthropic"};
        
        for (String fallbackProvider : fallbackOrder) {
            if (currentProvider == null || !fallbackProvider.equals(currentProvider.getProviderName())) {
                if (setCurrentProvider(fallbackProvider)) {
                    log.info("Successfully fell back to provider: {}", fallbackProvider);
                    return true;
                }
            }
        }
        
        log.error("No AI providers are available for fallback!");
        return false;
    }
}
