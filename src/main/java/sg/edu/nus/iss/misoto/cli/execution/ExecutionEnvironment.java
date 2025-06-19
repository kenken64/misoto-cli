package sg.edu.nus.iss.misoto.cli.execution;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.misoto.cli.errors.UserError;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Execution Environment Service
 * 
 * Provides functionality for executing shell commands and scripts
 * in a controlled environment with proper error handling.
 */
@Service
@Slf4j
public class ExecutionEnvironment {
    
    private final ExecutionConfig config;
    private String workingDirectory;
    private final Map<String, String> environmentVariables;
    
    // Dangerous commands that should be blocked for safety
    private static final List<Pattern> DANGEROUS_COMMANDS = List.of(
        Pattern.compile("^\\s*rm\\s+(-rf?|--recursive)\\s+[/~]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\s*dd\\s+.*of=/dev/(disk|hd|sd)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\s*mkfs", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\s*:(\\s*)\\{(\\s*):(\\s*)\\|(\\s*):(\\s*)&(\\s*)\\}(\\s*);", Pattern.CASE_INSENSITIVE), // Fork bomb
        Pattern.compile("^\\s*sudo\\s+rm\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\s*format\\s+[a-z]:", Pattern.CASE_INSENSITIVE), // Windows format command
        Pattern.compile("^\\s*del\\s+/[sq]\\s+", Pattern.CASE_INSENSITIVE), // Windows recursive delete
        Pattern.compile("^```[^`]*```$", Pattern.CASE_INSENSITIVE), // Block commands that are just code blocks
        Pattern.compile("^```\\s*$", Pattern.CASE_INSENSITIVE) // Block empty code blocks
    );
    
    public ExecutionEnvironment(ExecutionConfig config) {
        this.config = config != null ? config : new ExecutionConfig();
        this.workingDirectory = System.getProperty("user.dir");
        this.environmentVariables = new HashMap<>(System.getenv());
        
        log.debug("Execution environment initialized with working directory: {}", workingDirectory);
    }
    
    /**
     * Execute a command synchronously
     */
    public ExecutionResult executeCommand(String command) {
        return executeCommand(command, new ExecutionOptions());
    }
    
    /**
     * Execute a command with options
     */
    public ExecutionResult executeCommand(String command, ExecutionOptions options) {
        log.debug("Executing command: {}", command);
        
        // Validate command
        validateCommand(command);
        
        long startTime = System.currentTimeMillis();
        
        try {
            ProcessBuilder processBuilder = createProcessBuilder(command, options);
            Process process = processBuilder.start();
            
            // Handle timeout
            boolean finished;
            if (options.getTimeout() > 0) {
                finished = process.waitFor(options.getTimeout(), TimeUnit.MILLISECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    throw new UserError("Command timed out after " + options.getTimeout() + "ms: " + command);
                }
            } else {
                process.waitFor();
                finished = true;
            }
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Read output
            String output = readProcessOutput(process, options);
            int exitCode = process.exitValue();
            
            ExecutionResult result = new ExecutionResult();
            result.setCommand(command);
            result.setOutput(output);
            result.setExitCode(exitCode);
            result.setDuration(duration);
            result.setSuccess(exitCode == 0);
            
            log.debug("Command completed: {} (exit code: {}, duration: {}ms)", command, exitCode, duration);
            
            return result;
            
        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to execute command: {}", command, e);
            
            ExecutionResult result = new ExecutionResult();
            result.setCommand(command);
            result.setOutput("");
            result.setExitCode(-1);
            result.setDuration(duration);
            result.setSuccess(false);
            result.setError(e);
            
            return result;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            long duration = System.currentTimeMillis() - startTime;
            
            ExecutionResult result = new ExecutionResult();
            result.setCommand(command);
            result.setOutput("");
            result.setExitCode(-1);
            result.setDuration(duration);
            result.setSuccess(false);
            result.setError(e);
            
            return result;
        }
    }
    
    /**
     * Execute a command asynchronously
     */
    public CompletableFuture<ExecutionResult> executeCommandAsync(String command) {
        return executeCommandAsync(command, new ExecutionOptions());
    }
    
    /**
     * Execute a command asynchronously with options
     */
    public CompletableFuture<ExecutionResult> executeCommandAsync(String command, ExecutionOptions options) {
        return CompletableFuture.supplyAsync(() -> executeCommand(command, options));
    }
    
    /**
     * Execute a background process
     */
    public BackgroundProcess executeBackground(String command, BackgroundProcessOptions options) {
        log.debug("Starting background process: {}", command);
        
        validateCommand(command);
        
        try {
            ProcessBuilder processBuilder = createProcessBuilder(command, options);
            Process process = processBuilder.start();
            
            BackgroundProcess backgroundProcess = new BackgroundProcess(process, options);
            
            log.debug("Background process started with PID: {}", backgroundProcess.getPid());
            
            return backgroundProcess;
            
        } catch (IOException e) {
            log.error("Failed to start background process: {}", command, e);
            throw new UserError("Failed to start background process: " + e.getMessage());
        }
    }
    
    /**
     * Check if a command is safe to execute
     */
    public boolean isCommandSafe(String command) {
        if (command == null || command.trim().isEmpty()) {
            return false;
        }
        
        String trimmedCommand = command.trim();
        
        // Check against dangerous patterns
        for (Pattern pattern : DANGEROUS_COMMANDS) {
            if (pattern.matcher(trimmedCommand).find()) {
                return false;
            }
        }
        
        // Check allowed commands if configured
        if (config.getAllowedCommands() != null && !config.getAllowedCommands().isEmpty()) {
            return config.getAllowedCommands().stream()
                    .anyMatch(allowed -> trimmedCommand.startsWith(allowed));
        }
        
        // Check blocked commands if configured
        if (config.getBlockedCommands() != null && !config.getBlockedCommands().isEmpty()) {
            return config.getBlockedCommands().stream()
                    .noneMatch(blocked -> trimmedCommand.startsWith(blocked));
        }
        
        return true;
    }
    
    /**
     * Set the working directory
     */
    public void setWorkingDirectory(String directory) {
        if (directory == null || directory.trim().isEmpty()) {
            throw new UserError("Working directory cannot be empty");
        }
        
        Path path = Paths.get(directory);
        if (!Files.exists(path)) {
            throw new UserError("Directory does not exist: " + directory);
        }
        
        if (!Files.isDirectory(path)) {
            throw new UserError("Path is not a directory: " + directory);
        }
        
        this.workingDirectory = path.toAbsolutePath().toString();
        log.debug("Working directory set to: {}", workingDirectory);
    }
    
    /**
     * Get the working directory
     */
    public String getWorkingDirectory() {
        return workingDirectory;
    }
    
    /**
     * Set an environment variable
     */
    public void setEnvironmentVariable(String name, String value) {
        if (name == null || name.trim().isEmpty()) {
            throw new UserError("Environment variable name cannot be empty");
        }
        
        environmentVariables.put(name, value);
        log.debug("Environment variable set: {}={}", name, value);
    }
    
    /**
     * Get an environment variable
     */
    public String getEnvironmentVariable(String name) {
        return environmentVariables.get(name);
    }
    
    /**
     * Get all environment variables
     */
    public Map<String, String> getEnvironmentVariables() {
        return new HashMap<>(environmentVariables);
    }
    
    /**
     * Get execution configuration
     */
    public ExecutionConfig getConfig() {
        return config;
    }
    
    // Private helper methods
    
    private void validateCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            throw new UserError("Command cannot be empty");
        }
        
        if (!isCommandSafe(command)) {
            throw new UserError("Command execution blocked for safety reasons: " + command);
        }
    }
    
    private ProcessBuilder createProcessBuilder(String command, ExecutionOptions options) {
        ProcessBuilder processBuilder;
        
        // Determine shell based on OS
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
        } else {
            String shell = options.getShell() != null ? options.getShell() : 
                          (config.getShell() != null ? config.getShell() : getDefaultShell());
            processBuilder = new ProcessBuilder(shell, "-c", command);
        }
        
        // Set working directory
        String workDir = options.getCwd() != null ? options.getCwd() : workingDirectory;
        processBuilder.directory(Paths.get(workDir).toFile());
        
        // Set environment variables
        Map<String, String> env = processBuilder.environment();
        env.putAll(environmentVariables);
        if (options.getEnv() != null) {
            env.putAll(options.getEnv());
        }
        
        // Configure output redirection
        if (options.isCaptureStderr()) {
            processBuilder.redirectErrorStream(true);
        }
        
        return processBuilder;
    }
    
    /**
     * Get the default shell based on the operating system
     */
    private String getDefaultShell() {
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("mac")) {
            // macOS uses zsh as default since macOS Catalina (10.15)
            return "/bin/zsh";
        } else if (os.contains("linux")) {
            // Linux typically uses bash
            return "/bin/bash";
        } else {
            // Fallback to bash for other Unix-like systems
            return "/bin/bash";
        }
    }
    
    private String readProcessOutput(Process process, ExecutionOptions options) throws IOException {
        StringBuilder output = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
                
                // Check max buffer size
                if (options.getMaxBuffer() > 0 && output.length() > options.getMaxBuffer()) {
                    output.append("... [Output truncated] ...");
                    break;
                }
            }
        }
        
        return output.toString();
    }
    
    /**
     * Result of a command execution
     */
    @Data
    public static class ExecutionResult {
        private String command;
        private String output;
        private int exitCode;
        private long duration;
        private boolean success;
        private Exception error;
    }
    
    /**
     * Command execution options
     */
    @Data
    public static class ExecutionOptions {
        private String cwd;
        private Map<String, String> env;
        private long timeout = 0; // 0 means no timeout
        private String shell;
        private int maxBuffer = 1024 * 1024; // 1MB default
        private boolean captureStderr = true;
    }
      /**
     * Background process options
     */
    @Data
    @EqualsAndHashCode(callSuper=false)
    public static class BackgroundProcessOptions extends ExecutionOptions {
        private Runnable onOutput;
        private Runnable onError;
        private Runnable onExit;
    }
    
    /**
     * Background process handle
     */
    public static class BackgroundProcess {
        private final Process process;
        private final BackgroundProcessOptions options;
        
        public BackgroundProcess(Process process, BackgroundProcessOptions options) {
            this.process = process;
            this.options = options;
            
            // Set up output monitoring if callbacks are provided
            if (options.getOnOutput() != null || options.getOnError() != null || options.getOnExit() != null) {
                setupMonitoring();
            }
        }
        
        public long getPid() {
            return process.pid();
        }
        
        public boolean isRunning() {
            return process.isAlive();
        }
        
        public boolean kill() {
            if (process.isAlive()) {
                process.destroyForcibly();
                return true;
            }
            return false;
        }
        
        public int waitFor() throws InterruptedException {
            return process.waitFor();
        }
        
        public int exitValue() {
            return process.exitValue();
        }
        
        private void setupMonitoring() {
            // This would typically set up background threads to monitor output
            // For simplicity, we'll just set up an exit handler
            CompletableFuture.runAsync(() -> {
                try {
                    process.waitFor();
                    if (options.getOnExit() != null) {
                        options.getOnExit().run();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
    }
    
    /**
     * Execution configuration
     */
    @Data
    public static class ExecutionConfig {
        private String shell;
        private List<String> allowedCommands;
        private List<String> blockedCommands;
        private boolean enableSafetyChecks = true;
        private long defaultTimeout = 0;
        private int maxConcurrentProcesses = 10;
    }
}
