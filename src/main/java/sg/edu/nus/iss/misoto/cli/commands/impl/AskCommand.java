package sg.edu.nus.iss.misoto.cli.commands.impl;

import sg.edu.nus.iss.misoto.cli.commands.Command;
import sg.edu.nus.iss.misoto.cli.ai.AiClient;
import sg.edu.nus.iss.misoto.cli.errors.UserError;
import sg.edu.nus.iss.misoto.cli.utils.FormattingUtil;
import sg.edu.nus.iss.misoto.cli.utils.ValidationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Command for asking questions to Claude AI
 */
@Component
@Slf4j
public class AskCommand implements Command {
    
    @Autowired
    private AiClient aiClient;
    
    @Override
    public String getName() {
        return "ask";
    }
    
    @Override
    public String getDescription() {
        return "Ask a question to Claude AI";
    }
    
    @Override
    public String getCategory() {
        return "AI Assistance";
    }
    
    @Override
    public boolean isHidden() {
        return false;
    }
    
    @Override
    public boolean requiresAuth() {
        return true;
    }
    
    @Override
    public String getUsage() {
        return "claude-code ask \"<question>\"";
    }
    
    @Override
    public List<String> getExamples() {
        return List.of(
            "claude-code ask \"How do I implement a binary search tree in Java?\"",
            "claude-code ask \"What are the best practices for Spring Boot security?\"",
            "claude-code ask \"Explain the difference between ArrayList and LinkedList\""
        );
    }
      @Override
    public void execute(List<String> args) throws Exception {
        if (args.isEmpty()) {
            throw new UserError("Please provide a question to ask. Usage: " + getUsage());
        }
        
        // Join all arguments to form the question
        String question = String.join(" ", args);
        
        // Validate the question using our validation utilities
        if (!ValidationUtil.isNonEmptyString(question)) {
            throw new UserError("Question cannot be empty");
        }
        
        // Validate question length (reasonable limit)
        if (question.length() > 5000) {
            throw new UserError("Question is too long. Please keep it under 5000 characters.");
        }
          try {
            // Enhanced logging with formatted output
            String truncatedQuestion = FormattingUtil.truncate(question, 100);
            System.out.println(FormattingUtil.formatWithColor("Asking Claude AI: " + truncatedQuestion, FormattingUtil.ANSI_CYAN));
            System.out.println(FormattingUtil.formatWithColor("Thinking...", FormattingUtil.ANSI_YELLOW));
            System.out.println();
            
            // Track timing
            long startTime = System.currentTimeMillis();
            
            String response = aiClient.sendMessage(
                "You are a helpful programming assistant. Provide clear, accurate, and practical answers.",
                question
            );
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Format and display the response
            System.out.println(FormattingUtil.formatWithColor("Claude AI Response:", FormattingUtil.ANSI_GREEN + FormattingUtil.ANSI_BOLD));
            System.out.println(FormattingUtil.formatWithColor("==================", FormattingUtil.ANSI_GREEN));
            System.out.println(response);
            System.out.println();
            
            // Show timing information
            String formattedDuration = FormattingUtil.formatDuration(duration);
            System.out.println(FormattingUtil.formatWithColor("Response time: " + formattedDuration, FormattingUtil.ANSI_BLUE));
            
        } catch (Exception e) {
            log.error("Failed to get response from Claude AI", e);
            throw new UserError("Failed to get response from Claude AI: " + e.getMessage());
        }
    }
}
