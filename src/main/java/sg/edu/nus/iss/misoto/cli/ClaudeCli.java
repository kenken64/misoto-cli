package sg.edu.nus.iss.misoto.cli;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import sg.edu.nus.iss.misoto.cli.commands.CommandRegistry;
import sg.edu.nus.iss.misoto.cli.commands.CommandExecutor;
import sg.edu.nus.iss.misoto.cli.auth.AuthManager;
import sg.edu.nus.iss.misoto.cli.ai.AiClient;
import sg.edu.nus.iss.misoto.cli.config.ConfigManager;
import sg.edu.nus.iss.misoto.cli.errors.UserError;
import sg.edu.nus.iss.misoto.cli.errors.ErrorFormatter;
import sg.edu.nus.iss.misoto.cli.telemetry.TelemetryService;
import sg.edu.nus.iss.misoto.cli.telemetry.TelemetryEventType;
import sg.edu.nus.iss.misoto.cli.agent.AgentService;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Claude Code CLI
 * 
 * Main entry point for the Claude Code CLI tool. Handles command-line
 * argument parsing, command dispatching, and error handling.
 */
@Component
@Slf4j
public class ClaudeCli implements CommandLineRunner {
    
    // Application version - could be read from properties file
    private static final String VERSION = "1.0.0";
    
    @Autowired
    private Environment environment;
    
    @Autowired
    private CommandRegistry commandRegistry;
    
    @Autowired
    private CommandExecutor commandExecutor;
    
    @Autowired
    private AuthManager authManager;
    
    @Autowired
    private AiClient aiClient;
    
    @Autowired
    private ConfigManager configManager;
    @Autowired
    private ErrorFormatter errorFormatter;
    
    @Autowired
    private TelemetryService telemetryService;
    
    @Autowired(required = false)
    private AgentService agentService;
    
    @Override
    public void run(String... args) throws Exception {
        // Skip CLI execution during tests
        if (isTestProfile()) {
            log.debug("Skipping CLI execution in test profile");
            return;
        }
        try {
            // Parse command-line arguments first to get any config options
            ParsedCommand parsedCommand = parseCommandLineArgs(args);
            
            // Build CLI options for configuration
            Map<String, Object> cliOptions = buildCliOptions(args);
            
            // Load configuration
            configManager.loadConfig(cliOptions);
              // Initialize services
            var telemetryConfig = new TelemetryService.TelemetryConfig();
            telemetryConfig.setEnabled(configManager.getConfig().isTelemetryEnabled());
            telemetryConfig.setEndpoint(configManager.getConfig().getTelemetryEndpoint());
            telemetryService.initialize(telemetryConfig);
            
            // Track CLI start event
            telemetryService.recordEvent(TelemetryEventType.CLI_START, Map.of(
                "args_count", args.length,
                "command", parsedCommand.getCommandName() != null ? parsedCommand.getCommandName() : "none"
            ));
            
            // Initialize authentication
            authManager.initialize();
            
            // Handle special commands first
            if (parsedCommand.isHelpCommand()) {
                displayHelp(parsedCommand.getHelpTarget());
                return;
            }
            
            if (parsedCommand.isVersionCommand()) {
                displayVersion();
                return;
            }
            
            if (parsedCommand.isEmpty()) {
                displayHelp(null);
                return;
            }
            
            // Get the command
            var command = commandRegistry.get(parsedCommand.getCommandName());
              if (command.isEmpty()) {
                System.err.println("Unknown command: " + parsedCommand.getCommandName());
                System.err.println("Use \"claude-code help\" to see available commands.");
                
                // Track error event
                telemetryService.recordEvent(TelemetryEventType.ERROR_OCCURRED, Map.of(
                    "error_type", "unknown_command",
                    "command", parsedCommand.getCommandName()
                ));
                
                System.exit(1);
            }
            
            var cmd = command.get();
              // Check if command requires authentication
            if (cmd.requiresAuth() && !authManager.isAuthenticated()) {
                System.err.println("Command '" + parsedCommand.getCommandName() + "' requires authentication.");
                System.err.println("Please log in using the \"claude-code login\" command first.");
                
                // Track authentication error
                telemetryService.recordEvent(TelemetryEventType.ERROR_OCCURRED, Map.of(
                    "error_type", "authentication_required",
                    "command", parsedCommand.getCommandName()
                ));
                
                System.exit(1);
            }
            
            // Initialize AI if required
            if (cmd.requiresAuth()) {
                aiClient.initialize();
            }
            
            // Track command execution
            telemetryService.recordEvent(TelemetryEventType.COMMAND_EXECUTE, Map.of(
                "command", parsedCommand.getCommandName(),
                "args_count", parsedCommand.getArgs().size()
            ));
            
            // Execute the command
            commandExecutor.execute(parsedCommand.getCommandName(), parsedCommand.getArgs());
              } catch (Exception error) {
            // Track error
            telemetryService.recordEvent(TelemetryEventType.ERROR_OCCURRED, Map.of(
                "error_type", error.getClass().getSimpleName(),
                "error_message", error.getMessage() != null ? error.getMessage() : "unknown"
            ));
            
            handleError(error);
        } finally {
            // Shutdown services gracefully
            if (agentService != null && agentService.isRunning()) {
                showAgentShutdownProgress();
            }
            
            if (telemetryService != null) {
                telemetryService.shutdown();
            }
        }
    }

    /**
     * Show animated progress bar for agent shutdown
     */
    private void showAgentShutdownProgress() {
        final int barWidth = 40;
        final String greenBar = "\u001B[42m \u001B[0m";  // Green background
        final String grayBar = "\u001B[100m \u001B[0m";   // Gray background
        final String yellow = "\u001B[33m";
        final String green = "\u001B[32m";
        final String red = "\u001B[31m";
        final String reset = "\u001B[0m";
        
        System.out.print(yellow + "Preset: agent_shutdown " + reset);
        System.out.flush();
        
        long startTime = System.currentTimeMillis();
        agentService.stopAgent();
        
        // Always animate progress bar from 0 to 100%
        int totalSteps = 100; // 100 steps for smooth animation
        for (int i = 0; i <= totalSteps; i++) {
            // Calculate progress
            int progress = (i * barWidth) / totalSteps;
            int percentage = i;
            long elapsed = System.currentTimeMillis() - startTime;
            
            // Clear line and redraw
            System.out.print("\r" + yellow + "Preset: agent_shutdown " + reset);
            
            // Draw progress bar
            for (int j = 0; j < barWidth; j++) {
                if (j < progress) {
                    System.out.print(greenBar);
                } else {
                    System.out.print(grayBar);
                }
            }
            
            // Calculate ETA
            String eta;
            if (i == 0) {
                eta = "calculating";
            } else if (i == totalSteps) {
                eta = "complete";
            } else {
                long avgTimePerStep = elapsed / i;
                long remainingSteps = totalSteps - i;
                long etaMs = remainingSteps * avgTimePerStep;
                eta = etaMs + "ms";
            }
            
            // Show completion when done
            if (i == totalSteps) {
                System.out.print(" " + green + percentage + "%" + reset + " | ETA: " + eta + " | " + elapsed + "ms");
            } else {
                System.out.print(" " + percentage + "% | ETA: " + eta + " | " + elapsed + "ms");
            }
            System.out.flush();
            
            // Delay between updates
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        System.out.println(); // New line after completion
    }

    /**
     * Display help information
     */
    private void displayHelp(String commandName) {
        if (commandName != null && !commandName.equals("help")) {
            // Display help for a specific command
            var command = commandRegistry.get(commandName);
            
            if (command.isEmpty()) {
                System.err.println("Unknown command: " + commandName);
                System.err.println("Use \"claude-code help\" to see available commands.");
                System.exit(1);
            }
            
            System.out.println(commandRegistry.generateCommandHelp(command.get()));
            return;
        }
        
        // Display general help
        System.out.println(String.format("""
            
            Claude Code CLI v%s
            
            A command-line interface for interacting with Claude AI for code assistance,
            generation, refactoring, and more.
            
            Usage:
              claude-code <command> [arguments] [options]
            
            Available Commands:""", VERSION));
        
        // Group commands by category
        var categories = commandRegistry.getCategories();
        
        // Commands without a category
        var uncategorizedCommands = commandRegistry.list()
                .stream()
                .filter(cmd -> cmd.getCategory() == null && !cmd.isHidden())
                .sorted((a, b) -> a.getName().compareTo(b.getName()))
                .toList();
        
        if (!uncategorizedCommands.isEmpty()) {
            uncategorizedCommands.forEach(command -> 
                System.out.printf("  %-15s %s%n", command.getName(), command.getDescription())
            );
            System.out.println();
        }
        
        // Commands with categories
        for (String category : categories) {
            System.out.println(category + ":");
            
            var commands = commandRegistry.getByCategory(category)
                    .stream()
                    .filter(cmd -> !cmd.isHidden())
                    .sorted((a, b) -> a.getName().compareTo(b.getName()))
                    .toList();
            
            commands.forEach(command -> 
                System.out.printf("  %-15s %s%n", command.getName(), command.getDescription())
            );
            
            System.out.println();
        }
        
        System.out.println("""
            For more information on a specific command, use:
              claude-code help <command>
            
            Examples:
              $ claude-code ask "How do I implement a binary search tree in Java?"
              $ claude-code explain path/to/file.java
              $ claude-code refactor path/to/file.java --focus=performance
              $ claude-code fix path/to/code.java
            """);
    }

    /**
     * Display version information
     */
    private void displayVersion() {
        System.out.println("Claude Code CLI v" + VERSION);
    }

    /**
     * Parse command-line arguments
     */
    private ParsedCommand parseCommandLineArgs(String[] args) {
        // Handle empty command
        if (args.length == 0) {
            return ParsedCommand.empty();
        }
        
        // Extract command name
        String commandName = args[0].toLowerCase();
        
        // Handle help command
        if ("help".equals(commandName)) {
            String helpTarget = args.length > 1 ? args[1] : null;
            return ParsedCommand.help(helpTarget);
        }
        
        // Handle version command
        if ("version".equals(commandName) || "--version".equals(commandName) || "-v".equals(commandName)) {
            return ParsedCommand.version();
        }
        
        // Return regular command
        List<String> commandArgs = args.length > 1 ? Arrays.asList(Arrays.copyOfRange(args, 1, args.length)) : List.of();
        return ParsedCommand.command(commandName, commandArgs);
    }

    /**
     * Build CLI options map for configuration
     */
    private Map<String, Object> buildCliOptions(String[] args) {
        Map<String, Object> options = new HashMap<>();
        
        // Simple flag parsing for configuration options
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            
            switch (arg) {
                case "--verbose", "-v" -> options.put("verbose", true);
                case "--quiet", "-q" -> options.put("quiet", true);
                case "--debug", "-d" -> options.put("debug", true);
                case "--config", "-c" -> {
                    if (i + 1 < args.length) {
                        options.put("config", args[i + 1]);
                        i++; // Skip next argument as it's the config file path
                    }
                }
            }
        }
        
        return options;
    }

    /**
     * Handle errors
     */
    private void handleError(Exception error) {
        String formattedError = errorFormatter.formatErrorForDisplay(error);
        System.err.println(formattedError);
        
        // Exit with error code
        if (error instanceof UserError) {
            System.exit(1);
        } else {
            // Unexpected error, use a different exit code
            log.error("Unexpected error in CLI", error);
            System.exit(2);
        }
    }
    
    /**
     * Data class to hold parsed command information
     */
    private static class ParsedCommand {
        private final CommandType type;
        private final String commandName;
        private final List<String> args;
        private final String helpTarget;
        
        private ParsedCommand(CommandType type, String commandName, List<String> args, String helpTarget) {
            this.type = type;
            this.commandName = commandName;
            this.args = args;
            this.helpTarget = helpTarget;
        }
        
        public static ParsedCommand empty() {
            return new ParsedCommand(CommandType.EMPTY, null, List.of(), null);
        }
        
        public static ParsedCommand help(String target) {
            return new ParsedCommand(CommandType.HELP, null, List.of(), target);
        }
        
        public static ParsedCommand version() {
            return new ParsedCommand(CommandType.VERSION, null, List.of(), null);
        }
        
        public static ParsedCommand command(String name, List<String> args) {
            return new ParsedCommand(CommandType.COMMAND, name, args, null);
        }
        
        public boolean isEmpty() { return type == CommandType.EMPTY; }
        public boolean isHelpCommand() { return type == CommandType.HELP; }
        public boolean isVersionCommand() { return type == CommandType.VERSION; }
        public String getCommandName() { return commandName; }
        public List<String> getArgs() { return args; }
        public String getHelpTarget() { return helpTarget; }
        
        private enum CommandType {
            EMPTY, HELP, VERSION, COMMAND
        }
    }
    
    /**
     * Check if running in test profile
     */
    private boolean isTestProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("test".equals(profile)) {
                return true;
            }
        }
        return false;
    }
}
