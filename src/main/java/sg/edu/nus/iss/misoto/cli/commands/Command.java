package sg.edu.nus.iss.misoto.cli.commands;

import java.util.List;

/**
 * Interface representing a CLI command
 */
public interface Command {
    
    /**
     * Get the command name
     */
    String getName();
    
    /**
     * Get the command description
     */
    String getDescription();
    
    /**
     * Get the command category (optional)
     */
    String getCategory();
    
    /**
     * Check if the command is hidden from help display
     */
    boolean isHidden();
    
    /**
     * Check if the command requires authentication
     */
    boolean requiresAuth();
    
    /**
     * Get command usage information
     */
    String getUsage();
    
    /**
     * Get command examples
     */
    List<String> getExamples();
    
    /**
     * Execute the command with given arguments
     */
    void execute(List<String> args) throws Exception;
}
