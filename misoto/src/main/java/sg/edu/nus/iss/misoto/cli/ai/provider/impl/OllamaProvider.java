package sg.edu.nus.iss.misoto.cli.ai.provider.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sg.edu.nus.iss.misoto.cli.ai.provider.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Ollama AI provider implementation
 */
@Component
@Slf4j
public class OllamaProvider implements AiProvider {
    
    @Value("${misoto.ai.ollama.base-url:http://localhost:11434}")
    private String baseUrl;
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private String currentModel = "llama3.2";
    private AiUsage lastUsage;
    private boolean initialized = false;
    private List<String> availableModels = new ArrayList<>();
    
    public OllamaProvider() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public String getProviderName() {
        return "ollama";
    }
    
    @Override
    public String getDisplayName() {
        return "Ollama";
    }
    
    @Override
    public boolean isAvailable() {
        return initialized && isOllamaRunning();
    }
    
    @Override
    public void initialize(Map<String, Object> config) throws Exception {
        // Set base URL if specified in config
        if (config.containsKey("base_url")) {
            baseUrl = (String) config.get("base_url");
        }
        
        // Check if Ollama is running
        if (!isOllamaRunning()) {
            throw new IllegalStateException("Ollama is not running. Please start Ollama service first.");
        }
        
        // Load available models
        loadAvailableModels();
        
        // Set model if specified in config
        if (config.containsKey("model")) {
            String requestedModel = (String) config.get("model");
            if (availableModels.contains(requestedModel)) {
                setModel(requestedModel);
            } else {
                log.warn("Requested model '{}' not available. Using default: {}", requestedModel, currentModel);
            }
        }
        
        initialized = true;
        log.info("Ollama provider initialized with model: {} (available: {})", currentModel, availableModels.size());
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
            return AiResponse.error("Ollama provider not available", getProviderName());
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("model", currentModel);
            request.put("stream", false);
            
            // Build the prompt with system prompt, history, and user message
            StringBuilder fullPrompt = new StringBuilder();
            
            if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
                fullPrompt.append("System: ").append(systemPrompt).append("\n\n");
            }
            
            // Add conversation history
            if (history != null) {
                for (ChatMessage msg : history) {
                    switch (msg.getRole()) {
                        case SYSTEM:
                            fullPrompt.append("System: ").append(msg.getContent()).append("\n");
                            break;
                        case USER:
                            fullPrompt.append("Human: ").append(msg.getContent()).append("\n");
                            break;
                        case ASSISTANT:
                            fullPrompt.append("Assistant: ").append(msg.getContent()).append("\n");
                            break;
                    }
                }
            }
            
            fullPrompt.append("Human: ").append(userMessage).append("\nAssistant: ");
            request.put("prompt", fullPrompt.toString());
            
            String requestBody = objectMapper.writeValueAsString(request);
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/generate"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMinutes(5))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
            
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                return AiResponse.error("Ollama API error: " + response.statusCode() + " " + response.body(), getProviderName());
            }
            
            JsonNode responseJson = objectMapper.readTree(response.body());
            String responseText = responseJson.get("response").asText();
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Extract usage information if available
            AiUsage aiUsage = null;
            if (responseJson.has("eval_count") && responseJson.has("prompt_eval_count")) {
                int promptTokens = responseJson.get("prompt_eval_count").asInt();
                int responseTokens = responseJson.get("eval_count").asInt();
                aiUsage = AiUsage.of(promptTokens, responseTokens, 0.0); // Ollama is free
                lastUsage = aiUsage;
            }
            
            log.debug("Ollama response received in {}ms", responseTime);
            
            return AiResponse.success(responseText, currentModel, getProviderName(), aiUsage, responseTime);
            
        } catch (Exception e) {
            log.error("Failed to get response from Ollama: {}", e.getMessage());
            return AiResponse.error("Failed to communicate with Ollama: " + e.getMessage(), getProviderName());
        }
    }
    
    @Override
    public List<String> getAvailableModels() {
        return new ArrayList<>(availableModels);
    }
    
    @Override
    public String getCurrentModel() {
        return currentModel;
    }
    
    @Override
    public void setModel(String model) {
        if (availableModels.contains(model)) {
            this.currentModel = model;
            log.info("Ollama model changed to: {}", model);
        } else {
            throw new IllegalArgumentException("Model '" + model + "' not available in Ollama. Available: " + availableModels);
        }
    }
    
    @Override
    public Map<String, Object> getConfigurationSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("base_url", Map.of("type", "string", "required", false, "default", "http://localhost:11434", "description", "Ollama server URL"));
        schema.put("model", Map.of("type", "string", "required", false, "default", "llama3.2", "description", "Model name (use 'ollama list' to see available)"));
        return schema;
    }
    
    @Override
    public boolean validateConfiguration(Map<String, Object> config) {
        // Ollama doesn't require API keys, just needs to be running
        return true;
    }
    
    @Override
    public AiUsage getLastUsage() {
        return lastUsage;
    }
    
    @Override
    public ProviderCapabilities getCapabilities() {
        return ProviderCapabilities.builder()
            .supportsChat(true)
            .supportsStreaming(true)
            .supportsFunctionCalling(false)
            .supportsSystemPrompts(true)
            .supportsHistory(true)
            .supportsImageInput(false)
            .supportsFileInput(false)
            .maxTokens(32768)
            .maxHistoryMessages(20)
            .build();
    }
    
    private boolean isOllamaRunning() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/tags"))
                .timeout(Duration.ofSeconds(5))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
            
        } catch (Exception e) {
            log.debug("Ollama is not running or not accessible: {}", e.getMessage());
            return false;
        }
    }
    
    private void loadAvailableModels() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/tags"))
                .timeout(Duration.ofSeconds(10))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode responseJson = objectMapper.readTree(response.body());
                JsonNode models = responseJson.get("models");
                
                if (models != null && models.isArray()) {
                    availableModels = new ArrayList<>();
                    for (JsonNode model : models) {
                        String modelName = model.get("name").asText();
                        // Remove tag if present (e.g., "llama3.2:latest" -> "llama3.2")
                        if (modelName.contains(":")) {
                            modelName = modelName.split(":")[0];
                        }
                        if (!availableModels.contains(modelName)) {
                            availableModels.add(modelName);
                        }
                    }
                }
                
                log.debug("Loaded {} Ollama models: {}", availableModels.size(), availableModels);
                
                // Set a default model if current model is not available
                if (!availableModels.contains(currentModel) && !availableModels.isEmpty()) {
                    currentModel = availableModels.get(0);
                    log.info("Set default Ollama model to: {}", currentModel);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load Ollama models: {}", e.getMessage());
            availableModels = List.of("llama3.2", "llama3.1", "llama3", "mistral", "codellama");
        }
    }
}
