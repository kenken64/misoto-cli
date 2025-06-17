package sg.edu.nus.iss.misoto.cli.commands.impl;

import sg.edu.nus.iss.misoto.cli.commands.Command;
import sg.edu.nus.iss.misoto.cli.ai.AiClient;
import sg.edu.nus.iss.misoto.cli.utils.FormattingUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Command for displaying AI model information
 */
@Component
@Slf4j
public class InfoCommand implements Command {
    
    @Autowired
    private AiClient aiClient;
    
    @Override
    public String getName() {
        return "info";
    }
    
    @Override
    public String getDescription() {
        return "Display AI model configuration information";
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
        return false;
    }
    
    @Override
    public String getUsage() {
        return "claude-code info";
    }
    
    @Override
    public List<String> getExamples() {
        return List.of("claude-code info");
    }
    
    @Override
    public void execute(List<String> args) throws Exception {
        System.out.println(FormattingUtil.formatWithColor("AI Model Information", FormattingUtil.ANSI_CYAN + FormattingUtil.ANSI_BOLD));
        System.out.println(FormattingUtil.formatWithColor("====================", FormattingUtil.ANSI_CYAN));
        System.out.println();
        
        try {
            String modelInfo = aiClient.getModelInfo();
            System.out.println("Current Configuration: " + FormattingUtil.formatWithColor(modelInfo, FormattingUtil.ANSI_GREEN));
            
            System.out.println();
            System.out.println("Details:");
            System.out.println("  Model: " + FormattingUtil.formatWithColor(aiClient.getModelName(), FormattingUtil.ANSI_CYAN));
            System.out.println("  Provider: " + FormattingUtil.formatWithColor(aiClient.getCurrentProvider(), FormattingUtil.ANSI_CYAN));
            System.out.println("  Model Info: " + FormattingUtil.formatWithColor(aiClient.getModelInfo(), FormattingUtil.ANSI_CYAN));
            
        } catch (Exception e) {
            System.out.println(FormattingUtil.formatWithColor("Error getting model information: " + e.getMessage(), FormattingUtil.ANSI_RED));
            log.debug("Error getting AI model information", e);
        }
    }
}
