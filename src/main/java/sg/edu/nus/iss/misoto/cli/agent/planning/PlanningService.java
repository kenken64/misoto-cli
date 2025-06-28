package sg.edu.nus.iss.misoto.cli.agent.planning;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.misoto.cli.ai.AiClient;
import sg.edu.nus.iss.misoto.cli.agent.task.AgentTask;
import sg.edu.nus.iss.misoto.cli.agent.task.TaskQueueService;
import sg.edu.nus.iss.misoto.cli.agent.context.FileContextService;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.io.IOException;
import java.util.stream.Stream;
import java.util.concurrent.TimeUnit;

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
    
    @Autowired
    private FileContextService fileContextService;
    
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
            // Check for missing tools and suggest installations before execution
            checkAndSuggestMissingTools(subTask, execution);
            
            // Execute AI-generated commands and save files before the ReAct cycle
            executeSubTaskDirectives(subTask);
            
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
            
            // Handle task failure
            if (!result.isSuccess()) {
                boolean shouldContinue = promptUserOnTaskFailure(subTask, result);
                if (!shouldContinue) {
                    System.out.println("🛑 Execution stopped by user choice.");
                    execution.setStatus(PlanExecution.ExecutionStatus.FAILED);
                    plan.setStatus(ExecutionPlan.PlanStatus.FAILED);
                    return execution;
                }
                
                // Check if we need to replan
                if (result.isShouldReplan()) {
                    log.info("Replanning required for subtask: {}", subTask.getId());
                    replanFromStep(plan, execution, subTask);
                }
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
            
            SYSTEM TOOL AVAILABILITY:
            %s
            
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
            formatPreviousSteps(execution.getSteps()),
            checkAvailableSystemTools()
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
            Based on your reasoning analysis above, you need to take ONE SPECIFIC ACTION now.
            
            REASONING CONTEXT:
            %s
            
            CURRENT SUBTASK: %s
            SUBTASK GOAL: %s
            
            Choose the MOST APPROPRIATE action type for this specific subtask and provide exact parameters.
            
            Respond EXACTLY in this format:
            ACTION_TYPE: [FILE_READ|FILE_WRITE|FILE_COPY|FILE_DELETE|SHELL_COMMAND|CODE_GENERATION|AI_ANALYSIS|MCP_TOOL_CALL]
            ACTION_DESCRIPTION: [Specific description of what THIS action accomplishes for THIS subtask]
            PARAMETERS: [exact parameters needed for this action]
            EXPECTED_OUTCOME: [what will be accomplished by THIS specific action]
            
            PARAMETER FORMATS (use exact parameter names):
            • SHELL_COMMAND: command=exact_command_to_run, working_directory=path
            • FILE_WRITE: file_path=exact/path/to/file.ext, content=complete_file_content
            • FILE_READ: file_path=exact/path/to/file.ext
            • CODE_GENERATION: task_description=specific_code_to_generate, language=programming_language
            • AI_ANALYSIS: task_description=what_to_analyze, context=analysis_context
            
            IMPORTANT RULES:
            1. Choose action type that directly accomplishes the current subtask goal
            2. For file creation: Use FILE_WRITE with complete file content
            3. For directory creation: Use SHELL_COMMAND with mkdir commands
            4. For dependency installation: Use SHELL_COMMAND with npm/pip/etc commands
            5. Be specific - avoid generic descriptions
            6. Ensure parameters are complete and executable
            7. DO NOT use code blocks (```) in your response - provide plain text only
            8. For commands: provide the exact command without any formatting
            9. For file content: provide complete code without markdown formatting
            
            Take action NOW for subtask: %s
            """, reasoning, subTask.getId(), subTask.getDescription(), subTask.getDescription());
        
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
            printDetailedFailureInfo(actionSpec, completedTask);
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
        
        // Add detailed logging for debugging
        log.debug("Evaluating success for subtask: {}", subTask.getDescription());
        log.debug("Action was successful: {}", actionResult.isSuccess());
        log.debug("Action description: {}", actionResult.getActionDescription());
        if (actionResult.getResult() != null) {
            log.debug("Action output: {}", actionResult.getResult().getOutput());
            log.debug("Files created: {}", actionResult.getResult().getFilesCreated());
        }
        
        String evaluationPrompt = String.format("""
            Evaluate if this subtask has been completed successfully:
            
            SUBTASK GOAL: %s
            EXPECTED OUTCOME: %s
            
            ACTION TAKEN: %s
            ACTION SUCCESS: %s
            ACTION OUTPUT: %s
            
            OBSERVATION: %s
            
            Consider:
            1. Did the action execute without errors?
            2. Does the output match the expected outcome?
            3. Was the subtask goal actually accomplished?
            4. Are there any files created or commands executed as expected?
            
            Respond with YES or NO followed by your reasoning:
            - YES if the subtask goal was accomplished and the action succeeded
            - NO if there were errors, missing outputs, or the goal wasn't met
            """,
            subTask.getDescription(),
            subTask.getExpectedOutcome(),
            actionResult.getActionDescription(),
            actionResult.isSuccess() ? "SUCCESS" : "FAILED",
            actionResult.getResult() != null ? actionResult.getResult().getOutput() : "No output",
            observation
        );
        
        log.debug("Success evaluation prompt: {}", evaluationPrompt);
        String response = aiClient.sendMessage(evaluationPrompt);
        log.info("✅ AI Success Evaluation: {}", response);
        
        boolean success = response.toLowerCase().contains("yes") && actionResult.isSuccess();
        log.info("📊 Subtask {} evaluated as: {} (Action success: {}, AI evaluation: {})", 
            subTask.getId(), 
            success ? "SUCCESS" : "NEEDS MORE WORK",
            actionResult.isSuccess(),
            response.toLowerCase().contains("yes") ? "YES" : "NO"
        );
        
        // If the subtask failed, print detailed failure information
        if (!success) {
            printSubtaskFailureDetails(subTask, actionResult, response);
        }
        
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
        // Analyze current directory for context
        String directoryAnalysis = analyzeCurrentDirectory();
        
        // Analyze any specific files mentioned in the goal
        String fileAnalysis = analyzeGoalForFiles(goal);
        
        // Detect technology stack and use appropriate template
        String technologyTemplate = buildTechnologySpecificTemplate(goal, directoryAnalysis);
        
        return String.format("""
            You are an AI agent capable of breaking down complex development goals into comprehensive, executable step-by-step plans.
            Your job is to create a detailed execution plan that matches the quality and completeness of professional development tutorials.
            
            **GRANULAR TASK SEPARATION REQUIREMENTS:**
            
            You MUST separate different types of actions into individual subtasks:
            
            1. **TERMINAL COMMAND TASKS**: Each command or set of related commands gets its own subtask
               - Project initialization (mkdir, cd commands)
               - Dependency installation (npm install, pip install)
               - Build/compilation commands
               - Test execution commands
               
            2. **FILE CREATION/EDITING TASKS**: Each file creation or modification gets its own subtask
               - Creating configuration files (package.json, pom.xml)
               - Creating source code files (server.js, App.js, database.js)
               - Creating styling files (App.css, styles.css)
               - Creating documentation files
               
            3. **VERIFICATION TASKS**: Testing and validation get separate subtasks
               - Running tests
               - Manual verification steps
               - Health checks
            
            **EXAMPLES OF PROPER TASK SEPARATION:**
            
            ❌ WRONG (Combined):
            SUBTASK_1:
            Description: Set up backend project with Node.js and install dependencies
            Commands: 
            mkdir todo-app-backend
            cd todo-app-backend
            npm init -y
            npm install express sqlite3 cors
            
            ✅ CORRECT (Separated):
            SUBTASK_1:
            Description: Initialize backend project directory structure
            Commands: 
            mkdir -p todo-app-backend
            cd todo-app-backend
            
            SUBTASK_2:
            Description: Initialize Node.js package configuration
            Commands:
            npm init -y
            
            SUBTASK_3:
            Description: Install backend dependencies
            Commands:
            npm install express sqlite3 cors body-parser
            npm install -D nodemon
            
            SUBTASK_4:
            Description: Create database setup file
            File Path: todo-app-backend/database.js
            File Content: [Complete database.js code]
            
            SUBTASK_5:
            Description: Create Express server file
            File Path: todo-app-backend/server.js
            File Content: [Complete server.js code]
            
            GOAL: %s
            CONTEXT: %s
            CURRENT_DIRECTORY: %s
            
            EXISTING CODEBASE ANALYSIS:
            %s
            
            RELEVANT FILE ANALYSIS:
            %s
            
            TECHNOLOGY-SPECIFIC TEMPLATE:
            %s
            
            **CRITICAL REQUIREMENTS:**
            
            1. **COMPREHENSIVE PLANNING**: Create a plan as detailed as a professional tutorial
            2. **COMPLETE CODE**: Provide full, working code files (not snippets or placeholders)
            3. **REAL COMMANDS**: Use actual, executable shell commands
            4. **PRODUCTION QUALITY**: Include proper error handling, validation, and best practices
            5. **STEP-BY-STEP**: Break complex tasks into logical, sequential steps
            6. **TECHNOLOGY AWARENESS**: Use appropriate frameworks, tools, and conventions for the tech stack
            
            **PLANNING PRINCIPLES:**
            - Follow established project patterns and conventions
            - Include complete file contents (not partial code)
            - Provide working directory structure setup
            - Include dependency installation and configuration
            - Add proper testing and validation steps
            - Consider deployment and production readiness
            - Include documentation and README files
            
            **For Full-Stack Applications:**
            - Separate backend and frontend clearly
            - Include database setup and schema
            - Add API endpoint definitions with complete implementations
            - Include proper CORS, middleware, and security configurations
            - Provide complete UI components with styling
            - Add state management and data flow
            - Include build and deployment configurations
            
            **Response Format (Enhanced):**
            
            SUBTASK_1:
            Description: [Detailed, specific action with context]
            Expected Outcome: [Exact deliverable and success criteria]
            Priority: [CRITICAL|HIGH|MEDIUM|LOW]
            Complexity: [SIMPLE|MODERATE|COMPLEX]
            Dependencies: [Previous subtask numbers or NONE]
            Commands: [Complete shell commands with all parameters]
            mkdir -p project-structure
            cd project-structure
            npm init -y
            npm install express cors body-parser
            Code Language: [Specific language/framework]
            Code Content: [COMPLETE, WORKING code file - not snippets]
            File Path: [Exact relative path from current directory]
            File Content: [COMPLETE file content ready to save]
            
            **ENHANCED EXAMPLES:**
            
            SUBTASK_1:
            Description: Initialize Node.js backend project with Express server and SQLite database setup
            Expected Outcome: Complete backend project structure with working Express server, database connection, and basic API endpoints
            Priority: CRITICAL
            Complexity: MODERATE
            Dependencies: NONE
            Commands: 
            mkdir -p todo-app-backend
            cd todo-app-backend
            npm init -y
            npm install express sqlite3 cors body-parser
            npm install -D nodemon
            Code Language: JavaScript
            Code Content: 
            const express = require('express');
            const cors = require('cors');
            const bodyParser = require('body-parser');
            const db = require('./database');
            
            const app = express();
            const PORT = process.env.PORT || 5000;
            
            // Middleware
            app.use(cors());
            app.use(bodyParser.json());
            app.use(bodyParser.urlencoded({ extended: true }));
            
            // GET all todos
            app.get('/api/todos', (req, res) => {
              const sql = 'SELECT * FROM todos ORDER BY created_at DESC';
              
              db.all(sql, [], (err, rows) => {
                if (err) {
                  res.status(500).json({ error: err.message });
                  return;
                }
                res.json({
                  message: 'success',
                  data: rows
                });
              });
            });
            
            // [Complete server implementation continues...]
            
            app.listen(PORT, () => {
              console.log(`Server is running on port ${PORT}`);
            });
            File Path: todo-app-backend/server.js
            File Content: [Complete Express server with all endpoints - exact match to tutorial]
            
            **TECHNOLOGY-SPECIFIC REQUIREMENTS:**
            
            For React + Node.js + SQLite Todo App:
            - Backend: Complete Express server with all CRUD endpoints
            - Database: SQLite with proper schema and connection handling
            - Frontend: React with functional components, hooks, and Axios
            - Styling: Complete CSS with responsive design
            - API: RESTful endpoints with proper error handling
            - Integration: Full frontend-backend integration
            - Testing: Include test endpoints and validation
            
            For Java Spring Boot Applications:
            - Include complete @SpringBootApplication main class
            - Add all necessary @RestController classes with full CRUD operations
            - Include @Entity classes with JPA annotations
            - Add application.properties with database configuration
            - Include Maven pom.xml with all dependencies
            
            For Python Applications:
            - Include requirements.txt with all dependencies
            - Add complete Flask/Django application structure
            - Include database models and migrations
            - Add proper error handling and validation
            
            **QUALITY STANDARDS:**
            - Each code file must be complete and immediately executable
            - All commands must work without modification
            - Include proper project structure initialization
            - Add configuration files (package.json, pom.xml, etc.)
            - Include proper dependency management
            - Add error handling and validation throughout
            - Provide complete styling and responsive design for UI
            - Include proper API documentation in comments
            
            **DECOMPOSITION TARGET:**
            Break the goal into 12-20 granular subtasks that together create a complete, working application.
            Each subtask should be focused on ONE specific action (either a command OR a file operation).
            
            **GRANULAR SUBTASK GUIDELINES:**
            
            For a React + Node.js + SQLite todo app, you should create subtasks like:
            
            1. Initialize backend project directory
            2. Initialize Node.js package (npm init)
            3. Install Express dependencies
            4. Install SQLite dependencies
            5. Install development dependencies (nodemon)
            6. Create database setup file (database.js)
            7. Create Express server file (server.js)
            8. Initialize React frontend project
            9. Install React dependencies (axios)
            10. Create main App component file
            11. Create CSS styling file
            12. Create additional React components
            13. Test backend endpoints
            14. Test frontend-backend integration
            15. Final application verification
            
            Each subtask should have EITHER:
            - Commands: for terminal operations
            - File Path + File Content: for source code files
            - Expected Outcome: for verification tasks
            
            Now create a comprehensive, tutorial-quality plan for this goal: %s
            """, goal, context.toString(), System.getProperty("user.dir"), directoryAnalysis, fileAnalysis, technologyTemplate, goal);
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
        
        log.debug("Parsing subtasks from AI response:");
        log.debug("Response length: {} characters", response.length());
        log.debug("Response preview: {}", response.length() > 500 ? response.substring(0, 500) + "..." : response);
        
        // Simple parsing implementation - in production would use more robust parsing
        String[] blocks = response.split("SUBTASK_");
        log.debug("Found {} potential subtask blocks", blocks.length - 1);
        
        for (int i = 1; i < blocks.length; i++) {
            String block = blocks[i];
            log.debug("Processing subtask block {}: {}", i, block.length() > 200 ? block.substring(0, 200) + "..." : block);
            SubTask subTask = parseSubTaskBlock(block, i);
            log.info("Created subtask {}: {}", i, subTask.getName());
            subTasks.add(subTask);
        }
        
        log.info("Successfully parsed {} subtasks", subTasks.size());
        return subTasks;
    }
    
    private SubTask parseSubTaskBlock(String block, int index) {
        Map<String, String> fields = parseSubTaskFields(block);
        
        // Extract description and create a meaningful name
        String description = fields.getOrDefault("Description", "AI-generated subtask #" + index);
        String name = extractTaskName(description, index);
        
        // Parse priority with error handling
        SubTask.Priority priority;
        try {
            priority = SubTask.Priority.valueOf(fields.getOrDefault("Priority", "MEDIUM"));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid priority '{}', defaulting to MEDIUM", fields.get("Priority"));
            priority = SubTask.Priority.MEDIUM;
        }
        
        // Parse complexity with error handling
        SubTask.Complexity complexity;
        try {
            complexity = SubTask.Complexity.valueOf(fields.getOrDefault("Complexity", "MODERATE"));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid complexity '{}', defaulting to MODERATE", fields.get("Complexity"));
            complexity = SubTask.Complexity.MODERATE;
        }

        SubTask subTask = SubTask.builder()
            .id("subtask-" + index)
            .name(name)
            .description(description)
            .expectedOutcome(fields.getOrDefault("Expected Outcome", ""))
            .priority(priority)
            .complexity(complexity)
            .dependencies(parseList(fields.getOrDefault("Dependencies", "NONE")))
            .status(SubTask.Status.PENDING)
            .commands(parseCommandList(fields.getOrDefault("Commands", "NONE")))
            .codeLanguage(fields.getOrDefault("Code Language", null))
            .codeContent(fields.getOrDefault("Code Content", null))
            .filePath(fields.getOrDefault("File Path", null))
            .fileContent(fields.getOrDefault("File Content", null))
            .createdAt(Instant.now())
            .build();
        
        // Load file context if this subtask involves file operations
        if (subTask.getFilePath() != null && !subTask.getFilePath().trim().isEmpty()) {
            try {
                subTask = fileContextService.loadFileContext(subTask);
                log.debug("Loaded file context for subtask {}: {}", subTask.getId(), subTask.getFilePath());
            } catch (Exception e) {
                log.warn("Failed to load file context for subtask {}: {}", subTask.getId(), e.getMessage());
            }
        }
        
        return subTask;
    }
    
    /**
     * Extract a meaningful task name from the description
     */
    private String extractTaskName(String description, int index) {
        if (description == null || description.trim().isEmpty()) {
            return "Subtask #" + index;
        }
        
        // Try to extract the first sentence or action verb
        String[] sentences = description.split("[.!?]");
        if (sentences.length > 0) {
            String firstSentence = sentences[0].trim();
            if (firstSentence.length() > 50) {
                // Truncate long descriptions
                return firstSentence.substring(0, 47) + "...";
            }
            return firstSentence;
        }
        
        // Fallback to truncated description
        if (description.length() > 50) {
            return description.substring(0, 47) + "...";
        }
        
        return description;
    }
    
    /**
     * Parse fields from action specification responses
     */
    private Map<String, String> parseFields(String block) {
        Map<String, String> fields = new HashMap<>();
        String[] lines = block.split("\n");
        
        String currentField = null;
        StringBuilder currentValue = new StringBuilder();
        
        for (String line : lines) {
            if (line.contains(":") && (line.startsWith("ACTION_TYPE:") || 
                                     line.startsWith("ACTION_DESCRIPTION:") || 
                                     line.startsWith("PARAMETERS:") || 
                                     line.startsWith("EXPECTED_OUTCOME:"))) {
                // Save previous field if exists
                if (currentField != null) {
                    fields.put(currentField, currentValue.toString().trim());
                }
                
                // Start new field
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    currentField = parts[0].trim();
                    currentValue = new StringBuilder(parts[1].trim());
                }
            } else if (currentField != null && !line.trim().isEmpty()) {
                // Continue multi-line field
                if (currentValue.length() > 0) {
                    currentValue.append("\n");
                }
                currentValue.append(line.trim());
            }
        }
        
        // Save last field
        if (currentField != null) {
            fields.put(currentField, currentValue.toString().trim());
        }
        
        return fields;
    }
    
    /**
     * Parse fields from subtask specification responses
     */
    private Map<String, String> parseSubTaskFields(String block) {
        Map<String, String> fields = new HashMap<>();
        String[] lines = block.split("\n");
        
        String currentField = null;
        StringBuilder currentValue = new StringBuilder();
        
        for (String line : lines) {
            // Check for subtask field patterns
            if (line.contains(":") && (line.startsWith("Description:") || 
                                     line.startsWith("Expected Outcome:") || 
                                     line.startsWith("Priority:") || 
                                     line.startsWith("Complexity:") ||
                                     line.startsWith("Dependencies:") ||
                                     line.startsWith("Commands:") ||
                                     line.startsWith("Code Language:") ||
                                     line.startsWith("Code Content:") ||
                                     line.startsWith("File Path:") ||
                                     line.startsWith("File Content:"))) {
                // Save previous field if exists
                if (currentField != null) {
                    fields.put(currentField, currentValue.toString().trim());
                }
                
                // Start new field
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    currentField = parts[0].trim();
                    currentValue = new StringBuilder(parts[1].trim());
                }
            } else if (currentField != null && !line.trim().isEmpty()) {
                // Continue multi-line field
                if (currentValue.length() > 0) {
                    currentValue.append("\n");
                }
                currentValue.append(line.trim());
            }
        }
        
        // Save last field
        if (currentField != null) {
            fields.put(currentField, currentValue.toString().trim());
        }
        
        log.debug("Parsed subtask fields: {}", fields);
        return fields;
    }
    
    private List<String> parseList(String value) {
        if ("NONE".equals(value)) {
            return new ArrayList<>();
        }
        return Arrays.asList(value.split(","));
    }
    
    private List<String> parseCommandList(String value) {
        if ("NONE".equals(value) || value == null || value.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        List<String> commands = new ArrayList<>();
        String[] lines = value.split("\n");
        
        for (String line : lines) {
            String command = line.trim();
            if (!command.isEmpty() && !command.equals("NONE")) {
                commands.add(command);
            }
        }
        
        return commands;
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
        
        // Parse task type with error handling
        AgentTask.TaskType taskType;
        String actionTypeStr = fields.getOrDefault("ACTION_TYPE", "AI_ANALYSIS");
        try {
            taskType = AgentTask.TaskType.valueOf(actionTypeStr);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown action type '{}', defaulting to AI_ANALYSIS", actionTypeStr);
            taskType = AgentTask.TaskType.AI_ANALYSIS;
        }
        
        // Print command if it's a shell command and we have the command parameter
        if (taskType == AgentTask.TaskType.SHELL_COMMAND && parameters.containsKey("command")) {
            String command = (String) parameters.get("command");
            if (command != null && !command.trim().isEmpty()) {
                System.out.println("🤖 Agent plans to execute: " + command);
            }
        }
        
        // Print file operation if it's file-related
        if ((taskType == AgentTask.TaskType.FILE_WRITE || taskType == AgentTask.TaskType.FILE_READ) 
            && parameters.containsKey("file_path")) {
            String filePath = (String) parameters.get("file_path");
            if (filePath != null && !filePath.trim().isEmpty()) {
                System.out.println("📁 Agent plans to " + 
                    (taskType == AgentTask.TaskType.FILE_WRITE ? "create/write" : "read") + 
                    " file: " + filePath);
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
        if (params == null || params.trim().isEmpty()) {
            log.debug("Parameters string is null or empty");
            return paramMap;
        }
        
        String originalParams = params;
        
        // Clean up code blocks and other formatting artifacts
        params = cleanParameterString(params);
        
        log.debug("Parsing parameters - Original: '{}', Cleaned: '{}'", originalParams, params);
        
        // Try different parsing strategies
        
        // Strategy 1: Parse multiple key=value pairs separated by commas
        // Handle patterns like: command=npm init -y, working_directory=backend_todo
        String[] pairs = params.split(",\\s*(?=[^=]*=)"); // Split on commas that precede key=value pairs
        
        for (String pair : pairs) {
            String pattern = "([^=\\s]+)\\s*=\\s*(.*)";
            java.util.regex.Pattern regexPattern = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher matcher = regexPattern.matcher(pair.trim());
            
            if (matcher.find()) {
                String key = matcher.group(1).trim();
                String value = matcher.group(2).trim();
                
                // Remove surrounding quotes if present
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                if (value.startsWith("'") && value.endsWith("'")) {
                    value = value.substring(1, value.length() - 1);
                }
                
                // For command parameters, be more careful with cleaning
                if ("command".equals(key)) {
                    // Only remove outer code blocks for commands, preserve the command content
                    value = cleanCommandValue(value);
                } else {
                    value = cleanParameterString(value);
                }
                
                paramMap.put(key, value);
                log.debug("  Parameter (Strategy 1): {} = '{}'", key, value);
            }
        }
        
        // Fallback: if regex didn't work, try simple parsing but be smarter about it
        if (paramMap.isEmpty()) {
            log.debug("Strategy 1 failed, trying fallback parsing");
            // Look for the first = sign and treat everything after it as the value
            int equalsIndex = params.indexOf('=');
            if (equalsIndex > 0) {
                String key = params.substring(0, equalsIndex).trim();
                String value = params.substring(equalsIndex + 1).trim();
                
                // Remove quotes if present
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                
                // For command parameters, be more careful with cleaning
                if ("command".equals(key)) {
                    value = cleanCommandValue(value);
                } else {
                    value = cleanParameterString(value);
                }
                
                paramMap.put(key, value);
                log.debug("  Fallback parameter: {} = '{}'", key, value);
            } else {
                log.warn("Could not parse parameters: '{}'", params);
            }
        }
        
        log.debug("Parsed parameters: {}", paramMap);
        return paramMap;
    }
    
    /**
     * Clean parameter strings by removing code blocks and formatting artifacts
     */
    private String cleanParameterString(String params) {
        if (params == null) {
            return "";
        }
        
        // Remove code blocks (``` at start and end)
        params = params.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("```\\s*$", "");
        
        // Remove single backticks
        params = params.replace("`", "");
        
        // Remove any remaining triple backticks that might be embedded
        params = params.replaceAll("```", "");
        
        // Remove double asterisks and other markdown formatting
        params = params.replaceAll("\\*\\*", ""); // Remove **
        params = params.replaceAll("\\*", ""); // Remove single *
        params = params.replaceAll("__", ""); // Remove __
        params = params.replaceAll("_", ""); // Remove single _
        
        // Clean up multiple whitespace and newlines
        params = params.replaceAll("\\s+", " ");
        
        // Remove common markdown artifacts
        params = params.replaceAll("^\\s*-\\s*", ""); // Remove list markers
        params = params.replaceAll("^\\s*\\*\\s*", ""); // Remove bullet points
        
        // Trim
        params = params.trim();
        
        log.debug("Cleaned parameters from '{}' to '{}'", params, params);
        return params;
    }
    
    /**
     * Clean command values more carefully - preserve command content but remove formatting
     */
    private String cleanCommandValue(String command) {
        if (command == null) {
            return "";
        }
        
        String original = command;
        
        // Remove code blocks (``` at start and end) but preserve the command
        command = command.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("```\\s*$", "");
        
        // Remove single backticks only if they wrap the entire command
        if (command.startsWith("`") && command.endsWith("`") && command.length() > 2) {
            command = command.substring(1, command.length() - 1);
        }
        
        // Remove double asterisks (markdown bold) that wrap the entire command
        if (command.startsWith("**") && command.endsWith("**") && command.length() > 4) {
            command = command.substring(2, command.length() - 2);
        }
        
        // Remove any remaining double asterisks (in case they don't wrap the whole command)
        command = command.replaceAll("\\*\\*", "");
        
        // Remove single asterisks (markdown italic)
        command = command.replaceAll("\\*", "");
        
        // Clean up excess whitespace but preserve necessary spaces in commands
        command = command.replaceAll("\\s+", " ");
        command = command.trim();
        
        // Don't remove other content that might be part of the command
        
        log.debug("Cleaned command from '{}' to '{}'", original, command);
        return command;
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
                } else {
                    // Sanitize file path to ensure it's writable
                    String filePath = enhanced.get("file_path").toString();
                    if (filePath.startsWith("/path/to/") || !isValidWritablePath(filePath)) {
                        // Replace invalid paths with safe alternatives
                        String sanitizedPath = sanitizeFilePath(filePath, description);
                        enhanced.put("file_path", sanitizedPath);
                        log.warn("Sanitized invalid file path '{}' to '{}'", filePath, sanitizedPath);
                    }
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
    
    private boolean isValidWritablePath(String filePath) {
        // Check for common invalid path patterns
        if (filePath == null || filePath.trim().isEmpty()) {
            return false;
        }
        
        // Check for placeholder paths
        if (filePath.startsWith("/path/to/") || 
            filePath.startsWith("/example/") ||
            filePath.equals("/dev/null") ||
            filePath.startsWith("/tmp/nonexistent/") ||
            filePath.startsWith("/usr/") ||
            filePath.startsWith("/System/")) {
            return false;
        }
        
        // Check if path is reasonable (not in system directories)
        return !filePath.startsWith("/bin/") && 
               !filePath.startsWith("/sbin/") && 
               !filePath.startsWith("/etc/");
    }
    
    private String sanitizeFilePath(String originalPath, String description) {
        // Extract filename from the original path
        String filename = originalPath.substring(originalPath.lastIndexOf('/') + 1);
        
        // If no filename or invalid filename, generate one from description
        if (filename.isEmpty() || filename.equals("*") || filename.equals("?")) {
            if (description.toLowerCase().contains("sql") || originalPath.toLowerCase().contains("sql")) {
                filename = "todoApp.sql";
            } else if (description.toLowerCase().contains("todo")) {
                filename = "todoApp.txt";
            } else if (description.toLowerCase().contains("database")) {
                filename = "database.sql";
            } else {
                String extension = "";
                if (originalPath.contains(".")) {
                    extension = originalPath.substring(originalPath.lastIndexOf('.'));
                }
                filename = "generated_file" + extension;
            }
        }
        
        // Return a safe path in current directory
        return "./" + filename;
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
    
    /**
     * Execute AI-generated directives (commands, file creation) for a subtask
     */
    private void executeSubTaskDirectives(SubTask subTask) {
        try {
            // Save files if specified
            if (subTask.getFilePath() != null && subTask.getFileContent() != null) {
                saveFileToCurrentDirectory(subTask.getFilePath(), subTask.getFileContent());
            }
            
            // Execute commands if specified
            if (subTask.getCommands() != null && !subTask.getCommands().isEmpty()) {
                executeCommands(subTask.getCommands(), subTask.getId());
            }
            
            // Log code content if provided (for reference)
            if (subTask.getCodeContent() != null && !subTask.getCodeContent().trim().isEmpty()) {
                System.out.println("📝 AI Generated Code (" + 
                    (subTask.getCodeLanguage() != null ? subTask.getCodeLanguage() : "unknown") + "):");
                System.out.println("```");
                System.out.println(subTask.getCodeContent());
                System.out.println("```");
            }
            
        } catch (Exception e) {
            log.error("Error executing subtask directives for {}: {}", subTask.getId(), e.getMessage());
            System.out.println("❌ Error executing AI directives: " + e.getMessage());
        }
    }
    
    /**
     * Save file content to the current directory
     */
    private void saveFileToCurrentDirectory(String filePath, String content) {
        try {
            // Ensure the file path is relative to current directory
            String safePath = sanitizeFilePath(filePath, "AI generated file");
            Path targetPath = Paths.get(safePath);
            
            // Create parent directories if needed
            if (targetPath.getParent() != null) {
                Files.createDirectories(targetPath.getParent());
            }
            
            // Write content to file
            Files.write(targetPath, content.getBytes(), 
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            System.out.println("💾 Saved file: " + targetPath.toAbsolutePath());
            log.info("Saved AI-generated file: {}", targetPath.toAbsolutePath());
            
        } catch (Exception e) {
            log.error("Error saving file {}: {}", filePath, e.getMessage());
            System.out.println("❌ Error saving file " + filePath + ": " + e.getMessage());
        }
    }
    
    /**
     * Execute a list of shell commands
     */
    private void executeCommands(List<String> commands, String subtaskId) {
        for (String command : commands) {
            if (command == null || command.trim().isEmpty()) {
                continue;
            }
            
            try {
                System.out.println("🚀 Executing command: " + command);
                log.info("Executing AI command for subtask {}: {}", subtaskId, command);
                
                // Create a shell command task and execute it via the task queue
                AgentTask commandTask = AgentTask.builder()
                    .id("ai-command-" + System.currentTimeMillis())
                    .name("AI Command: " + command)
                    .type(AgentTask.TaskType.SHELL_COMMAND)
                    .description("Execute AI-generated command: " + command)
                    .parameters(Map.of("command", command))
                    .priority(AgentTask.TaskPriority.HIGH)
                    .createdAt(Instant.now())
                    .build();
                
                String taskId = taskQueue.submitTask(commandTask);
                
                // Wait for command completion
                AgentTask completedTask = waitForTaskCompletion(taskId);
                
                if (completedTask.getStatus() == AgentTask.TaskStatus.COMPLETED) {
                    System.out.println("✅ Command completed successfully");
                    if (completedTask.getResult() != null && completedTask.getResult().getOutput() != null) {
                        String output = completedTask.getResult().getOutput().trim();
                        if (!output.isEmpty()) {
                            System.out.println("📤 Output: " + output);
                        }
                    }
                } else {
                    System.out.println("❌ Command failed: " + completedTask.getErrorMessage());
                    log.warn("AI command failed for subtask {}: {}", subtaskId, completedTask.getErrorMessage());
                }
                
            } catch (Exception e) {
                log.error("Error executing command '{}' for subtask {}: {}", command, subtaskId, e.getMessage());
                System.out.println("❌ Error executing command '" + command + "': " + e.getMessage());
            }
        }
    }
    
    /**
     * Analyze the current directory to understand the existing codebase structure,
     * patterns, and conventions for better AI planning context
     */
    private String analyzeCurrentDirectory() {
        try {
            StringBuilder analysis = new StringBuilder();
            Path currentDir = Paths.get(System.getProperty("user.dir"));
            
            analysis.append("Current Directory: ").append(currentDir.toAbsolutePath()).append("\n\n");
            
            // 1. Analyze project structure
            analysis.append("PROJECT STRUCTURE:\n");
            analyzeProjectStructure(currentDir, analysis, 0, 3); // Max depth 3
            analysis.append("\n");
            
            // 2. Detect project type and technologies
            analysis.append("PROJECT TYPE & TECHNOLOGIES:\n");
            analyzeProjectType(currentDir, analysis);
            analysis.append("\n");
            
            // 3. Analyze configuration files
            analysis.append("CONFIGURATION FILES:\n");
            analyzeConfigurationFiles(currentDir, analysis);
            analysis.append("\n");
            
            // 4. Analyze key source files
            analysis.append("KEY SOURCE FILES:\n");
            analyzeKeySourceFiles(currentDir, analysis);
            analysis.append("\n");
            
            // 5. Analyze existing patterns and conventions
            analysis.append("EXISTING PATTERNS:\n");
            analyzeCodePatterns(currentDir, analysis);
            
            return analysis.toString();
            
        } catch (Exception e) {
            log.warn("Error analyzing current directory: {}", e.getMessage());
            return "Directory analysis failed: " + e.getMessage();
        }
    }
    
    /**
     * Analyze project structure recursively
     */
    private void analyzeProjectStructure(Path dir, StringBuilder analysis, int depth, int maxDepth) {
        if (depth > maxDepth) return;
        
        try (Stream<Path> files = Files.list(dir)) {
            String indent = "  ".repeat(depth);
            
            files.sorted()
                .filter(path -> !shouldIgnoreFile(path))
                .limit(20) // Limit to prevent overwhelming output
                .forEach(path -> {
                    try {
                        if (Files.isDirectory(path)) {
                            analysis.append(indent).append("📁 ").append(path.getFileName()).append("/\n");
                            if (depth < maxDepth) {
                                analyzeProjectStructure(path, analysis, depth + 1, maxDepth);
                            }
                        } else {
                            analysis.append(indent).append("📄 ").append(path.getFileName()).append("\n");
                        }
                    } catch (Exception e) {
                        // Skip files that can't be accessed
                    }
                });
        } catch (IOException e) {
            analysis.append("Error reading directory: ").append(dir).append("\n");
        }
    }
    
    /**
     * Detect project type based on files and structure
     */
    private void analyzeProjectType(Path dir, StringBuilder analysis) {
        List<String> technologies = new ArrayList<>();
        
        try (Stream<Path> files = Files.list(dir)) {
            files.forEach(path -> {
                String fileName = path.getFileName().toString().toLowerCase();
                
                // Java projects
                if (fileName.equals("pom.xml")) technologies.add("Maven (Java)");
                if (fileName.equals("build.gradle")) technologies.add("Gradle (Java/Kotlin)");
                if (fileName.equals("mvnw") || fileName.equals("mvnw.cmd")) technologies.add("Maven Wrapper");
                
                // Node.js projects
                if (fileName.equals("package.json")) technologies.add("Node.js/npm");
                if (fileName.equals("yarn.lock")) technologies.add("Yarn");
                
                // Python projects
                if (fileName.equals("requirements.txt")) technologies.add("Python (pip)");
                if (fileName.equals("setup.py")) technologies.add("Python setuptools");
                if (fileName.equals("pyproject.toml")) technologies.add("Python (modern packaging)");
                
                // Other technologies
                if (fileName.equals("dockerfile")) technologies.add("Docker");
                if (fileName.equals("docker-compose.yml")) technologies.add("Docker Compose");
                if (fileName.equals("makefile")) technologies.add("Make build system");
                
                // Spring Boot
                if (fileName.equals("application.properties") || fileName.equals("application.yml")) {
                    technologies.add("Spring Boot");
                }
            });
        } catch (IOException e) {
            analysis.append("Error detecting project type\n");
        }
        
        if (technologies.isEmpty()) {
            analysis.append("- Unknown project type\n");
        } else {
            technologies.forEach(tech -> analysis.append("- ").append(tech).append("\n"));
        }
    }
    
    /**
     * Analyze configuration files for project context
     */
    private void analyzeConfigurationFiles(Path dir, StringBuilder analysis) {
        String[] configFiles = {
            "pom.xml", "build.gradle", "package.json", "requirements.txt",
            "application.properties", "application.yml", ".env", "Dockerfile",
            "docker-compose.yml", "CLAUDE.md", "README.md"
        };
        
        for (String configFile : configFiles) {
            Path configPath = dir.resolve(configFile);
            if (Files.exists(configPath)) {
                analysis.append("- ").append(configFile);
                try {
                    // Add brief content summary for key files
                    if (configFile.equals("pom.xml")) {
                        String content = Files.readString(configPath);
                        if (content.contains("<groupId>")) {
                            String groupId = extractXmlValue(content, "groupId");
                            String artifactId = extractXmlValue(content, "artifactId");
                            analysis.append(" (").append(groupId).append(":").append(artifactId).append(")");
                        }
                    } else if (configFile.equals("package.json")) {
                        String content = Files.readString(configPath);
                        if (content.contains("\"name\"")) {
                            String name = extractJsonValue(content, "name");
                            analysis.append(" (").append(name).append(")");
                        }
                    }
                    analysis.append(" ✓\n");
                } catch (Exception e) {
                    analysis.append(" (error reading)\n");
                }
            }
        }
    }
    
    /**
     * Analyze key source files to understand the codebase
     */
    private void analyzeKeySourceFiles(Path dir, StringBuilder analysis) {
        try {
            // Look for main application files
            findMainFiles(dir, analysis);
            
            // Count source files by type
            Map<String, Integer> fileTypes = new HashMap<>();
            countSourceFiles(dir, fileTypes, 0, 3);
            
            analysis.append("Source file summary:\n");
            fileTypes.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> analysis.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append(" files\n"));
            
        } catch (Exception e) {
            analysis.append("Error analyzing source files\n");
        }
    }
    
    /**
     * Find main application entry points
     */
    private void findMainFiles(Path dir, StringBuilder analysis) {
        try (Stream<Path> paths = Files.walk(dir, 4)) {
            paths.filter(Files::isRegularFile)
                .filter(path -> {
                    String fileName = path.getFileName().toString();
                    return fileName.equals("Main.java") || 
                           fileName.equals("Application.java") ||
                           fileName.endsWith("Application.java") ||
                           fileName.equals("main.py") ||
                           fileName.equals("app.py") ||
                           fileName.equals("index.js") ||
                           fileName.equals("server.js");
                })
                .limit(5)
                .forEach(path -> {
                    String relativePath = dir.relativize(path).toString();
                    analysis.append("- Main file: ").append(relativePath).append("\n");
                });
        } catch (IOException e) {
            // Ignore errors in file walking
        }
    }
    
    /**
     * Count source files by extension
     */
    private void countSourceFiles(Path dir, Map<String, Integer> fileTypes, int depth, int maxDepth) {
        if (depth > maxDepth) return;
        
        try (Stream<Path> files = Files.list(dir)) {
            files.forEach(path -> {
                if (shouldIgnoreFile(path)) return;
                
                if (Files.isDirectory(path)) {
                    countSourceFiles(path, fileTypes, depth + 1, maxDepth);
                } else {
                    String fileName = path.getFileName().toString();
                    String extension = getFileExtension(fileName);
                    if (isSourceFileExtension(extension)) {
                        fileTypes.merge(extension, 1, Integer::sum);
                    }
                }
            });
        } catch (IOException e) {
            // Ignore errors
        }
    }
    
    /**
     * Analyze code patterns and conventions
     */
    private void analyzeCodePatterns(Path dir, StringBuilder analysis) {
        List<String> patterns = new ArrayList<>();
        
        // Check for Spring Boot patterns
        if (hasFile(dir, "application.properties") || hasFile(dir, "application.yml")) {
            patterns.add("Spring Boot configuration pattern");
        }
        
        // Check for Maven patterns
        if (hasFile(dir, "pom.xml")) {
            patterns.add("Maven project structure");
        }
        
        // Check for package structure
        Path srcMain = dir.resolve("src/main/java");
        if (Files.exists(srcMain)) {
            patterns.add("Maven standard directory layout");
        }
        
        // Check for test patterns
        Path srcTest = dir.resolve("src/test/java");
        if (Files.exists(srcTest)) {
            patterns.add("JUnit test structure");
        }
        
        // Check for Spring annotations in source files
        if (hasSpringAnnotations(dir)) {
            patterns.add("Spring Framework annotations (@Component, @Service, @Repository)");
        }
        
        if (patterns.isEmpty()) {
            analysis.append("- No specific patterns detected\n");
        } else {
            patterns.forEach(pattern -> analysis.append("- ").append(pattern).append("\n"));
        }
    }
    
    // Helper methods
    
    private boolean shouldIgnoreFile(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.startsWith(".") || 
               fileName.equals("target") || 
               fileName.equals("node_modules") ||
               fileName.equals("__pycache__") ||
               fileName.equals("build") ||
               fileName.equals("dist");
    }
    
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1).toLowerCase() : "";
    }
    
    private boolean isSourceFileExtension(String extension) {
        return Arrays.asList("java", "py", "js", "ts", "jsx", "tsx", "html", "css", "scss", 
                           "xml", "yml", "yaml", "json", "sql", "sh", "md").contains(extension);
    }
    
    private boolean hasFile(Path dir, String fileName) {
        return Files.exists(dir.resolve(fileName));
    }
    
    private boolean hasSpringAnnotations(Path dir) {
        try (Stream<Path> paths = Files.walk(dir, 3)) {
            return paths.filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .limit(10) // Check first 10 Java files
                .anyMatch(this::containsSpringAnnotations);
        } catch (IOException e) {
            return false;
        }
    }
    
    private boolean containsSpringAnnotations(Path javaFile) {
        try {
            String content = Files.readString(javaFile);
            return content.contains("@Component") || 
                   content.contains("@Service") || 
                   content.contains("@Repository") ||
                   content.contains("@RestController") ||
                   content.contains("@Configuration");
        } catch (Exception e) {
            return false;
        }
    }
    
    private String extractXmlValue(String xml, String tagName) {
        try {
            String startTag = "<" + tagName + ">";
            String endTag = "</" + tagName + ">";
            int start = xml.indexOf(startTag);
            int end = xml.indexOf(endTag, start);
            if (start != -1 && end != -1) {
                return xml.substring(start + startTag.length(), end).trim();
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return "unknown";
    }
    
    private String extractJsonValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\"";
            int start = json.indexOf(searchKey);
            if (start != -1) {
                int colonIndex = json.indexOf(":", start);
                int valueStart = json.indexOf("\"", colonIndex) + 1;
                int valueEnd = json.indexOf("\"", valueStart);
                if (valueStart > 0 && valueEnd > valueStart) {
                    return json.substring(valueStart, valueEnd);
                }
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return "unknown";
    }
    
    /**
     * Check for missing tools/commands and suggest OS-appropriate installation commands
     */
    private void checkAndSuggestMissingTools(SubTask subTask, PlanExecution execution) {
        try {
            List<String> missingTools = new ArrayList<>();
            
            // Check commands in the subtask
            if (subTask.getCommands() != null) {
                for (String command : subTask.getCommands()) {
                    List<String> toolsInCommand = extractToolsFromCommand(command);
                    for (String tool : toolsInCommand) {
                        if (!isToolAvailable(tool)) {
                            missingTools.add(tool);
                        }
                    }
                }
            }
            
            // Check if code language tools are available
            if (subTask.getCodeLanguage() != null) {
                String languageTool = getLanguageToolCommand(subTask.getCodeLanguage());
                if (languageTool != null && !isToolAvailable(languageTool)) {
                    missingTools.add(languageTool);
                }
            }
            
            // If missing tools found, suggest installation commands
            if (!missingTools.isEmpty()) {
                System.out.println("⚠️  Missing tools detected for subtask: " + subTask.getDescription());
                suggestToolInstallations(missingTools, execution);
            }
            
        } catch (Exception e) {
            log.warn("Error checking missing tools for subtask {}: {}", subTask.getId(), e.getMessage());
        }
    }
    
    /**
     * Extract tool names from command strings
     */
    private List<String> extractToolsFromCommand(String command) {
        List<String> tools = new ArrayList<>();
        if (command == null || command.trim().isEmpty()) {
            return tools;
        }
        
        String[] parts = command.trim().split("\\s+");
        if (parts.length > 0) {
            String tool = parts[0];
            
            // Handle common command patterns
            if (tool.equals("sudo") && parts.length > 1) {
                tool = parts[1]; // Get the actual command after sudo
            }
            
            // Extract base command name (remove paths)
            if (tool.contains("/")) {
                tool = tool.substring(tool.lastIndexOf("/") + 1);
            }
            
            tools.add(tool);
        }
        
        return tools;
    }
    
    /**
     * Get the command tool for a programming language
     */
    private String getLanguageToolCommand(String language) {
        return switch (language.toLowerCase()) {
            case "python", "python3" -> "python3";
            case "node", "javascript", "js" -> "node";
            case "java" -> "java";
            case "go" -> "go";
            case "rust" -> "cargo";
            case "php" -> "php";
            case "ruby" -> "ruby";
            case "dotnet", "c#", "csharp" -> "dotnet";
            case "swift" -> "swift";
            case "kotlin" -> "kotlin";
            default -> null;
        };
    }
    
    /**
     * Check if a tool/command is available on the system
     */
    private boolean isToolAvailable(String tool) {
        try {
            // Use 'which' on Unix/Linux/macOS or 'where' on Windows
            String checkCommand = isWindows() ? "where " + tool : "which " + tool;
            
            ProcessBuilder pb = new ProcessBuilder();
            if (isWindows()) {
                pb.command("cmd", "/c", checkCommand);
            } else {
                String shell = getDefaultShell();
                pb.command(shell, "-c", checkCommand);
            }
            
            Process process = pb.start();
            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            
            return process.exitValue() == 0;
            
        } catch (Exception e) {
            log.debug("Error checking tool availability for {}: {}", tool, e.getMessage());
            return false;
        }
    }
    
    /**
     * Suggest installation commands for missing tools using AI
     */
    private void suggestToolInstallations(List<String> missingTools, PlanExecution execution) {
        try {
            String osInfo = getOperatingSystemInfo();
            String prompt = buildInstallationSuggestionPrompt(missingTools, osInfo);
            
            log.info("🤖 Asking AI for installation suggestions for missing tools: {}", missingTools);
            String suggestions = aiClient.sendMessage(prompt);
            
            System.out.println("🔧 AI Installation Suggestions:");
            System.out.println(suggestions);
            System.out.println("");
            
            // Store suggestions in execution memory for later reference
            execution.getWorkingMemory().put("missing_tools", String.join(", ", missingTools));
            execution.getWorkingMemory().put("installation_suggestions", suggestions);
            
            // Ask user if they want to proceed with installations
            System.out.println("💡 Would you like to install these tools automatically? (This will be handled in the next planning cycle)");
            
        } catch (Exception e) {
            log.error("Error getting installation suggestions: {}", e.getMessage());
            System.out.println("❌ Error getting installation suggestions: " + e.getMessage());
        }
    }
    
    /**
     * Build prompt for AI to suggest installation commands
     */
    private String buildInstallationSuggestionPrompt(List<String> missingTools, String osInfo) {
        return String.format("""
            You are an expert system administrator helping to install missing tools/commands.
            
            MISSING TOOLS: %s
            OPERATING SYSTEM: %s
            
            For each missing tool, provide the appropriate installation command for this operating system.
            Consider multiple installation methods when available (package manager, official installer, etc.).
            
            **IMPORTANT REQUIREMENTS:**
            - Provide commands that are safe and commonly used
            - Include brief explanations of what each tool does
            - Suggest the most reliable installation method first
            - For package managers, use the most appropriate one for the OS
            - Include verification commands to check successful installation
            
            **Response Format:**
            For each tool, provide:
            
            🔧 **[TOOL_NAME]** - [Brief description]
            
            **Installation Options:**
            1. **Recommended**: [Primary installation command]
            2. **Alternative**: [Alternative method if available]
            
            **Verify Installation**: [Command to verify it worked]
            
            **Example:**
            🔧 **python3** - Python programming language interpreter
            
            **Installation Options:**
            1. **Recommended**: brew install python3  (macOS with Homebrew)
            2. **Alternative**: Download from https://python.org/downloads/
            
            **Verify Installation**: python3 --version
            
            Now provide installation suggestions for the missing tools: %s
            """, String.join(", ", missingTools), osInfo, String.join(", ", missingTools));
    }
    
    /**
     * Get detailed operating system information
     */
    private String getOperatingSystemInfo() {
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String osArch = System.getProperty("os.arch");
        
        StringBuilder osInfo = new StringBuilder();
        osInfo.append(osName).append(" ").append(osVersion).append(" (").append(osArch).append(")");
        
        // Add more specific information based on OS
        if (osName.toLowerCase().contains("mac")) {
            osInfo.append(" - macOS");
            // Check if Homebrew is available
            if (isToolAvailable("brew")) {
                osInfo.append(" with Homebrew");
            }
            if (isToolAvailable("port")) {
                osInfo.append(" with MacPorts");
            }
        } else if (osName.toLowerCase().contains("linux")) {
            osInfo.append(" - Linux");
            // Check for common package managers
            if (isToolAvailable("apt")) {
                osInfo.append(" with APT (Debian/Ubuntu)");
            } else if (isToolAvailable("yum")) {
                osInfo.append(" with YUM (RHEL/CentOS)");
            } else if (isToolAvailable("dnf")) {
                osInfo.append(" with DNF (Fedora)");
            } else if (isToolAvailable("pacman")) {
                osInfo.append(" with Pacman (Arch)");
            } else if (isToolAvailable("zypper")) {
                osInfo.append(" with Zypper (openSUSE)");
            }
        } else if (osName.toLowerCase().contains("windows")) {
            osInfo.append(" - Windows");
            if (isToolAvailable("choco")) {
                osInfo.append(" with Chocolatey");
            }
            if (isToolAvailable("winget")) {
                osInfo.append(" with WinGet");
            }
            if (isToolAvailable("scoop")) {
                osInfo.append(" with Scoop");
            }
        }
        
        return osInfo.toString();
    }
    
    /**
     * Check if running on Windows
     */
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
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
    
    /**
     * Analyze the goal to identify any specific files mentioned and provide context about them
     */
    private String analyzeGoalForFiles(String goal) {
        if (goal == null || goal.trim().isEmpty()) {
            return "No specific files mentioned in the goal.";
        }
        
        StringBuilder analysis = new StringBuilder();
        analysis.append("FILE CONTEXT ANALYSIS:\n");
        
        // Look for common file patterns in the goal
        List<String> potentialFiles = extractPotentialFileNames(goal);
        
        if (potentialFiles.isEmpty()) {
            analysis.append("- No specific file names detected in the goal\n");
        } else {
            analysis.append("- Potential files mentioned in goal:\n");
            
            for (String fileName : potentialFiles) {
                analysis.append("  * ").append(fileName);
                
                // Analyze each potential file
                try {
                    String fileContext = fileContextService.analyzeFileForAI(fileName);
                    analysis.append(" -> ").append(fileContext.replace("\n", "\n    ")).append("\n");
                } catch (Exception e) {
                    analysis.append(" -> File analysis failed: ").append(e.getMessage()).append("\n");
                }
            }
        }
        
        // Look for file operation keywords
        String goalLower = goal.toLowerCase();
        if (goalLower.contains("create") || goalLower.contains("generate")) {
            analysis.append("- Goal involves FILE CREATION\n");
        }
        if (goalLower.contains("modify") || goalLower.contains("update") || goalLower.contains("edit") || goalLower.contains("change")) {
            analysis.append("- Goal involves FILE MODIFICATION (preserve existing content)\n");
        }
        if (goalLower.contains("replace") || goalLower.contains("overwrite")) {
            analysis.append("- Goal involves FILE REPLACEMENT (overwrite content)\n");
        }
        if (goalLower.contains("append") || goalLower.contains("add to")) {
            analysis.append("- Goal involves FILE APPENDING (add to existing content)\n");
        }
        
        return analysis.toString();
    }
    
    /**
     * Extract potential file names from the goal text
     */
    private List<String> extractPotentialFileNames(String goal) {
        List<String> files = new ArrayList<>();
        
        // Common file patterns with extensions
        String[] commonExtensions = {
            ".py", ".java", ".js", ".ts", ".cpp", ".c", ".h", ".cs", ".php", ".rb", ".go", ".rs",
            ".txt", ".md", ".json", ".yml", ".yaml", ".xml", ".csv", ".html", ".css", ".sql",
            ".properties", ".conf", ".cfg", ".ini", ".dockerfile"
        };
        
        for (String ext : commonExtensions) {
            // Look for patterns like "filename.ext" or "file_name.ext"
            String pattern = "\\b[\\w_\\-]+\\" + ext + "\\b";
            java.util.regex.Pattern regexPattern = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher matcher = regexPattern.matcher(goal);
            
            while (matcher.find()) {
                String fileName = matcher.group();
                if (!files.contains(fileName)) {
                    files.add(fileName);
                }
            }
        }
        
        // Look for quoted file paths
        java.util.regex.Pattern quotedPattern = java.util.regex.Pattern.compile("\"([^\"]+\\.[a-zA-Z0-9]+)\"");
        java.util.regex.Matcher quotedMatcher = quotedPattern.matcher(goal);
        while (quotedMatcher.find()) {
            String fileName = quotedMatcher.group(1);
            if (!files.contains(fileName)) {
                files.add(fileName);
            }
        }
        
        // Look for common file names without extensions but with context
        String[] commonFileNames = {
            "readme", "license", "makefile", "dockerfile", "requirements",
            "package.json", "pom.xml", "build.gradle", "setup.py"
        };
        
        String goalLower = goal.toLowerCase();
        for (String commonName : commonFileNames) {
            if (goalLower.contains(commonName)) {
                // Try to find the actual file with common extensions
                if (commonName.equals("readme")) {
                    if (fileContextService != null) {
                        // Check for README.md, README.txt, etc.
                        String[] readmeVariants = {"README.md", "README.txt", "README.rst", "readme.md"};
                        for (String variant : readmeVariants) {
                            try {
                                if (variant.toLowerCase().contains("readme") && !files.contains(variant)) {
                                    files.add(variant);
                                    break;
                                }
                            } catch (Exception e) {
                                // Ignore errors
                            }
                        }
                    }
                } else if (!files.contains(commonName)) {
                    files.add(commonName);
                }
            }
        }
        
        return files;
    }
    
    /**
     * Check availability of common system tools for AI awareness
     */
    private String checkAvailableSystemTools() {
        StringBuilder toolStatus = new StringBuilder();
        
        // Common development tools to check
        String[] commonTools = {
            "git", "docker", "python3", "node", "npm", "java", "javac", "maven", "mvn",
            "pip", "pip3", "yarn", "go", "rust", "cargo", "php", "ruby", "dotnet"
        };
        
        List<String> available = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        
        for (String tool : commonTools) {
            if (isToolAvailable(tool)) {
                available.add(tool);
            } else {
                missing.add(tool);
            }
        }
        
        toolStatus.append("Available: ").append(String.join(", ", available)).append("\n");
        if (!missing.isEmpty()) {
            toolStatus.append("Missing: ").append(String.join(", ", missing));
        }
        
        return toolStatus.toString();
    }
    
    /**
     * Build technology-specific planning template based on the goal and current environment
     */
    private String buildTechnologySpecificTemplate(String goal, String directoryAnalysis) {
        StringBuilder template = new StringBuilder();
        
        String goalLower = goal.toLowerCase();
        
        // Detect technology stack from goal and environment
        boolean isReactApp = goalLower.contains("react") || goalLower.contains("frontend");
        boolean isNodeBackend = goalLower.contains("node") || goalLower.contains("express") || goalLower.contains("javascript backend");
        boolean isJavaApp = goalLower.contains("java") || goalLower.contains("spring") || directoryAnalysis.contains("pom.xml");
        boolean isPythonApp = goalLower.contains("python") || goalLower.contains("flask") || goalLower.contains("django");
        boolean isTodoApp = goalLower.contains("todo") || goalLower.contains("task");
        boolean hasDatabase = goalLower.contains("database") || goalLower.contains("sqlite") || goalLower.contains("mysql") || goalLower.contains("postgres");
        
        template.append("TECHNOLOGY-SPECIFIC PLANNING TEMPLATE:\n\n");
        
        // React + Node.js + SQLite Todo App Template
        if (isReactApp && isNodeBackend && isTodoApp) {
            template.append("""
                **FULL-STACK TODO APPLICATION TEMPLATE (React + Node.js + SQLite)**
                
                **PROJECT STRUCTURE:**
                ```
                todo-app/
                ├── todo-app-backend/          # Node.js Express backend
                │   ├── server.js              # Main Express server
                │   ├── database.js            # SQLite database setup
                │   ├── package.json           # Backend dependencies
                │   └── todos.db              # SQLite database file (auto-generated)
                └── todo-app-frontend/         # React frontend
                    ├── src/
                    │   ├── App.js            # Main React component
                    │   ├── App.css           # Application styling
                    │   └── index.js          # React entry point
                    ├── package.json          # Frontend dependencies
                    └── public/
                ```
                
                **BACKEND REQUIREMENTS:**
                - Express server with CORS enabled
                - SQLite database with todos table
                - Full CRUD API endpoints:
                  * GET /api/todos - List all todos
                  * GET /api/todos/:id - Get single todo
                  * POST /api/todos - Create new todo
                  * PUT /api/todos/:id - Update todo
                  * PATCH /api/todos/:id/toggle - Toggle completion
                  * DELETE /api/todos/:id - Delete todo
                  * GET /api/health - Health check
                - Proper error handling and validation
                - Database connection management
                - Body parsing middleware
                
                **FRONTEND REQUIREMENTS:**
                - React functional components with hooks
                - Axios for HTTP requests
                - State management for todos, loading, errors
                - Components:
                  * Main App component
                  * TodoItem component for display
                  * EditTodoForm component for inline editing
                - Complete CRUD operations
                - Responsive CSS styling
                - Error handling and loading states
                - Form validation
                
                **DATABASE SCHEMA:**
                ```sql
                CREATE TABLE todos (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  title TEXT NOT NULL,
                  description TEXT,
                  completed BOOLEAN DEFAULT FALSE,
                  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
                );
                ```
                
                **DEPENDENCIES:**
                Backend: express, sqlite3, cors, body-parser, nodemon (dev)
                Frontend: react, axios, react-dom, react-scripts
                
                **GRANULAR EXECUTION STEPS (15-20 Individual Tasks):**
                
                **BACKEND SETUP (Tasks 1-7):**
                1. Create backend project directory
                2. Initialize npm package (npm init -y)
                3. Install Express dependencies
                4. Install SQLite dependencies  
                5. Install development dependencies (nodemon)
                6. Create database.js file with schema
                7. Create server.js file with all CRUD endpoints
                
                **FRONTEND SETUP (Tasks 8-13):**
                8. Create React app with create-react-app
                9. Install Axios for API communication
                10. Create main App.js component
                11. Create App.css with responsive styling
                12. Create TodoItem component
                13. Create EditTodoForm component
                
                **INTEGRATION & TESTING (Tasks 14-18):**
                14. Test backend API endpoints
                15. Test frontend-backend integration
                16. Test CRUD operations end-to-end
                17. Verify responsive design
                18. Final application validation
                
                **TASK TYPE GUIDELINES:**
                - Directory/npm commands → SHELL_COMMAND tasks
                - File creation → FILE_WRITE tasks
                - Testing → AI_ANALYSIS tasks
                - Each file gets its own individual task
                - Each command sequence gets its own task
                """);
        }
        
        // Java Spring Boot Template
        else if (isJavaApp) {
            template.append("""
                **SPRING BOOT APPLICATION TEMPLATE**
                
                **PROJECT STRUCTURE:**
                ```
                project-name/
                ├── src/main/java/
                │   └── com/example/app/
                │       ├── Application.java          # Main Spring Boot class
                │       ├── controller/               # REST controllers
                │       ├── service/                  # Business logic
                │       ├── repository/               # Data access
                │       └── model/                    # Entity classes
                ├── src/main/resources/
                │   ├── application.properties        # Configuration
                │   └── schema.sql                   # Database schema
                ├── src/test/java/                   # Unit tests
                └── pom.xml                          # Maven dependencies
                ```
                
                **REQUIREMENTS:**
                - @SpringBootApplication main class
                - @RestController with @RequestMapping
                - @Entity classes with JPA annotations
                - @Service layer for business logic
                - @Repository interfaces extending JpaRepository
                - Complete CRUD operations
                - Exception handling with @ControllerAdvice
                - Application configuration in properties
                - Maven dependencies for web, JPA, database
                
                **DEPENDENCIES (pom.xml):**
                - spring-boot-starter-web
                - spring-boot-starter-data-jpa
                - Database driver (H2, MySQL, PostgreSQL)
                - spring-boot-starter-test
                """);
        }
        
        // Python Application Template
        else if (isPythonApp) {
            template.append("""
                **PYTHON APPLICATION TEMPLATE**
                
                **PROJECT STRUCTURE:**
                ```
                project-name/
                ├── app.py                    # Main application file
                ├── requirements.txt          # Python dependencies
                ├── models/                   # Database models
                ├── routes/                   # API routes
                ├── static/                   # Static files
                ├── templates/                # HTML templates (if using)
                └── tests/                    # Unit tests
                ```
                
                **REQUIREMENTS:**
                - Flask/Django application setup
                - Database models with SQLAlchemy/Django ORM
                - API routes with proper HTTP methods
                - Error handling and validation
                - Virtual environment setup
                - Requirements.txt with all dependencies
                
                **DEPENDENCIES:**
                Flask: flask, flask-sqlalchemy, flask-cors
                Django: django, djangorestframework
                Database: sqlite3 (built-in) or psycopg2, pymysql
                """);
        }
        
        // Generic Full-Stack Template
        else if (goalLower.contains("full") && goalLower.contains("stack")) {
            template.append("""
                **GENERIC FULL-STACK APPLICATION TEMPLATE**
                
                **REQUIREMENTS:**
                - Separate backend and frontend directories
                - Database setup with proper schema
                - RESTful API with full CRUD operations
                - Frontend framework with state management
                - API integration between frontend and backend
                - Proper error handling throughout
                - Responsive design and styling
                - Build and deployment configuration
                """);
        }
        
        // Database-specific requirements
        if (hasDatabase) {
            template.append("""
                
                **DATABASE REQUIREMENTS:**
                - Proper database connection setup
                - Schema creation with appropriate tables
                - Data validation and constraints
                - Connection pooling (for production)
                - Migration scripts (if applicable)
                - Backup and recovery considerations
                """);
        }
        
        template.append("""
            
            **GENERAL QUALITY STANDARDS:**
            - Production-ready code with error handling
            - Proper project structure and organization
            - Complete configuration files
            - Documentation and comments
            - Testing setup and basic tests
            - Security considerations (CORS, validation, etc.)
            - Performance optimization
            - Code formatting and style consistency
            """);
        
        return template.toString();
    }
    
    /**
     * Print detailed failure information for step execution
     */
    private void printDetailedFailureInfo(ActionSpec actionSpec, AgentTask completedTask) {
        System.out.println("🔍 Detailed Failure Analysis:");
        System.out.println("├─ Action Type: " + actionSpec.getType());
        System.out.println("├─ Action Description: " + actionSpec.getDescription());
        
        // Print action parameters
        if (actionSpec.getParameters() != null && !actionSpec.getParameters().isEmpty()) {
            System.out.println("├─ Parameters:");
            actionSpec.getParameters().forEach((key, value) -> {
                if ("command".equals(key)) {
                    System.out.println("│  ├─ Command: '" + value + "'");
                } else if ("file_path".equals(key)) {
                    System.out.println("│  ├─ File Path: '" + value + "'");
                } else if ("working_directory".equals(key)) {
                    System.out.println("│  ├─ Working Directory: '" + value + "'");
                } else {
                    System.out.println("│  ├─ " + key + ": '" + value + "'");
                }
            });
        }
        
        // Print task status and error details
        System.out.println("├─ Task Status: " + completedTask.getStatus());
        System.out.println("├─ Error Message: " + completedTask.getErrorMessage());
        
        // Print task result details if available
        if (completedTask.getResult() != null) {
            AgentTask.TaskResult result = completedTask.getResult();
            System.out.println("├─ Exit Code: " + result.getExitCode());
            
            if (result.getOutput() != null && !result.getOutput().trim().isEmpty()) {
                System.out.println("├─ Command Output:");
                String[] outputLines = result.getOutput().trim().split("\n");
                for (int i = 0; i < outputLines.length; i++) {
                    String prefix = (i == outputLines.length - 1) ? "│  └─ " : "│  ├─ ";
                    System.out.println(prefix + outputLines[i]);
                }
            }
            
            if (result.getCommandsExecuted() != null && !result.getCommandsExecuted().isEmpty()) {
                System.out.println("├─ Commands Executed:");
                for (int i = 0; i < result.getCommandsExecuted().size(); i++) {
                    String cmd = result.getCommandsExecuted().get(i);
                    String prefix = (i == result.getCommandsExecuted().size() - 1) ? "│  └─ " : "│  ├─ ";
                    System.out.println(prefix + "'" + cmd + "'");
                }
            }
        }
        
        // Print any error details if available
        if (completedTask.getErrorMessage() != null && !completedTask.getErrorMessage().trim().isEmpty()) {
            System.out.println("├─ Error Details: " + completedTask.getErrorMessage());
        }
        
        // Print result error if different from main error message
        if (completedTask.getResult() != null && 
            completedTask.getResult().getError() != null && 
            !completedTask.getResult().getError().equals(completedTask.getErrorMessage())) {
            System.out.println("├─ Result Error: " + completedTask.getResult().getError());
        }
        
        System.out.println("└─ [End Failure Analysis]");
        System.out.println();
    }
    
    /**
     * Print detailed failure information for subtask evaluation
     */
    private void printSubtaskFailureDetails(SubTask subTask, ActionResult actionResult, String aiEvaluation) {
        System.out.println();
        System.out.println("🚨 SUBTASK FAILURE REPORT");
        System.out.println("═══════════════════════════════════════");
        System.out.println("📋 Subtask: " + subTask.getDescription());
        System.out.println("🔧 Priority: " + subTask.getPriority());
        System.out.println("⚡ Complexity: " + subTask.getComplexity());
        
        // Action result analysis
        System.out.println();
        System.out.println("🎯 Action Execution Result:");
        System.out.println("├─ Action Success: " + (actionResult.isSuccess() ? "✅ YES" : "❌ NO"));
        System.out.println("├─ Action Description: " + actionResult.getActionDescription());
        
        if (actionResult.getTaskId() != null) {
            System.out.println("├─ Task ID: " + actionResult.getTaskId());
        }
        
        // AI evaluation analysis
        System.out.println();
        System.out.println("🤖 AI Evaluation:");
        boolean aiSuccess = aiEvaluation.toLowerCase().contains("yes");
        System.out.println("├─ AI Assessment: " + (aiSuccess ? "✅ SUCCESS" : "❌ NEEDS MORE WORK"));
        System.out.println("├─ AI Response: \"" + aiEvaluation.trim() + "\"");
        
        // Combined failure analysis
        System.out.println();
        System.out.println("📊 Failure Analysis:");
        if (!actionResult.isSuccess() && !aiSuccess) {
            System.out.println("├─ Issue Type: Both action execution and AI evaluation failed");
            System.out.println("├─ Severity: HIGH - Complete task failure");
        } else if (!actionResult.isSuccess()) {
            System.out.println("├─ Issue Type: Action execution failed but AI would have approved");
            System.out.println("├─ Severity: HIGH - Technical execution problem");
        } else if (!aiSuccess) {
            System.out.println("├─ Issue Type: Action succeeded but doesn't meet requirements");
            System.out.println("├─ Severity: MEDIUM - Quality/completeness issue");
        }
        
        // Output and result details
        if (actionResult.getResult() != null) {
            System.out.println();
            System.out.println("🔍 Execution Details:");
            
            if (actionResult.getResult().getOutput() != null && !actionResult.getResult().getOutput().trim().isEmpty()) {
                System.out.println("├─ Command Output:");
                String[] outputLines = actionResult.getResult().getOutput().trim().split("\n");
                for (int i = 0; i < Math.min(10, outputLines.length); i++) { // Limit to 10 lines
                    String prefix = (i == Math.min(10, outputLines.length) - 1) ? "│  └─ " : "│  ├─ ";
                    System.out.println(prefix + outputLines[i]);
                }
                if (outputLines.length > 10) {
                    System.out.println("│  └─ ... (" + (outputLines.length - 10) + " more lines)");
                }
            }
            
            if (actionResult.getResult().getExitCode() != null && actionResult.getResult().getExitCode() != 0) {
                System.out.println("├─ Exit Code: " + actionResult.getResult().getExitCode() + " (ERROR)");
            }
        }
        
        // Suggestions for debugging
        System.out.println();
        System.out.println("💡 Debugging Suggestions:");
        if (subTask.getCommands() != null && !subTask.getCommands().isEmpty()) {
            System.out.println("├─ Try running commands manually:");
            for (String cmd : subTask.getCommands()) {
                System.out.println("│  └─ " + cmd);
            }
        }
        
        if (subTask.getFilePath() != null) {
            System.out.println("├─ Check if file path is accessible: " + subTask.getFilePath());
        }
        
        if (subTask.getDependencies() != null && !subTask.getDependencies().isEmpty()) {
            System.out.println("├─ Verify dependencies completed: " + String.join(", ", subTask.getDependencies()));
        }
        
        System.out.println("└─ Consider checking working directory and permissions");
        System.out.println();
        System.out.println("═══════════════════════════════════════");
        System.out.println();
    }
    
    /**
     * Prompt user whether to continue execution after a task failure
     */
    private boolean promptUserOnTaskFailure(SubTask failedSubTask, ReActCycleResult result) {
        System.out.println();
        System.out.println("⚠️  TASK FAILURE DETECTED");
        System.out.println("════════════════════════════════════════");
        System.out.println("📋 Failed Task: " + failedSubTask.getDescription());
        System.out.println("🔧 Priority: " + failedSubTask.getPriority());
        System.out.println("⚡ Complexity: " + failedSubTask.getComplexity());
        
        // Show failure context
        System.out.println();
        System.out.println("🎯 What happened:");
        System.out.println("├─ Action: " + result.getAction());
        System.out.println("├─ Success: ❌ FAILED");
        if (result.getObservation() != null && !result.getObservation().trim().isEmpty()) {
            System.out.println("└─ Observation: " + result.getObservation().trim());
        }
        
        // Show options
        System.out.println();
        System.out.println("🤔 What would you like to do?");
        System.out.println();
        System.out.println("1️⃣  Continue - Skip this task and proceed with remaining tasks");
        System.out.println("2️⃣  Stop - Halt execution to investigate and fix the issue");
        System.out.println("3️⃣  Retry - Attempt this task again (if you fixed the issue)");
        System.out.println();
        
        // Get user input
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        while (true) {
            System.out.print("👉 Enter your choice (1=Continue, 2=Stop, 3=Retry): ");
            try {
                String input = scanner.nextLine().trim();
                
                switch (input.toLowerCase()) {
                    case "1", "c", "continue" -> {
                        System.out.println("✅ Continuing with next task...");
                        System.out.println();
                        return true;
                    }
                    case "2", "s", "stop" -> {
                        System.out.println("🛑 Stopping execution for manual investigation...");
                        System.out.println();
                        return false;
                    }
                    case "3", "r", "retry" -> {
                        System.out.println("🔄 Retrying the failed task...");
                        System.out.println();
                        return handleTaskRetry(failedSubTask);
                    }
                    default -> {
                        System.out.println("❌ Invalid choice. Please enter 1, 2, or 3 (or 'continue', 'stop', 'retry')");
                        continue;
                    }
                }
            } catch (Exception e) {
                System.out.println("❌ Error reading input. Please try again.");
                continue;
            }
        }
    }
    
    /**
     * Handle task retry logic
     */
    private boolean handleTaskRetry(SubTask failedSubTask) {
        System.out.println("🔧 Retry Options:");
        System.out.println();
        System.out.println("1️⃣  Quick Retry - Retry the task immediately");
        System.out.println("2️⃣  Manual Fix - Wait for you to fix issues, then retry");
        System.out.println("3️⃣  Skip - Give up on this task and continue");
        System.out.println();
        
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        while (true) {
            System.out.print("👉 Retry method (1=Quick, 2=Manual Fix, 3=Skip): ");
            try {
                String input = scanner.nextLine().trim();
                
                switch (input.toLowerCase()) {
                    case "1", "q", "quick" -> {
                        System.out.println("⚡ Performing quick retry...");
                        // For now, just continue - could implement actual retry logic later
                        return true;
                    }
                    case "2", "m", "manual" -> {
                        System.out.println("🔧 Manual Fix Mode:");
                        System.out.println("├─ Please fix the issue in another terminal");
                        System.out.println("├─ Check the error details above");
                        System.out.println("├─ Verify file paths, permissions, and dependencies");
                        System.out.println("└─ Press Enter when ready to retry...");
                        System.out.print("👉 ");
                        scanner.nextLine(); // Wait for user
                        System.out.println("✅ Retrying after manual fix...");
                        return true;
                    }
                    case "3", "s", "skip" -> {
                        System.out.println("⏭️  Skipping failed task and continuing...");
                        return true;
                    }
                    default -> {
                        System.out.println("❌ Invalid choice. Please enter 1, 2, or 3");
                        continue;
                    }
                }
            } catch (Exception e) {
                System.out.println("❌ Error reading input. Please try again.");
                continue;
            }
        }
    }
}