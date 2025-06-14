package sg.edu.nus.iss.misoto.cli.ai;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
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
}
