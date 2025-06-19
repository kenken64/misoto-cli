package sg.edu.nus.iss.misoto.cli.agent.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.misoto.cli.ai.AiClient;
import sg.edu.nus.iss.misoto.cli.execution.ExecutionEnvironment;
import sg.edu.nus.iss.misoto.cli.mcp.manager.McpServerManager;
import sg.edu.nus.iss.misoto.cli.mcp.model.McpToolResult;
import sg.edu.nus.iss.misoto.cli.agent.decision.DecisionEngine;
import sg.edu.nus.iss.misoto.cli.agent.context.FileContextService;
import sg.edu.nus.iss.misoto.cli.fileops.FileOperations;

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
    
    @Autowired
    private FileContextService fileContextService;
    
    @Autowired
    private FileOperations fileOperations;
    
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
        
        // Print the file operation
        System.out.println("🤖 Agent reading file: " + filePath);
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("file_path parameter is required and cannot be empty");
        }
        
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
        
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("file_path parameter is required and cannot be empty");
        }
        if (content == null) {
            throw new IllegalArgumentException("content parameter is required and cannot be null");
        }
        
        // Check for context preservation parameters
        boolean append = (Boolean) task.getParameters().getOrDefault("append", false);
        boolean preserveContext = (Boolean) task.getParameters().getOrDefault("preserve_context", true);
        String operationModeStr = (String) task.getParameters().get("operation_mode");
        
        // Load file context for intelligent writing
        boolean fileExists = fileOperations.fileExists(filePath);
        String originalContent = null;
        String finalContent = content;
        
        if (fileExists && preserveContext && !append) {
            try {
                originalContent = fileOperations.readTextFile(filePath);
                
                // Create a temporary SubTask to use FileContextService
                var subTask = sg.edu.nus.iss.misoto.cli.agent.planning.SubTask.builder()
                    .filePath(filePath)
                    .fileContent(content)
                    .originalFileContent(originalContent)
                    .fileExists(true)
                    .preserveContext(true)
                    .operationMode(parseOperationMode(operationModeStr, task.getDescription()))
                    .description(task.getDescription())
                    .build();
                
                // Use FileContextService to merge content intelligently
                finalContent = fileContextService.mergeContent(subTask);
                
                // Create backup before modification
                fileContextService.createBackup(filePath);
                
                System.out.println("🤖 Agent writing file with context preservation: " + filePath);
                System.out.println("📄 Original content: " + originalContent.length() + " bytes");
                System.out.println("📝 New content: " + content.length() + " bytes");
                System.out.println("🔀 Merged content: " + finalContent.length() + " bytes");
                
            } catch (Exception e) {
                log.warn("Failed to load file context for {}, falling back to simple write: {}", filePath, e.getMessage());
                System.out.println("⚠️  Context preservation failed, using simple write for: " + filePath);
            }
        } else {
            System.out.println("🤖 Agent writing file: " + filePath);
        }
        
        // Write the file using FileOperations service
        try {
            if (append) {
                fileOperations.appendTextFile(filePath, content);
            } else {
                fileOperations.writeTextFile(filePath, finalContent);
            }
        } catch (Exception e) {
            // Fallback to direct file operations if FileOperations fails
            log.warn("FileOperations failed, using direct write: {}", e.getMessage());
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());
            
            if (append) {
                Files.writeString(path, content, java.nio.file.StandardOpenOption.CREATE, 
                                 java.nio.file.StandardOpenOption.APPEND);
            } else {
                Files.writeString(path, finalContent);
            }
        }
        
        return AgentTask.TaskResult.builder()
            .output("File written successfully" + (originalContent != null ? " with context preservation" : ""))
            .filesCreated(fileExists ? List.of() : List.of(filePath))
            .filesModified(fileExists ? List.of(filePath) : List.of())
            .artifacts(Map.of(
                "bytes_written", finalContent.length(),
                "original_bytes", originalContent != null ? originalContent.length() : 0,
                "context_preserved", originalContent != null,
                "file_existed", fileExists
            ))
            .build();
    }
    
    /**
     * Parse operation mode from string or infer from task description
     */
    private sg.edu.nus.iss.misoto.cli.agent.planning.SubTask.FileOperationMode parseOperationMode(String operationModeStr, String description) {
        if (operationModeStr != null) {
            try {
                return sg.edu.nus.iss.misoto.cli.agent.planning.SubTask.FileOperationMode.valueOf(operationModeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.debug("Invalid operation mode: {}", operationModeStr);
            }
        }
        
        // Infer from description
        if (description != null) {
            String desc = description.toLowerCase();
            if (desc.contains("replace") || desc.contains("overwrite")) {
                return sg.edu.nus.iss.misoto.cli.agent.planning.SubTask.FileOperationMode.REPLACE;
            }
            if (desc.contains("append") || desc.contains("add to end")) {
                return sg.edu.nus.iss.misoto.cli.agent.planning.SubTask.FileOperationMode.APPEND;
            }
            if (desc.contains("modify") || desc.contains("update") || desc.contains("edit")) {
                return sg.edu.nus.iss.misoto.cli.agent.planning.SubTask.FileOperationMode.MODIFY;
            }
        }
        
        return sg.edu.nus.iss.misoto.cli.agent.planning.SubTask.FileOperationMode.AUTO;
    }
    
    private AgentTask.TaskResult executeFileCopy(AgentTask task) throws IOException {
        String sourcePath = (String) task.getParameters().get("source_path");
        String targetPath = (String) task.getParameters().get("target_path");
        
        // Print the file operation
        System.out.println("🤖 Agent copying file: " + sourcePath + " → " + targetPath);
        
        if (sourcePath == null || sourcePath.trim().isEmpty()) {
            throw new IllegalArgumentException("source_path parameter is required and cannot be empty");
        }
        if (targetPath == null || targetPath.trim().isEmpty()) {
            throw new IllegalArgumentException("target_path parameter is required and cannot be empty");
        }
        
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
        
        // Print the file operation
        System.out.println("🤖 Agent deleting file: " + filePath);
        
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("file_path parameter is required and cannot be empty");
        }
        
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
        
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            throw new IllegalArgumentException("directory_path parameter is required and cannot be empty");
        }
        
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
        
        if (command == null || command.trim().isEmpty()) {
            throw new IllegalArgumentException("command parameter is required and cannot be empty");
        }
        
        // Print the command that will be executed
        String workingDirDisplay = workingDir != null ? " (in: " + workingDir + ")" : "";
        System.out.println("🤖 Agent executing command" + workingDirDisplay + ": " + command);
        log.info("Agent executing shell command: {} in directory: {}", command, workingDir);
        
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
        
        if (scriptContent == null || scriptContent.trim().isEmpty()) {
            throw new IllegalArgumentException("script_content parameter is required and cannot be empty");
        }
        
        // Print the script that will be executed
        System.out.println("🤖 Agent executing " + scriptType + " script:");
        System.out.println("┌─────────────────────────────────────");
        System.out.println(scriptContent.lines().map(line -> "│ " + line).collect(java.util.stream.Collectors.joining("\n")));
        System.out.println("└─────────────────────────────────────");
        log.info("Agent executing {} script with content: {}", scriptType, scriptContent);
        
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
        
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("content parameter is required and cannot be empty");
        }
        
        String prompt = createAnalysisPrompt(analysisType, content);
        String response = aiClient.sendMessage(prompt);
        
        return AgentTask.TaskResult.builder()
            .output(response)
            .artifacts(Map.of("analysis_type", analysisType, "input_length", content.length()))
            .build();
    }
    
    /**
     * Execute code with automatic retry and error fixing
     */
    private String executeCodeWithRetryAndFix(String filename, String originalCode, String taskDescription, List<String> commandsExecuted) {
        final int MAX_RETRY_ATTEMPTS = 3;
        String currentCode = originalCode;
        String runOutput = "";
        
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                log.info("Attempting to execute code (attempt {}/{}): {}", attempt, MAX_RETRY_ATTEMPTS, filename);
                
                // Write current code to file
                Path filePath = Paths.get(filename);
                Files.writeString(filePath, currentCode);
                
                // Try different Python commands (python3, python, py)
                String[] pythonCommands = {"python3", "python", "py"};
                ExecutionEnvironment.ExecutionResult result = null;
                String usedCommand = null;
                
                for (String cmd : pythonCommands) {
                    try {
                        // First test if the command exists
                        ExecutionEnvironment.ExecutionOptions testOptions = new ExecutionEnvironment.ExecutionOptions();
                        testOptions.setTimeout(5000L);
                        ExecutionEnvironment.ExecutionResult testResult = executionEnvironment.executeCommand(cmd + " --version", testOptions);
                        
                        if (testResult.getExitCode() == 0) {
                            // Python command works, now run the script
                            ExecutionEnvironment.ExecutionOptions runOptions = new ExecutionEnvironment.ExecutionOptions();
                            runOptions.setTimeout(30000L);
                            runOptions.setCwd(System.getProperty("user.dir"));
                            result = executionEnvironment.executeCommand(cmd + " " + filename, runOptions);
                            usedCommand = cmd;
                            break;
                        }
                    } catch (Exception e) {
                        log.debug("Python command '{}' failed: {}", cmd, e.getMessage());
                    }
                }
                
                if (result == null || usedCommand == null) {
                    return "\n\nPython script execution skipped: No Python interpreter found in PATH.\n" +
                           "Please install Python or ensure it's in your system PATH.\n" +
                           "You can run the script manually with: python " + filename;
                }
                
                // Check if execution was successful
                if (result.getExitCode() == 0) {
                    runOutput = String.format("\n\nScript execution successful (attempt %d, using %s):\n%s", 
                                             attempt, usedCommand, result.getOutput());
                    commandsExecuted.add(usedCommand + " " + filename + " (successful)");
                    
                    if (attempt > 1) {
                        runOutput += "\n\n✅ Code was automatically fixed and now runs successfully!";
                    }
                    
                    return runOutput;
                } else {
                    // Execution failed, try to fix the code
                    String errorOutput = result.getOutput();
                    String exitCode = String.valueOf(result.getExitCode());
                    
                    log.warn("Code execution failed (attempt {}): Exit code {}, Error: {}", attempt, exitCode, errorOutput);
                    
                    if (attempt < MAX_RETRY_ATTEMPTS) {
                        // Try to fix the code using AI
                        String fixedCode = fixCodeWithAI(currentCode, errorOutput, taskDescription, attempt);
                        if (fixedCode != null && !fixedCode.equals(currentCode)) {
                            currentCode = fixedCode;
                            runOutput += String.format("\n\n❌ Attempt %d failed (exit code %s): %s\n🔧 Attempting to fix code automatically...", 
                                                      attempt, exitCode, errorOutput);
                            continue;
                        }
                    }
                    
                    runOutput = String.format("\n\nScript execution failed after %d attempts (using %s):\n%s\nExit code: %s\n\n💡 You can run the script manually with: python %s", 
                                             attempt, usedCommand, errorOutput, exitCode, filename);
                    commandsExecuted.add(usedCommand + " " + filename + " (failed after " + attempt + " attempts)");
                    return runOutput;
                }
                
            } catch (Exception e) {
                log.error("Exception during code execution attempt {}: {}", attempt, e.getMessage());
                if (attempt == MAX_RETRY_ATTEMPTS) {
                    runOutput = "\n\nScript execution failed after " + attempt + " attempts: " + e.getMessage() + 
                              "\nYou can run the script manually with: python " + filename;
                    return runOutput;
                }
            }
        }
        
        return runOutput;
    }
    
    /**
     * Use AI to fix code based on execution errors
     */
    private String fixCodeWithAI(String originalCode, String errorOutput, String taskDescription, int attempt) {
        try {
            String fixPrompt = String.format("""
                The following code failed to execute with an error. Please fix the code to make it work correctly.
                
                ORIGINAL TASK: %s
                
                CURRENT CODE:
                ```python
                %s
                ```
                
                ERROR OUTPUT:
                %s
                
                FIXING ATTEMPT: %d
                
                Please analyze the error and provide the corrected code. Common issues to check:
                1. Syntax errors (missing quotes, colons, parentheses)
                2. Indentation errors
                3. Missing imports
                4. Variable name errors
                5. Logic errors
                6. Type errors
                
                Respond with ONLY the corrected Python code, no explanations or markdown blocks.
                Make sure the code is complete and executable.
                """, taskDescription, originalCode, errorOutput, attempt);
            
            String fixedCodeResponse = aiClient.sendMessage(fixPrompt);
            
            // Clean up the response - remove any markdown code blocks
            String fixedCode = fixedCodeResponse.trim();
            if (fixedCode.startsWith("```python")) {
                fixedCode = fixedCode.substring(9);
            }
            if (fixedCode.startsWith("```")) {
                fixedCode = fixedCode.substring(3);
            }
            if (fixedCode.endsWith("```")) {
                fixedCode = fixedCode.substring(0, fixedCode.length() - 3);
            }
            
            fixedCode = fixedCode.trim();
            
            log.info("AI suggested code fix for attempt {}: {} characters", attempt, fixedCode.length());
            return fixedCode;
            
        } catch (Exception e) {
            log.error("Failed to generate code fix with AI: {}", e.getMessage());
            return null;
        }
    }

    private AgentTask.TaskResult executeCodeGeneration(AgentTask task) throws Exception {
        String taskDescription = (String) task.getParameters().get("task_description");
        String conversationContext = (String) task.getParameters().get("conversation_context");
        
        if (taskDescription == null || taskDescription.trim().isEmpty()) {
            throw new IllegalArgumentException("task_description parameter is required and cannot be empty");
        }
        
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
                "END_CODE\n\n" +
                "Alternative: You can also use markdown code blocks like:\n" +
                "LANGUAGE: <language>\n" +
                "FILENAME: <filename>\n" +
                "DIRECTORIES: <comma-separated list or 'none'>\n" +
                "```<language>\n<actual code>\n```";
        
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
            
            // Try to run the generated code if it's a script with automatic error fixing
            String runOutput = "";
            if (language.toLowerCase().contains("python") && filename.endsWith(".py")) {
                runOutput = executeCodeWithRetryAndFix(filename, code, taskDescription, commandsExecuted);
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
            } else if ((language.toLowerCase().contains("terraform") || language.toLowerCase().contains("tf")) && 
                       filename.endsWith(".tf")) {
                runOutput = "\n\nTerraform file created successfully. Use 'terraform validate' to check syntax.";
                commandsExecuted.add("Generated Terraform configuration file");
            } else if ((language.toLowerCase().contains("jinja") || language.toLowerCase().contains("j2")) && 
                       filename.endsWith(".j2")) {
                runOutput = "\n\nJinja2 template file created successfully. Use with your template engine.";
                commandsExecuted.add("Generated Jinja2 template file");
            } else if (language.toLowerCase().contains("tftpl") && filename.endsWith(".tftpl")) {
                runOutput = "\n\nTerraform template file created successfully. Use with templatefile() function.";
                commandsExecuted.add("Generated Terraform template file");
            } else if (language.toLowerCase().contains("bash") && filename.endsWith(".sh")) {
                runOutput = "\n\nBash script created successfully. Make executable with: chmod +x " + filename;
                commandsExecuted.add("Generated Bash script file");
            } else if (language.toLowerCase().contains("json") && filename.endsWith(".json")) {
                runOutput = "\n\nJSON file created successfully. Use 'cat " + filename + "' to view contents.";
                commandsExecuted.add("Generated JSON configuration file");
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
        
        if (context == null || context.trim().isEmpty()) {
            throw new IllegalArgumentException("context parameter is required and cannot be empty");
        }
        if (question == null || question.trim().isEmpty()) {
            throw new IllegalArgumentException("question parameter is required and cannot be empty");
        }
        
        String decision = decisionEngine.makeDecision(context, question, options);
        
        return AgentTask.TaskResult.builder()
            .output(decision)
            .artifacts(Map.of("context_length", context.length(), "options_count", options.size()))
            .build();
    }
    
    private AgentTask.TaskResult executeTextProcessing(AgentTask task) throws Exception {
        String text = (String) task.getParameters().get("text");
        String operation = (String) task.getParameters().get("operation");
        
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("text parameter is required and cannot be empty");
        }
        if (operation == null || operation.trim().isEmpty()) {
            throw new IllegalArgumentException("operation parameter is required and cannot be empty");
        }
        
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
        
        if (toolName == null || toolName.trim().isEmpty()) {
            throw new IllegalArgumentException("tool_name parameter is required and cannot be empty");
        }
        
        // Print the MCP tool call that will be executed
        System.out.println("🤖 Agent calling MCP tool: " + toolName + 
                          (arguments.isEmpty() ? "" : " with arguments: " + arguments));
        log.info("Agent executing MCP tool call: {} with arguments: {}", toolName, arguments);
        
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
        
        if (logFile == null || logFile.trim().isEmpty()) {
            throw new IllegalArgumentException("log_file parameter is required and cannot be empty");
        }
        
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
        boolean inMarkdownCodeBlock = false;
        String detectedLanguage = null;
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // Handle structured format (CODE: ... END_CODE)
            if (trimmedLine.startsWith("LANGUAGE:")) {
                result.put("LANGUAGE", trimmedLine.substring("LANGUAGE:".length()).trim().toLowerCase());
            } else if (trimmedLine.startsWith("FILENAME:")) {
                result.put("FILENAME", trimmedLine.substring("FILENAME:".length()).trim());
            } else if (trimmedLine.startsWith("DIRECTORIES:")) {
                result.put("DIRECTORIES", trimmedLine.substring("DIRECTORIES:".length()).trim());
            } else if (trimmedLine.equals("CODE:")) {
                inCodeSection = true;
            } else if (trimmedLine.equals("END_CODE")) {
                inCodeSection = false;
            } 
            // Handle markdown code blocks (```language ... ```)
            else if (trimmedLine.startsWith("```")) {
                if (!inMarkdownCodeBlock) {
                    // Starting a code block
                    inMarkdownCodeBlock = true;
                    // Extract language from the opening tag
                    String languageTag = trimmedLine.substring(3).trim().toLowerCase();
                    if (!languageTag.isEmpty()) {
                        detectedLanguage = languageTag;
                        result.put("LANGUAGE", languageTag);
                    }
                } else {
                    // Ending a code block
                    inMarkdownCodeBlock = false;
                }
            } else if (inCodeSection || inMarkdownCodeBlock) {
                // Preserve original line formatting for code content
                codeBuilder.append(line).append("\n");
            }
        }
        
        // If no explicit language was set but we detected one from markdown, use it
        if (!result.containsKey("LANGUAGE") && detectedLanguage != null) {
            result.put("LANGUAGE", detectedLanguage);
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
            case "terraform", "tf" -> "tf";
            case "jinja", "jinja2", "j2" -> "j2";
            case "tftpl", "terraform-template" -> "tftpl";
            case "hcl" -> "hcl";
            case "json" -> "json";
            case "xml" -> "xml";
            case "html" -> "html";
            case "css" -> "css";
            case "scss", "sass" -> "scss";
            case "sql" -> "sql";
            case "dockerfile", "docker" -> "dockerfile";
            default -> "txt";
        };
    }
}
