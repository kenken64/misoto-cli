package sg.edu.nus.iss.misoto.cli.commands;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Service for executing CLI commands
 */
@Service
@Slf4j
public class CommandExecutor {
    
    @Autowired
    private CommandRegistry commandRegistry;
    
    /**
     * Execute a command by name with arguments
     */
    public void execute(String commandName, List<String> args) throws Exception {
        log.debug("Executing command: {} with args: {}", commandName, args);
        
        var command = commandRegistry.get(commandName);
        if (command.isEmpty()) {
            throw new IllegalArgumentException("Unknown command: " + commandName);
        }
        
        try {
            command.get().execute(args);
            log.debug("Command {} executed successfully", commandName);
        } catch (Exception e) {
            log.error("Error executing command {}: {}", commandName, e.getMessage());
            throw e;
        }
    }
}
