package sg.edu.nus.iss.misoto.cli.commands.impl;

import sg.edu.nus.iss.misoto.cli.commands.Command;
import sg.edu.nus.iss.misoto.cli.ai.AiClient;
import sg.edu.nus.iss.misoto.cli.errors.UserError;
import sg.edu.nus.iss.misoto.cli.utils.FormattingUtil;
import sg.edu.nus.iss.misoto.cli.utils.ValidationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Command for explaining code files
 */
@Component
@Slf4j
public class ExplainCommand implements Command {
    
    @Autowired
    private AiClient aiClient;
    
    @Override
    public String getName() {
        return "explain";
    }
    
    @Override
    public String getDescription() {
        return "Explain code in a file using Claude AI";
    }
    
    @Override
    public String getCategory() {
        return "Code Analysis";
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
        return "claude-code explain <file-path> [--focus=<aspect>]";
    }
    
    @Override
    public List<String> getExamples() {
        return List.of(
            "claude-code explain src/main/java/com/example/Service.java",
            "claude-code explain app.py --focus=algorithm",
            "claude-code explain components/Button.tsx --focus=props"
        );
    }
      @Override
    public void execute(List<String> args) throws Exception {
        if (args.isEmpty()) {
            throw new UserError("Please provide a file path to explain. Usage: " + getUsage());
        }
        
        String filePath = args.get(0);
        String focus = null;
        
        // Parse focus parameter if provided
        for (String arg : args) {
            if (arg.startsWith("--focus=")) {
                focus = arg.substring("--focus=".length());
                break;
            }
        }
        
        // Validate file path
        if (!ValidationUtil.isNonEmptyString(filePath)) {
            throw new UserError("File path cannot be empty");
        }
        
        // Read the file
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new UserError("File does not exist: " + filePath);
        }
        
        if (!Files.isRegularFile(path)) {
            throw new UserError("Path is not a regular file: " + filePath);
        }
        
        String fileContent;
        long fileSize;
        try {
            fileContent = Files.readString(path);
            fileSize = Files.size(path);
        } catch (IOException e) {
            throw new UserError("Failed to read file: " + e.getMessage());
        }
        
        if (fileContent.trim().isEmpty()) {
            throw new UserError("File is empty: " + filePath);
        }
        
        // Check file size (reasonable limit for AI processing)
        if (fileSize > 100_000) { // 100KB limit
            System.out.println(FormattingUtil.formatWithColor(
                "Warning: Large file (" + FormattingUtil.formatFileSize(fileSize) + "). " +
                "Response may be truncated or take longer.", 
                FormattingUtil.ANSI_YELLOW
            ));
        }
        
        // Prepare the system prompt
        String systemPrompt = "You are a helpful code analysis assistant. " +
            "Explain the provided code clearly and concisely. " +
            "Focus on the purpose, structure, key components, and important logic. " +
            "Use simple language that both beginners and experienced developers can understand.";
        
        // Prepare the user message
        StringBuilder userMessage = new StringBuilder();
        userMessage.append("Please explain this code file: ").append(filePath).append("\n\n");
        
        if (focus != null) {
            userMessage.append("Please focus specifically on: ").append(focus).append("\n\n");
        }
        
        userMessage.append("```\n").append(fileContent).append("\n```");
        
        try {
            // Enhanced output with formatting and file info
            System.out.println(FormattingUtil.formatWithColor("Analyzing file: " + filePath, FormattingUtil.ANSI_CYAN));
            System.out.println(FormattingUtil.formatWithColor("File size: " + FormattingUtil.formatFileSize(fileSize), FormattingUtil.ANSI_BLUE));
            
            if (focus != null) {
                System.out.println(FormattingUtil.formatWithColor("Focus area: " + focus, FormattingUtil.ANSI_PURPLE));
            }
            
            System.out.println(FormattingUtil.formatWithColor("Processing...", FormattingUtil.ANSI_YELLOW));
            System.out.println();
            
            // Track timing
            long startTime = System.currentTimeMillis();
            
            String response = aiClient.sendMessage(systemPrompt, userMessage.toString());
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Format and display the response
            System.out.println(FormattingUtil.formatWithColor("Code Explanation:", FormattingUtil.ANSI_GREEN + FormattingUtil.ANSI_BOLD));
            System.out.println(FormattingUtil.formatWithColor("=================", FormattingUtil.ANSI_GREEN));
            System.out.println(response);
            System.out.println();
            
            // Show analysis info
            String formattedDuration = FormattingUtil.formatDuration(duration);
            System.out.println(FormattingUtil.formatWithColor("Analysis completed in: " + formattedDuration, FormattingUtil.ANSI_BLUE));
            
        } catch (Exception e) {
            log.error("Failed to get code explanation for file: {}", filePath, e);
            throw new UserError("Failed to get code explanation: " + e.getMessage());
        }
    }
}
