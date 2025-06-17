package sg.edu.nus.iss.misoto.cli.ai.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Chat message for conversation history
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    
    public enum Role {
        SYSTEM, USER, ASSISTANT
    }
    
    private Role role;
    private String content;
    private long timestamp;
    
    public static ChatMessage system(String content) {
        return ChatMessage.builder()
            .role(Role.SYSTEM)
            .content(content)
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    public static ChatMessage user(String content) {
        return ChatMessage.builder()
            .role(Role.USER)
            .content(content)
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    public static ChatMessage assistant(String content) {
        return ChatMessage.builder()
            .role(Role.ASSISTANT)
            .content(content)
            .timestamp(System.currentTimeMillis())
            .build();
    }
}
