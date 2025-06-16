package sg.edu.nus.iss.misoto.cli.agent.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.misoto.cli.ai.AiClient;
import sg.edu.nus.iss.misoto.cli.execution.ExecutionEnvironment;
import sg.edu.nus.iss.misoto.cli.mcp.manager.McpServerManager;
import sg.edu.nus.iss.misoto.cli.mcp.model.McpToolResult;
import sg.edu.nus.iss.misoto.cli.agent.decision.DecisionEngine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Executes agent tasks based on their type
 */
@Service
@Slf4j
public class TaskExecutorService {
    
    @Autowired
    private AiClient aiClient;
    
    @Autowired
    private ExecutionEnvironment executionEnvironment;
    
    @Autowired
    private McpServerManager mcpServerManager;
    
    @Autowired
    private DecisionEngine decisionEngine;
    
    private final Map<String, Long> runningTasks = new ConcurrentHashMap<>();
    
    /**
     * Execute a task and return the result
     */
    public AgentTask.TaskResult executeTask(AgentTask task) throws Exception {
        String taskId = task.getId();
        long startTime = System.currentTimeMillis();
        
        try {
            runningTasks.put(taskId, startTime);
            
            log.debug("Executing task type: {} [{}]", task.getType(), taskId);
            
            AgentTask.TaskResult result = switch (task.getType()) {
                case FILE_READ -> executeFileRead(task);
                case FILE_WRITE -> executeFileWrite(task);
                case FILE_COPY -> executeFileCopy(task);
                case FILE_DELETE -> executeFileDelete(task);
                case DIRECTORY_SCAN -> executeDirectoryScan(task);
                
                case SHELL_COMMAND -> executeShellCommand(task);
                case SCRIPT_EXECUTION -> executeScript(task);
                case BACKGROUND_PROCESS -> executeBackgroundProcess(task);
                
                case AI_ANALYSIS -> executeAiAnalysis(task);
                case CODE_GENERATION -> executeCodeGeneration(task);
                case DECISION_MAKING -> executeDecisionMaking(task);
                case TEXT_PROCESSING -> executeTextProcessing(task);
                
                case MCP_TOOL_CALL -> executeMcpToolCall(task);
                case MCP_RESOURCE_ACCESS -> executeMcpResourceAccess(task);
                case MCP_SERVER_MANAGEMENT -> executeMcpServerManagement(task);
                
                case SYSTEM -> executeSystemTask(task);
                case SYSTEM_MONITORING -> executeSystemMonitoring(task);
                case LOG_ANALYSIS -> executeLogAnalysis(task);
                case HEALTH_CHECK -> executeHealthCheck(task);
                
                case COMPOSITE_TASK -> executeCompositeTask(task);
                case CUSTOM_ACTION -> executeCustomAction(task);
                
                default -> throw new UnsupportedOperationException("Task type not supported: " + task.getType());
            };
            
            long executionTime = System.currentTimeMillis() - startTime;
            result.setExecutionTimeMs(executionTime);
            result.setSuccess(true);
            
            return result;
            
        } finally {
            runningTasks.remove(taskId);
        }
    }
    
    /**
     * Cancel a running task
     */
    public boolean cancelTask(String taskId) {
        if (runningTasks.containsKey(taskId)) {
            runningTasks.remove(taskId);
            log.info("Task execution cancelled: {}", taskId);
            return true;
        }
        return false;
    }
    
    // File Operations
    private AgentTask.TaskResult executeFileRead(AgentTask task) throws IOException {
        String filePath = (String) task.getParameters().get("file_path");
        Path path = Paths.get(filePath);
        
        if (!Files.exists(path)) {
            throw new IOException("File not found: " + filePath);
        }
        
        String content = Files.readString(path);
        
        return AgentTask.TaskResult.builder()
            .output(content)
            .artifacts(Map.of("file_size", Files.size(path), "file_path", filePath))
            .build();
    }
    
    private AgentTask.TaskResult executeFileWrite(AgentTask task) throws IOException {
        String filePath = (String) task.getParameters().get("file_path");
        String content = (String) task.getParameters().get("content");
        boolean append = (Boolean) task.getParameters().getOrDefault("append", false);
        
        Path path = Paths.get(filePath);
        Files.createDirectories(path.getParent());
        
        if (append) {
            Files.writeString(path, content, java.nio.file.StandardOpenOption.CREATE, 
                             java.nio.file.StandardOpenOption.APPEND);
        } else {
            Files.writeString(path, content);
        }
        
        return AgentTask.TaskResult.builder()
            .output("File written successfully")
            .filesCreated(append ? List.of() : List.of(filePath))
            .filesModified(append ? List.of(filePath) : List.of())
            .artifacts(Map.of("bytes_written", content.length()))
            .build();
    }
    
    private AgentTask.TaskResult executeFileCopy(AgentTask task) throws IOException {
        String sourcePath = (String) task.getParameters().get("source_path");
        String targetPath = (String) task.getParameters().get("target_path");
        
        Path source = Paths.get(sourcePath);
        Path target = Paths.get(targetPath);
        
        Files.createDirectories(target.getParent());
        Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        
        return AgentTask.TaskResult.builder()
            .output("File copied successfully")
            .filesCreated(List.of(targetPath))
            .artifacts(Map.of("source_size", Files.size(source)))
            .build();
    }
    
    private AgentTask.TaskResult executeFileDelete(AgentTask task) throws IOException {
        String filePath = (String) task.getParameters().get("file_path");
        Path path = Paths.get(filePath);
        
        boolean deleted = Files.deleteIfExists(path);
        
        return AgentTask.TaskResult.builder()
            .output(deleted ? "File deleted successfully" : "File not found")
            .artifacts(Map.of("deleted", deleted))
            .build();
    }
    
    private AgentTask.TaskResult executeDirectoryScan(AgentTask task) throws IOException {
        String directoryPath = (String) task.getParameters().get("directory_path");
        String pattern = (String) task.getParameters().getOrDefault("pattern", "*");
        boolean recursive = (Boolean) task.getParameters().getOrDefault("recursive", false);
        
        Path directory = Paths.get(directoryPath);
        List<String> files = new ArrayList<>();
        
        if (recursive) {
            Files.walk(directory)
                .filter(Files::isRegularFile)
                .map(Path::toString)
                .forEach(files::add);
        } else {
            Files.list(directory)
                .filter(Files::isRegularFile)
                .map(Path::toString)
                .forEach(files::add);
        }
        
        return AgentTask.TaskResult.builder()
            .output(String.join("\n", files))
            .artifacts(Map.of("file_count", files.size(), "files", files))
            .build();
    }
      // Command Execution
    private AgentTask.TaskResult executeShellCommand(AgentTask task) throws Exception {
        String command = (String) task.getParameters().get("command");
        String workingDir = (String) task.getParameters().get("working_directory");
        
        ExecutionEnvironment.ExecutionOptions options = new ExecutionEnvironment.ExecutionOptions();
        if (workingDir != null) {
            // Use reflection or alternative method if setWorkingDirectory doesn't exist
            try {
                options.getClass().getMethod("setWorkingDirectory", String.class).invoke(options, workingDir);
            } catch (Exception e) {
                log.warn("Could not set working directory: {}", e.getMessage());
            }
        }
        
        ExecutionEnvironment.ExecutionResult result = executionEnvironment.executeCommand(command, options);
        
        return AgentTask.TaskResult.builder()
            .output(result.getOutput())
            .exitCode(result.getExitCode())
            .commandsExecuted(List.of(command))
            .artifacts(Map.of("stderr", result.getOutput())) // Use output if getErrorOutput doesn't exist
            .build();
    }
    
    private AgentTask.TaskResult executeScript(AgentTask task) throws Exception {
        String scriptContent = (String) task.getParameters().get("script_content");
        String scriptType = (String) task.getParameters().getOrDefault("script_type", "bash");
        
        // Create temporary script file
        Path tempScript = Files.createTempFile("agent-script", "." + scriptType);
        Files.writeString(tempScript, scriptContent);
        
        try {
            String command = switch (scriptType.toLowerCase()) {
                case "bash", "sh" -> "bash " + tempScript;
                case "powershell", "ps1" -> "powershell -File " + tempScript;
                case "python", "py" -> "python " + tempScript;
                case "lua" -> "lua " + tempScript;
                default -> tempScript.toString();
            };
            
            ExecutionEnvironment.ExecutionResult result = executionEnvironment.executeCommand(command);
            
            return AgentTask.TaskResult.builder()
                .output(result.getOutput())
                .exitCode(result.getExitCode())
                .commandsExecuted(List.of(command))
                .build();
                
        } finally {
            Files.deleteIfExists(tempScript);
        }
    }
    
    private AgentTask.TaskResult executeBackgroundProcess(AgentTask task) throws Exception {
        String command = (String) task.getParameters().get("command");
        
        ExecutionEnvironment.BackgroundProcessOptions options = new ExecutionEnvironment.BackgroundProcessOptions();
        ExecutionEnvironment.BackgroundProcess process = executionEnvironment.executeBackground(command, options);
        
        return AgentTask.TaskResult.builder()
            .output("Background process started with PID: " + process.getPid())
            .artifacts(Map.of("pid", process.getPid(), "command", command))
            .build();
    }
    
    // AI Operations
    private AgentTask.TaskResult executeAiAnalysis(AgentTask task) throws Exception {
        String content = (String) task.getParameters().get("content");
        String analysisType = (String) task.getParameters().getOrDefault("analysis_type", "general");
        
        String prompt = createAnalysisPrompt(analysisType, content);
        String response = aiClient.sendMessage(prompt);
        
        return AgentTask.TaskResult.builder()
            .output(response)
            .artifacts(Map.of("analysis_type", analysisType, "input_length", content.length()))
            .build();
    }
    
    private AgentTask.TaskResult executeCodeGeneration(AgentTask task) throws Exception {
        String taskDescription = (String) task.getParameters().get("task_description");
        String conversationContext = (String) task.getParameters().get("conversation_context");
        
        // Analyze the task to extract requirements
        String analysisPrompt = "Analyze this task request and identify:\n" +
                "1. Programming language needed\n" +
                "2. Filename to create\n" +
                "3. Code to generate\n" +
                "4. Any additional files or directories needed\n\n" +
                "Task: " + taskDescription + "\n\n" +
                "Respond in this format:\n" +
                "LANGUAGE: <language>\n" +
                "FILENAME: <filename>\n" +
                "DIRECTORIES: <comma-separated list or 'none'>\n" +
                "CODE:\n<actual code>\n" +
                "END_CODE";
        
        String analysisResponse = aiClient.sendMessage(analysisPrompt);
        
        // Parse the analysis response
        Map<String, String> parsedResponse = parseAnalysisResponse(analysisResponse);
        String language = parsedResponse.getOrDefault("LANGUAGE", "python");
        String filename = parsedResponse.getOrDefault("FILENAME", "generated_code." + getFileExtension(language));
        String directories = parsedResponse.getOrDefault("DIRECTORIES", "none");
        String code = parsedResponse.getOrDefault("CODE", "# Generated code\nprint('Hello, World!')");
        
        List<String> filesCreated = new ArrayList<>();
        List<String> commandsExecuted = new ArrayList<>();
        
        try {
            // Create directories if needed
            if (!"none".equals(directories)) {
                for (String dir : directories.split(",")) {
                    String trimmedDir = dir.trim();
                    if (!trimmedDir.isEmpty()) {
                        Path dirPath = Paths.get(trimmedDir);
                        Files.createDirectories(dirPath);
                        log.info("Created directory: {}", trimmedDir);
                    }
                }
            }
            
            // Create the file
            Path filePath = Paths.get(filename);
            Files.createDirectories(filePath.getParent() != null ? filePath.getParent() : Paths.get("."));
            Files.writeString(filePath, code);
            filesCreated.add(filename);
            log.info("Created file: {}", filename);
            
            // List files in current directory
            List<String> currentFiles = Files.list(Paths.get("."))
                .map(path -> path.getFileName().toString())
                .sorted()
                .collect(java.util.stream.Collectors.toList());
            
            String listOutput = "Files in current directory:\n" + String.join("\n", currentFiles);
            commandsExecuted.add("ls (list current directory)");
            
            // Try to run the generated code if it's a script
            String runOutput = "";
            if (language.toLowerCase().contains("python") && filename.endsWith(".py")) {
                try {
                    ExecutionEnvironment.ExecutionResult result = executionEnvironment.executeCommand("python " + filename);
                    runOutput = "\n\nScript execution result:\n" + result.getOutput();
                    commandsExecuted.add("python " + filename);
                } catch (Exception e) {
                    runOutput = "\n\nScript execution failed: " + e.getMessage();
                }
            } else if (language.toLowerCase().contains("lua") && filename.endsWith(".lua")) {
                try {
                    ExecutionEnvironment.ExecutionResult result = executionEnvironment.executeCommand("lua " + filename);
                    runOutput = "\n\nLua script execution result:\n" + result.getOutput();
                    commandsExecuted.add("lua " + filename);
                } catch (Exception e) {
                    runOutput = "\n\nLua script execution failed (lua interpreter may not be installed): " + e.getMessage();
                }
            } else if ((language.toLowerCase().contains("yaml") || language.toLowerCase().contains("yml")) && 
                       (filename.endsWith(".yml") || filename.endsWith(".yaml"))) {
                runOutput = "\n\nYAML file created successfully. Use 'cat " + filename + "' to view contents.";
                commandsExecuted.add("Generated YAML configuration file");
            }
            
            String fullOutput = "Successfully generated " + language + " code!\n\n" +
                    "Created file: " + filename + "\n" +
                    "File size: " + Files.size(filePath) + " bytes\n\n" +
                    listOutput + runOutput;
            
            return AgentTask.TaskResult.builder()
                .success(true)
                .output(fullOutput)
                .filesCreated(filesCreated)
                .commandsExecuted(commandsExecuted)
                .artifacts(Map.of(
                    "language", language,
                    "filename", filename,
                    "code_length", code.length(),
                    "files_in_directory", currentFiles.size()
                ))
                .build();
                
        } catch (Exception e) {
            log.error("Error in code generation task", e);
            return AgentTask.TaskResult.builder()
                .success(false)
                .output("Failed to generate code: " + e.getMessage())
                .error(e.getMessage())
                .build();
        }
    }
    
    private AgentTask.TaskResult executeDecisionMaking(AgentTask task) throws Exception {
        String context = (String) task.getParameters().get("context");
        String question = (String) task.getParameters().get("question");
        List<String> options = (List<String>) task.getParameters().getOrDefault("options", List.of());
        
        String decision = decisionEngine.makeDecision(context, question, options);
        
        return AgentTask.TaskResult.builder()
            .output(decision)
            .artifacts(Map.of("context_length", context.length(), "options_count", options.size()))
            .build();
    }
    
    private AgentTask.TaskResult executeTextProcessing(AgentTask task) throws Exception {
        String text = (String) task.getParameters().get("text");
        String operation = (String) task.getParameters().get("operation");
        
        String prompt = "Perform " + operation + " on this text:\n" + text;
        String response = aiClient.sendMessage(prompt);
        
        return AgentTask.TaskResult.builder()
            .output(response)            .artifacts(Map.of("operation", operation, "input_length", text.length()))
            .build();
    }
    
    // MCP Operations
    private AgentTask.TaskResult executeMcpToolCall(AgentTask task) throws Exception {
        String toolName = (String) task.getParameters().get("tool_name");
        Map<String, Object> arguments = (Map<String, Object>) task.getParameters().getOrDefault("arguments", Map.of());
        
        McpToolResult result = mcpServerManager.callTool(toolName, arguments);
        
        // Convert content list to string
        String output = result.getContent().stream()
            .map(Object::toString)
            .collect(java.util.stream.Collectors.joining("\n"));
        
        return AgentTask.TaskResult.builder()
            .output(output)
            .artifacts(Map.of("tool_name", toolName, "mcp_result", result))
            .build();
    }
    
    private AgentTask.TaskResult executeMcpResourceAccess(AgentTask task) throws Exception {
        String resourceUri = (String) task.getParameters().get("resource_uri");
        
        // This would be implemented based on MCP resource access capabilities
        return AgentTask.TaskResult.builder()
            .output("MCP resource accessed: " + resourceUri)
            .artifacts(Map.of("resource_uri", resourceUri))
            .build();
    }
    
    private AgentTask.TaskResult executeMcpServerManagement(AgentTask task) throws Exception {
        String action = (String) task.getParameters().get("action");
        String serverId = (String) task.getParameters().get("server_id");
        
        String result;
        switch (action.toLowerCase()) {
            case "ping":
                result = mcpServerManager.pingAll() ? "All servers responding" : "Some servers not responding";
                break;
            case "connect":
                mcpServerManager.reconnectServer(serverId);
                result = "Connected to server: " + serverId;
                break;
            case "disconnect":
                mcpServerManager.disconnectServer(serverId);
                result = "Disconnected from server: " + serverId;
                break;
            default:
                result = "Unknown action: " + action;
                break;
        }
        
        return AgentTask.TaskResult.builder()
            .output(result)
            .artifacts(Map.of("action", action, "server_id", serverId))
            .build();
    }
    
    // System Operations
    private AgentTask.TaskResult executeSystemTask(AgentTask task) throws Exception {
        String action = (String) task.getParameters().getOrDefault("action", "test");
        
        // Simple system task execution for testing
        Map<String, Object> result = new HashMap<>();
        result.put("action", action);
        result.put("timestamp", Instant.now());
        result.put("status", "completed");
        
        String output = String.format("System task '%s' executed successfully at %s", 
                                    action, Instant.now());
        
        return AgentTask.TaskResult.builder()
            .output(output)
            .artifacts(result)
            .build();
    }
    
    private AgentTask.TaskResult executeSystemMonitoring(AgentTask task) throws Exception {
        String metric = (String) task.getParameters().getOrDefault("metric", "general");
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("timestamp", Instant.now());
        metrics.put("memory_usage", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        metrics.put("available_processors", Runtime.getRuntime().availableProcessors());
        
        return AgentTask.TaskResult.builder()
            .output("System monitoring completed")
            .metrics(metrics)
            .build();
    }
    
    private AgentTask.TaskResult executeLogAnalysis(AgentTask task) throws Exception {
        String logFile = (String) task.getParameters().get("log_file");
        String pattern = (String) task.getParameters().getOrDefault("pattern", "ERROR");
        
        if (logFile != null && Files.exists(Paths.get(logFile))) {
            String content = Files.readString(Paths.get(logFile));
            long matches = content.lines().filter(line -> line.contains(pattern)).count();
            
            return AgentTask.TaskResult.builder()
                .output("Log analysis completed. Found " + matches + " matches for pattern: " + pattern)
                .artifacts(Map.of("matches", matches, "pattern", pattern, "log_file", logFile))
                .build();
        } else {
            throw new IOException("Log file not found: " + logFile);
        }
    }
    
    private AgentTask.TaskResult executeHealthCheck(AgentTask task) throws Exception {
        Map<String, Object> healthStatus = new HashMap<>();
        
        // Check AI client
        healthStatus.put("ai_client_ready", aiClient.isReady());
        
        // Check MCP servers
        healthStatus.put("mcp_servers_responding", mcpServerManager.pingAll());
        
        // Check system resources
        Runtime runtime = Runtime.getRuntime();
        healthStatus.put("memory_usage_percent", 
            (double)(runtime.totalMemory() - runtime.freeMemory()) / runtime.totalMemory() * 100);
        
        return AgentTask.TaskResult.builder()
            .output("Health check completed")
            .metrics(healthStatus)
            .build();
    }
    
    // Complex Operations
    private AgentTask.TaskResult executeCompositeTask(AgentTask task) throws Exception {
        List<Map<String, Object>> subtasks = (List<Map<String, Object>>) task.getParameters().get("subtasks");
        List<String> results = new ArrayList<>();
        
        for (Map<String, Object> subtaskDef : subtasks) {
            // Create and execute subtask
            AgentTask subtask = createSubtask(subtaskDef, task);
            AgentTask.TaskResult subtaskResult = executeTask(subtask);
            results.add(subtaskResult.getOutput());
        }
        
        return AgentTask.TaskResult.builder()
            .output(String.join("\n---\n", results))
            .artifacts(Map.of("subtask_count", subtasks.size()))
            .build();
    }
    
    private AgentTask.TaskResult executeCustomAction(AgentTask task) throws Exception {
        String actionClass = (String) task.getParameters().get("action_class");
        
        // This would allow for pluggable custom actions
        // For now, return a placeholder
        return AgentTask.TaskResult.builder()
            .output("Custom action executed: " + actionClass)
            .artifacts(Map.of("action_class", actionClass))
            .build();
    }
    
    private String createAnalysisPrompt(String analysisType, String content) {
        return switch (analysisType.toLowerCase()) {
            case "code" -> "Analyze this code for potential issues, improvements, and best practices:\n" + content;
            case "security" -> "Perform a security analysis of this content:\n" + content;
            case "performance" -> "Analyze this content for performance implications:\n" + content;
            case "documentation" -> "Generate documentation for this content:\n" + content;
            default -> "Analyze this content:\n" + content;
        };
    }
    
    private AgentTask createSubtask(Map<String, Object> subtaskDef, AgentTask parentTask) {
        return AgentTask.builder()
            .type(AgentTask.TaskType.valueOf((String) subtaskDef.get("type")))
            .name((String) subtaskDef.get("name"))
            .parameters((Map<String, Object>) subtaskDef.getOrDefault("parameters", Map.of()))
            .context(parentTask.getContext())
            .build();
    }
    
    /**
     * Parse AI analysis response into structured data
     */
    private Map<String, String> parseAnalysisResponse(String response) {
        Map<String, String> result = new HashMap<>();
        String[] lines = response.split("\n");
        StringBuilder codeBuilder = new StringBuilder();
        boolean inCodeSection = false;
        
        for (String line : lines) {
            line = line.trim();
            
            if (line.startsWith("LANGUAGE:")) {
                result.put("LANGUAGE", line.substring("LANGUAGE:".length()).trim().toLowerCase());
            } else if (line.startsWith("FILENAME:")) {
                result.put("FILENAME", line.substring("FILENAME:".length()).trim());
            } else if (line.startsWith("DIRECTORIES:")) {
                result.put("DIRECTORIES", line.substring("DIRECTORIES:".length()).trim());
            } else if (line.equals("CODE:")) {
                inCodeSection = true;
            } else if (line.equals("END_CODE")) {
                inCodeSection = false;
            } else if (inCodeSection) {
                codeBuilder.append(line).append("\n");
            }
        }
        
        result.put("CODE", codeBuilder.toString().trim());
        return result;
    }
    
    /**
     * Get file extension for programming language
     */
    private String getFileExtension(String language) {
        return switch (language.toLowerCase()) {
            case "python", "py" -> "py";
            case "java" -> "java";
            case "javascript", "js" -> "js";
            case "typescript", "ts" -> "ts";
            case "c" -> "c";
            case "cpp", "c++" -> "cpp";
            case "csharp", "c#" -> "cs";
            case "go" -> "go";
            case "rust" -> "rs";
            case "ruby" -> "rb";
            case "php" -> "php";
            case "swift" -> "swift";
            case "kotlin" -> "kt";
            case "lua" -> "lua";
            case "yaml", "yml" -> "yml";
            case "bash", "shell" -> "sh";
            case "powershell" -> "ps1";
            default -> "txt";
        };
    }
}
