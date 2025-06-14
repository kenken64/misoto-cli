package sg.edu.nus.iss.misoto.cli.commands;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry for managing CLI commands
 */
@Service
@Slf4j
public class CommandRegistry {
    
    private final Map<String, Command> commands = new ConcurrentHashMap<>();
    
    /**
     * Register a command
     */
    public void register(Command command) {
        commands.put(command.getName().toLowerCase(), command);
        log.debug("Registered command: {}", command.getName());
    }
    
    /**
     * Get a command by name
     */
    public Optional<Command> get(String name) {
        return Optional.ofNullable(commands.get(name.toLowerCase()));
    }
    
    /**
     * List all commands
     */
    public List<Command> list() {
        return new ArrayList<>(commands.values());
    }
    
    /**
     * Get all categories
     */
    public Set<String> getCategories() {
        return commands.values().stream()
                .map(Command::getCategory)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
    
    /**
     * Get commands by category
     */
    public List<Command> getByCategory(String category) {
        return commands.values().stream()
                .filter(cmd -> Objects.equals(cmd.getCategory(), category))
                .collect(Collectors.toList());
    }
    
    /**
     * Generate help text for a specific command
     */
    public String generateCommandHelp(Command command) {
        StringBuilder help = new StringBuilder();
        
        help.append(String.format("Command: %s%n", command.getName()));
        help.append(String.format("Description: %s%n", command.getDescription()));
        
        if (command.getCategory() != null) {
            help.append(String.format("Category: %s%n", command.getCategory()));
        }
        
        if (command.getUsage() != null) {
            help.append(String.format("Usage: %s%n", command.getUsage()));
        }
        
        if (command.requiresAuth()) {
            help.append("Requires Authentication: Yes%n");
        }
        
        if (!command.getExamples().isEmpty()) {
            help.append("%nExamples:%n");
            command.getExamples().forEach(example -> 
                help.append(String.format("  %s%n", example))
            );
        }
        
        return help.toString();
    }
    
    /**
     * Get total number of registered commands
     */
    public int size() {
        return commands.size();
    }
    
    /**
     * Check if a command is registered
     */
    public boolean isRegistered(String name) {
        return commands.containsKey(name.toLowerCase());
    }
}
