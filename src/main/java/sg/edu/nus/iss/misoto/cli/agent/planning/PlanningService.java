package sg.edu.nus.iss.misoto.cli.agent.planning;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.misoto.cli.ai.AiClient;
import sg.edu.nus.iss.misoto.cli.agent.task.AgentTask;
import sg.edu.nus.iss.misoto.cli.agent.task.TaskQueueService;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ReAct-based Planning Service implementing Reasoning + Acting cycles
 * with task decomposition, plan generation, execution, and monitoring.
 */
@Slf4j
@Service
public class PlanningService {
    
    @Autowired
    private AiClient aiClient;
    
    @Autowired
    private TaskQueueService taskQueue;
    
    // Plan storage
    private final Map<String, ExecutionPlan> activePlans = new ConcurrentHashMap<>();
    private final Map<String, List<String>> planHistory = new ConcurrentHashMap<>();
    
    /**
     * Create a comprehensive plan using ReAct methodology
     */
    public ExecutionPlan createPlan(String goal, Map<String, Object> context) throws Exception {
        String planId = "plan-" + System.currentTimeMillis();
        
        log.info("Creating ReAct plan for goal: {}", goal);
        
        // Phase 1: Task Decomposition using ReAct reasoning
        List<SubTask> subTasks = decomposeTask(goal, context);
        
        // Phase 2: Plan Generation with reasoning chains
        PlanningStrategy strategy = generatePlanningStrategy(goal, subTasks, context);
        
        // Phase 3: Create execution plan with monitoring points
        ExecutionPlan plan = ExecutionPlan.builder()
            .id(planId)
            .goal(goal)
            .subTasks(subTasks)
            .strategy(strategy)
            .context(new HashMap<>(context))
            .status(ExecutionPlan.PlanStatus.CREATED)
            .createdAt(Instant.now())
            .build();
        
        activePlans.put(planId, plan);
        log.info("Created execution plan {} with {} subtasks", planId, subTasks.size());
        
        return plan;
    }
    
    /**
     * Phase 1: Task Decomposition using AI reasoning
     */
    private List<SubTask> decomposeTask(String goal, Map<String, Object> context) throws Exception {
        String decompositionPrompt = buildDecompositionPrompt(goal, context);
        log.info("🤖 Sending task decomposition prompt to AI...");
        log.debug("Decomposition prompt: {}", decompositionPrompt);
        
        String response = aiClient.sendMessage(decompositionPrompt);
        log.info("✅ Received AI response for task decomposition");
        log.info("AI Response: {}", response);
        
        return parseSubTasksFromResponse(response);
    }
    
    /**
     * Phase 2: Generate planning strategy with tool selection
     */
    private PlanningStrategy generatePlanningStrategy(String goal, List<SubTask> subTasks, Map<String, Object> context) throws Exception {
        String strategyPrompt = buildStrategyPrompt(goal, subTasks, context);
        log.info("🧠 Sending strategy generation prompt to AI...");
        log.debug("Strategy prompt: {}", strategyPrompt);
        
        String response = aiClient.sendMessage(strategyPrompt);
        log.info("✅ Received AI response for planning strategy");
        log.info("AI Strategy Response: {}", response);
        
        return parseStrategyFromResponse(response, subTasks);
    }
    
    /**
     * Execute a plan using ReAct cycles
     */
    public PlanExecution executePlan(String planId) throws Exception {
        ExecutionPlan plan = activePlans.get(planId);
        if (plan == null) {
            throw new IllegalArgumentException("Plan not found: " + planId);
        }
        
        log.info("Starting execution of plan: {}", planId);
        plan.setStatus(ExecutionPlan.PlanStatus.EXECUTING);
        
        PlanExecution execution = PlanExecution.builder()
            .planId(planId)
            .status(PlanExecution.ExecutionStatus.RUNNING)
            .startedAt(Instant.now())
            .steps(new ArrayList<>())
            .workingMemory(new HashMap<>())
            .build();
        
        // Execute each subtask using ReAct cycles
        for (SubTask subTask : plan.getSubTasks()) {
            ReActCycleResult result = executeReActCycle(subTask, execution, plan);
            
            ExecutionStep step = ExecutionStep.builder()
                .subTaskId(subTask.getId())
                .reasoning(result.getReasoning())
                .action(result.getAction())
                .observation(result.getObservation())
                .status(result.isSuccess() ? ExecutionStep.StepStatus.COMPLETED : ExecutionStep.StepStatus.FAILED)
                .startedAt(Instant.now())
                .completedAt(Instant.now())
                .build();
            
            execution.getSteps().add(step);
            
            // Update working memory with results
            execution.getWorkingMemory().putAll(result.getMemoryUpdates());
            
            // Check if we need to replan
            if (!result.isSuccess() && result.isShouldReplan()) {
                log.info("Replanning required for subtask: {}", subTask.getId());
                replanFromStep(plan, execution, subTask);
            }
        }
        
        execution.setStatus(PlanExecution.ExecutionStatus.COMPLETED);
        execution.setCompletedAt(Instant.now());
        
        plan.setStatus(ExecutionPlan.PlanStatus.COMPLETED);
        
        return execution;
    }
    
    /**
     * Execute a single ReAct cycle: Reasoning → Acting → Observation
     */
    private ReActCycleResult executeReActCycle(SubTask subTask, PlanExecution execution, ExecutionPlan plan) throws Exception {
        // Print the current subtask being worked on
        System.out.println("🎯 Working on: " + subTask.getDescription() + " [" + subTask.getPriority() + "]");
        
        // Reasoning Phase
        String reasoning = performReasoning(subTask, execution, plan);
        
        // Acting Phase
        ActionResult actionResult = performAction(subTask, reasoning, execution);
        
        // Observation Phase
        String observation = performObservation(actionResult, execution);
        
        // Self-reflection Phase
        boolean success = evaluateSuccess(subTask, actionResult, observation);
        boolean shouldReplan = shouldReplan(subTask, actionResult, observation);
        
        return ReActCycleResult.builder()
            .reasoning(reasoning)
            .action(actionResult.getActionDescription())
            .observation(observation)
            .success(success)
            .shouldReplan(shouldReplan)
            .memoryUpdates(actionResult.getMemoryUpdates())
            .build();
    }
    
    /**
     * Reasoning Phase: Analyze current state and decide next action
     */
    private String performReasoning(SubTask subTask, PlanExecution execution, ExecutionPlan plan) throws Exception {
        log.info("🤔 ReAct Phase: REASONING for subtask {}", subTask.getId());
        String reasoningPrompt = String.format("""
            You are an AI agent using ReAct (Reasoning + Acting) methodology.
            
            CURRENT GOAL: %s
            CURRENT SUBTASK: %s
            SUBTASK DESCRIPTION: %s
            
            WORKING MEMORY:
            %s
            
            PREVIOUS STEPS:
            %s
            
            AVAILABLE TOOLS:
            - FILE_READ, FILE_WRITE, FILE_COPY, FILE_DELETE: File operations
            - SHELL_COMMAND: Execute system commands
            - CODE_GENERATION: Generate and execute code
            - AI_ANALYSIS: Analyze data and make decisions
            - MCP_TOOL_CALL: Use Model Context Protocol tools
            
            REASONING TASK:
            Analyze the current situation and decide what action to take next to complete this subtask.
            Consider:
            1. What information do you currently have?
            2. What information do you still need?
            3. What tools would be most appropriate?
            4. What are the potential risks or dependencies?
            5. How does this fit into the overall plan?
            
            Provide your reasoning in a clear, step-by-step format.
            """, 
            plan.getGoal(),
            subTask.getId(),
            subTask.getDescription(),
            formatWorkingMemory(execution.getWorkingMemory()),
            formatPreviousSteps(execution.getSteps())
        );
        
        log.debug("Reasoning prompt: {}", reasoningPrompt);
        String reasoning = aiClient.sendMessage(reasoningPrompt);
        log.info("🧠 AI Reasoning: {}", reasoning);
        
        return reasoning;
    }
    
    /**
     * Acting Phase: Execute the decided action
     */
    private ActionResult performAction(SubTask subTask, String reasoning, PlanExecution execution) throws Exception {
        log.info("⚡ ReAct Phase: ACTING for subtask {}", subTask.getId());
        
        // Determine action type based on reasoning
        String actionPrompt = String.format("""
            Based on your reasoning:
            %s
            
            Now decide on a specific action to take. Respond EXACTLY in this format:
            ACTION_TYPE: [FILE_READ|FILE_WRITE|FILE_COPY|FILE_DELETE|SHELL_COMMAND|CODE_GENERATION|AI_ANALYSIS|MCP_TOOL_CALL]
            ACTION_DESCRIPTION: A clear description of what you're doing
            PARAMETERS: param1=value1, param2=value2
            EXPECTED_OUTCOME: What you expect to happen
            
            CRITICAL: Use these exact parameter names (case-sensitive):
              - For SHELL_COMMAND: command=mkdir -p todoApp
              - For FILE_READ: file_path=/path/to/file.txt
              - For FILE_WRITE: file_path=/path/to/file.txt, content=file content here
              - For FILE_COPY: source_path=/source/file, target_path=/target/file
              - For FILE_DELETE: file_path=/path/to/file.txt
              - For CODE_GENERATION: task_description=what to generate, language=python
              - For AI_ANALYSIS: task_description=what to analyze, context=analysis context
              - For MCP_TOOL_CALL: tool_name=tool_name, tool_arguments=arguments
            
            Example for creating a directory:
            ACTION_TYPE: SHELL_COMMAND
            ACTION_DESCRIPTION: Creating project directory structure for the Todo app
            PARAMETERS: command=mkdir -p todoApp/frontend todoApp/backend, working_directory=.
            EXPECTED_OUTCOME: Directory structure will be created
            """, reasoning);
        
        log.debug("Action decision prompt: {}", actionPrompt);
        String actionResponse = aiClient.sendMessage(actionPrompt);
        log.info("🎯 AI Action Decision: {}", actionResponse);
        ActionSpec actionSpec = parseActionSpec(actionResponse);
        
        // Execute the action by creating an agent task
        AgentTask task = createTaskFromAction(subTask, actionSpec);
        
        // Print the step being executed
        System.out.println("🚀 Executing step: " + actionSpec.getDescription() + " [" + actionSpec.getType() + "]");
        
        String taskId = taskQueue.submitTask(task);
        
        // Wait for task completion and collect results
        AgentTask completedTask = waitForTaskCompletion(taskId);
        
        Map<String, Object> memoryUpdates = new HashMap<>();
        memoryUpdates.put("last_action", actionSpec.getDescription());
        memoryUpdates.put("last_task_id", taskId);
        
        if (completedTask.getResult() != null) {
            memoryUpdates.put("last_result", completedTask.getResult().getOutput());
            memoryUpdates.put("files_created", completedTask.getResult().getFilesCreated());
        }
        
        // Print completion status
        if (completedTask.getStatus() == AgentTask.TaskStatus.COMPLETED) {
            System.out.println("✅ Step completed successfully");
            if (completedTask.getResult() != null && completedTask.getResult().getOutput() != null) {
                String output = completedTask.getResult().getOutput().trim();
                if (!output.isEmpty()) {
                    System.out.println("📤 Output: " + output);
                }
            }
        } else {
            System.out.println("❌ Step failed: " + completedTask.getErrorMessage());
        }
        
        return ActionResult.builder()
            .actionDescription(actionSpec.getDescription())
            .taskId(taskId)
            .success(completedTask.getStatus() == AgentTask.TaskStatus.COMPLETED)
            .result(completedTask.getResult())
            .memoryUpdates(memoryUpdates)
            .build();
    }
    
    /**
     * Observation Phase: Analyze the results of the action
     */
    private String performObservation(ActionResult actionResult, PlanExecution execution) throws Exception {
        log.info("👁️ ReAct Phase: OBSERVATION for action: {}", actionResult.getActionDescription());
        
        String observationPrompt = String.format("""
            You just executed an action. Now observe and analyze the results.
            
            ACTION TAKEN: %s
            ACTION SUCCESS: %s
            
            RESULTS:
            %s
            
            TASK: Analyze these results and provide observations about:
            1. Did the action achieve its intended goal?
            2. What new information was discovered?
            3. Are there any unexpected results or errors?
            4. What does this mean for the next steps?
            5. Should we continue with the current plan or adapt?
            
            Provide clear, factual observations.
            """,
            actionResult.getActionDescription(),
            actionResult.isSuccess(),
            formatActionResult(actionResult)
        );
        
        log.debug("Observation prompt: {}", observationPrompt);
        String observation = aiClient.sendMessage(observationPrompt);
        log.info("🔍 AI Observation: {}", observation);
        
        return observation;
    }
    
    /**
     * Self-reflection: Evaluate if the subtask was completed successfully
     */
    private boolean evaluateSuccess(SubTask subTask, ActionResult actionResult, String observation) throws Exception {
        log.info("🔬 ReAct Phase: SELF-REFLECTION for subtask {}", subTask.getId());
        
        String evaluationPrompt = String.format("""
            Evaluate if this subtask has been completed successfully:
            
            SUBTASK: %s
            EXPECTED OUTCOME: %s
            
            ACTION RESULT: %s
            OBSERVATION: %s
            
            Has this subtask been completed successfully? Respond with YES or NO and brief reasoning.
            """,
            subTask.getDescription(),
            subTask.getExpectedOutcome(),
            actionResult.getActionDescription(),
            observation
        );
        
        log.debug("Success evaluation prompt: {}", evaluationPrompt);
        String response = aiClient.sendMessage(evaluationPrompt);
        log.info("✅ AI Success Evaluation: {}", response);
        
        boolean success = response.toLowerCase().contains("yes");
        log.info("📊 Subtask {} evaluated as: {}", subTask.getId(), success ? "SUCCESS" : "NEEDS MORE WORK");
        
        return success;
    }
    
    /**
     * Determine if replanning is needed
     */
    private boolean shouldReplan(SubTask subTask, ActionResult actionResult, String observation) {
        // Simple heuristic - replan if action failed and it's a critical subtask
        return !actionResult.isSuccess() && subTask.getPriority() == SubTask.Priority.CRITICAL;
    }
    
    /**
     * Replan from a specific step when things go wrong
     */
    private void replanFromStep(ExecutionPlan plan, PlanExecution execution, SubTask failedSubTask) throws Exception {
        log.info("Replanning from failed subtask: {}", failedSubTask.getId());
        
        String replanPrompt = String.format("""
            A subtask has failed and requires replanning:
            
            FAILED SUBTASK: %s
            OVERALL GOAL: %s
            CURRENT PROGRESS: %s
            
            Please suggest:
            1. Alternative approaches for the failed subtask
            2. Modified subtasks that might work better
            3. Any dependencies that need to be addressed first
            
            Provide a revised plan for completing the remaining work.
            """,
            failedSubTask.getDescription(),
            plan.getGoal(),
            execution.getSteps().size() + " steps completed"
        );
        
        String replanResponse = aiClient.sendMessage(replanPrompt);
        
        // Parse and update the plan (simplified implementation)
        log.info("Replan suggestion: {}", replanResponse);
        
        // In a full implementation, this would modify the plan's remaining subtasks
    }
    
    // Helper methods for formatting and parsing
    
    private String buildDecompositionPrompt(String goal, Map<String, Object> context) {
        return String.format("""
            You are a task decomposition expert. Break down this high-level goal into smaller, manageable subtasks.
            
            GOAL: %s
            CONTEXT: %s
            
            Decompose this into 3-7 subtasks. For each subtask, provide:
            1. A clear, specific description
            2. Expected outcome
            3. Priority level (CRITICAL, HIGH, MEDIUM, LOW)
            4. Estimated complexity (SIMPLE, MODERATE, COMPLEX)
            5. Dependencies on other subtasks
            
            Format your response as:
            SUBTASK_1:
            Description: [specific description]
            Expected Outcome: [what should result]
            Priority: [CRITICAL|HIGH|MEDIUM|LOW]
            Complexity: [SIMPLE|MODERATE|COMPLEX]
            Dependencies: [list of other subtasks or NONE]
            
            [Continue for all subtasks...]
            """, goal, context.toString());
    }
    
    private String buildStrategyPrompt(String goal, List<SubTask> subTasks, Map<String, Object> context) {
        return String.format("""
            Create an execution strategy for these subtasks:
            
            GOAL: %s
            SUBTASKS: %s
            
            Determine:
            1. Optimal execution order
            2. Parallel execution opportunities
            3. Risk mitigation strategies
            4. Tool selection for each subtask
            5. Monitoring and checkpoint strategies
            
            Provide a comprehensive execution strategy.
            """, goal, formatSubTasks(subTasks));
    }
    
    private List<SubTask> parseSubTasksFromResponse(String response) {
        List<SubTask> subTasks = new ArrayList<>();
        
        // Simple parsing implementation - in production would use more robust parsing
        String[] blocks = response.split("SUBTASK_");
        
        for (int i = 1; i < blocks.length; i++) {
            String block = blocks[i];
            SubTask subTask = parseSubTaskBlock(block, i);
            subTasks.add(subTask);
        }
        
        return subTasks;
    }
    
    private SubTask parseSubTaskBlock(String block, int index) {
        Map<String, String> fields = parseFields(block);
        
        return SubTask.builder()
            .id("subtask-" + index)
            .description(fields.getOrDefault("Description", "Unnamed subtask"))
            .expectedOutcome(fields.getOrDefault("Expected Outcome", ""))
            .priority(SubTask.Priority.valueOf(fields.getOrDefault("Priority", "MEDIUM")))
            .complexity(SubTask.Complexity.valueOf(fields.getOrDefault("Complexity", "MODERATE")))
            .dependencies(parseList(fields.getOrDefault("Dependencies", "NONE")))
            .status(SubTask.Status.PENDING)
            .build();
    }
    
    private Map<String, String> parseFields(String block) {
        Map<String, String> fields = new HashMap<>();
        String[] lines = block.split("\n");
        
        for (String line : lines) {
            if (line.contains(":")) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    fields.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        
        return fields;
    }
    
    private List<String> parseList(String value) {
        if ("NONE".equals(value)) {
            return new ArrayList<>();
        }
        return Arrays.asList(value.split(","));
    }
    
    private PlanningStrategy parseStrategyFromResponse(String response, List<SubTask> subTasks) {
        return PlanningStrategy.builder()
            .description(response)
            .executionOrder(subTasks) // Simplified - would parse actual order
            .parallelGroups(new ArrayList<>())
            .riskMitigation(new HashMap<>())
            .build();
    }
    
    private ActionSpec parseActionSpec(String response) {
        Map<String, String> fields = parseFields(response);
        Map<String, Object> parameters = parseParameters(fields.getOrDefault("PARAMETERS", ""));
        AgentTask.TaskType taskType = AgentTask.TaskType.valueOf(fields.getOrDefault("ACTION_TYPE", "AI_ANALYSIS"));
        
        // Print command if it's a shell command and we have the command parameter
        if (taskType == AgentTask.TaskType.SHELL_COMMAND && parameters.containsKey("command")) {
            String command = (String) parameters.get("command");
            if (command != null && !command.trim().isEmpty()) {
                System.out.println("🤖 Agent plans to execute: " + command);
            }
        }
        
        return ActionSpec.builder()
            .type(taskType)
            .description(fields.getOrDefault("ACTION_DESCRIPTION", "Unknown action"))
            .parameters(parameters)
            .expectedOutcome(fields.getOrDefault("EXPECTED_OUTCOME", ""))
            .build();
    }
    
    private Map<String, Object> parseParameters(String params) {
        Map<String, Object> paramMap = new HashMap<>();
        if (!params.isEmpty()) {
            log.debug("Parsing parameters: {}", params);
            String[] pairs = params.split(",");
            for (String pair : pairs) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    String key = kv[0].trim();
                    String value = kv[1].trim();
                    // Remove quotes if present
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    paramMap.put(key, value);
                    log.debug("  Parameter: {} = {}", key, value);
                } else {
                    log.warn("Skipping malformed parameter pair: {}", pair);
                }
            }
        }
        log.debug("Parsed parameters: {}", paramMap);
        return paramMap;
    }
    
    private AgentTask createTaskFromAction(SubTask subTask, ActionSpec actionSpec) {
        // Enhance parameters based on task type if needed
        Map<String, Object> enhancedParams = enhanceParameters(actionSpec.getType(), actionSpec.getParameters(), actionSpec.getDescription());
        
        return AgentTask.builder()
            .id("action-" + System.currentTimeMillis())
            .name("Plan Action: " + actionSpec.getDescription())
            .type(actionSpec.getType())
            .description(actionSpec.getDescription())
            .parameters(enhancedParams)
            .priority(mapPriority(subTask.getPriority()))
            .createdAt(Instant.now())
            .build();
    }
    
    private Map<String, Object> enhanceParameters(AgentTask.TaskType taskType, Map<String, Object> originalParams, String description) {
        Map<String, Object> enhanced = new HashMap<>(originalParams);
        
        // Add fallback parameters based on task type if they're missing
        switch (taskType) {
            case SHELL_COMMAND -> {
                if (!enhanced.containsKey("command") || enhanced.get("command") == null || enhanced.get("command").toString().trim().isEmpty()) {
                    // Try to extract command from description or provide a safe default
                    String extractedCommand = extractCommandFromDescription(description);
                    if (extractedCommand != null) {
                        enhanced.put("command", extractedCommand);
                        System.out.println("🤖 Agent plans to execute: " + extractedCommand);
                        log.info("Added fallback command parameter: {}", extractedCommand);
                    } else {
                        log.warn("No command parameter found for SHELL_COMMAND task, using safe default");
                        enhanced.put("command", "echo 'No command specified'");
                    }
                }
            }
            case FILE_READ, FILE_DELETE -> {
                if (!enhanced.containsKey("file_path") || enhanced.get("file_path") == null) {
                    log.warn("No file_path parameter found for {} task", taskType);
                    enhanced.put("file_path", "./example.txt");
                }
            }
            case FILE_WRITE -> {
                if (!enhanced.containsKey("file_path") || enhanced.get("file_path") == null) {
                    enhanced.put("file_path", "./generated_file.txt");
                }
                if (!enhanced.containsKey("content") || enhanced.get("content") == null) {
                    enhanced.put("content", "Generated content from: " + description);
                }
            }
            case CODE_GENERATION -> {
                if (!enhanced.containsKey("task_description") || enhanced.get("task_description") == null) {
                    enhanced.put("task_description", description);
                }
                if (!enhanced.containsKey("language") || enhanced.get("language") == null) {
                    enhanced.put("language", "python");
                }
            }
            case AI_ANALYSIS -> {
                if (!enhanced.containsKey("task_description") || enhanced.get("task_description") == null) {
                    enhanced.put("task_description", description);
                }
                if (!enhanced.containsKey("context") || enhanced.get("context") == null) {
                    enhanced.put("context", "General analysis");
                }
            }
        }
        
        return enhanced;
    }
    
    private String extractCommandFromDescription(String description) {
        // Try to extract shell commands from common patterns in descriptions
        if (description.toLowerCase().contains("create directory") || description.toLowerCase().contains("creating") && description.toLowerCase().contains("directory")) {
            if (description.toLowerCase().contains("todo") || description.toLowerCase().contains("todoapp")) {
                return "mkdir -p todoApp";
            }
            return "mkdir -p project_directory";
        }
        if (description.toLowerCase().contains("list") && description.toLowerCase().contains("content")) {
            return "ls -la";
        }
        if (description.toLowerCase().contains("check") && description.toLowerCase().contains("exist")) {
            return "ls -la";
        }
        if (description.toLowerCase().contains("python") && description.toLowerCase().contains("version")) {
            return "python --version";
        }
        if (description.toLowerCase().contains("node") && description.toLowerCase().contains("version")) {
            return "node --version";
        }
        return null;
    }
    
    private AgentTask.TaskPriority mapPriority(SubTask.Priority priority) {
        return switch (priority) {
            case CRITICAL -> AgentTask.TaskPriority.HIGH;
            case HIGH -> AgentTask.TaskPriority.HIGH;
            case MEDIUM -> AgentTask.TaskPriority.MEDIUM;
            case LOW -> AgentTask.TaskPriority.LOW;
        };
    }
    
    private AgentTask waitForTaskCompletion(String taskId) throws InterruptedException {
        // Simple polling implementation
        int maxAttempts = 60;
        for (int i = 0; i < maxAttempts; i++) {
            AgentTask task = taskQueue.getTask(taskId);
            if (task != null && task.isCompleted()) {
                return task;
            }
            Thread.sleep(1000);
        }
        
        // Return the task even if not completed
        return taskQueue.getTask(taskId);
    }
    
    private String formatWorkingMemory(Map<String, Object> memory) {
        if (memory.isEmpty()) {
            return "No information in working memory yet.";
        }
        
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : memory.entrySet()) {
            sb.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }
    
    private String formatPreviousSteps(List<ExecutionStep> steps) {
        if (steps.isEmpty()) {
            return "No previous steps completed yet.";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            ExecutionStep step = steps.get(i);
            sb.append(String.format("Step %d: %s -> %s\n", i + 1, step.getAction(), step.getStatus()));
        }
        return sb.toString();
    }
    
    private String formatSubTasks(List<SubTask> subTasks) {
        StringBuilder sb = new StringBuilder();
        for (SubTask subTask : subTasks) {
            sb.append(String.format("- %s (%s priority)\n", subTask.getDescription(), subTask.getPriority()));
        }
        return sb.toString();
    }
    
    private String formatActionResult(ActionResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Success: ").append(result.isSuccess()).append("\n");
        if (result.getResult() != null) {
            sb.append("Output: ").append(result.getResult().getOutput()).append("\n");
            if (result.getResult().getFilesCreated() != null) {
                sb.append("Files Created: ").append(result.getResult().getFilesCreated()).append("\n");
            }
        }
        return sb.toString();
    }
    
    // Getters for external access
    
    public ExecutionPlan getPlan(String planId) {
        return activePlans.get(planId);
    }
    
    public List<ExecutionPlan> getActivePlans() {
        return new ArrayList<>(activePlans.values());
    }
    
    public void cancelPlan(String planId) {
        ExecutionPlan plan = activePlans.get(planId);
        if (plan != null) {
            plan.setStatus(ExecutionPlan.PlanStatus.CANCELLED);
            log.info("Cancelled plan: {}", planId);
        }
    }
}