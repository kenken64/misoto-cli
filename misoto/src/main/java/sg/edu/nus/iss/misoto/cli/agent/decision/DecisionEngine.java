package sg.edu.nus.iss.misoto.cli.agent.decision;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.misoto.cli.ai.AiClient;
import sg.edu.nus.iss.misoto.cli.agent.config.AgentConfiguration;
import sg.edu.nus.iss.misoto.cli.agent.state.AgentStateManager;
import sg.edu.nus.iss.misoto.cli.agent.decision.DecisionTypes.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Decision-making engine for the agent system
 * Uses AI to make context-aware decisions
 */
@Service
@Slf4j
public class DecisionEngine {
    
    @Autowired
    private AiClient aiClient;
    
    @Autowired
    private AgentStateManager stateManager;
    
    private AgentConfiguration config;
    private final Map<String, DecisionHistory> decisionHistory = new HashMap<>();
      /**
     * Initialize the decision engine
     */
    public void initialize(AgentConfiguration config) {
        this.config = config;
        log.info("Decision engine initialized with model: {}", config.getDecisionModel());
    }
    
    /**
     * Ensure AI client is ready for use
     */
    private void ensureAiClientReady() throws Exception {
        if (!aiClient.isReady()) {
            log.debug("AI client not ready, initializing...");
            aiClient.initialize();
        }
    }
    
    /**
     * Make a decision based on context and options
     */
    public String makeDecision(String context, String question, List<String> options) throws Exception {
        log.debug("Making decision for question: {}", question);
        
        DecisionRequest request = DecisionRequest.builder()
            .context(context)
            .question(question)
            .options(options)
            .timestamp(Instant.now())
            .agentState(stateManager.getContext())
            .build();
        
        String decision = processDecisionRequest(request);
        
        // Store decision in history
        DecisionHistory history = DecisionHistory.builder()
            .request(request)
            .decision(decision)
            .timestamp(Instant.now())
            .confidence(extractConfidence(decision))
            .build();
            
        decisionHistory.put(generateDecisionId(request), history);
        
        // Update agent state
        stateManager.setState("last_decision", decision);
        stateManager.setState("last_decision_time", Instant.now().toString());
        
        log.info("Decision made: {} (confidence: {})", decision, history.getConfidence());
        return decision;
    }
      /**
     * Make a strategic decision about agent behavior
     */
    public AgentStrategy decideStrategy(String situation, Map<String, Object> context) throws Exception {
        ensureAiClientReady();
        String prompt = buildStrategyPrompt(situation, context);
        String response = aiClient.sendMessage(prompt);
        
        AgentStrategy strategy = parseStrategyResponse(response);
        
        // Store strategy decision
        stateManager.setState("current_strategy", strategy.getType().name());
        stateManager.setState("strategy_reasoning", strategy.getReasoning());
        
        return strategy;
    }
      /**
     * Evaluate if an action should be taken
     */
    public ActionDecision shouldTakeAction(String action, String context, Map<String, Object> metadata) throws Exception {
        ensureAiClientReady();
        String prompt = buildActionEvaluationPrompt(action, context, metadata);
        String response = aiClient.sendMessage(prompt);
        
        ActionDecision decision = parseActionDecision(response);
        
        // Store action decision
        String decisionKey = "action_decision_" + action.replaceAll("\\s+", "_");
        stateManager.setState(decisionKey, decision.shouldProceed());
        stateManager.setState(decisionKey + "_reasoning", decision.getReasoning());
        
        return decision;
    }
    
    /**
     * Prioritize a list of tasks
     */
    public List<String> prioritizeTasks(List<String> tasks, String context) throws Exception {        if (tasks.isEmpty()) {
            return tasks;
        }
        
        ensureAiClientReady();
        String prompt = buildPrioritizationPrompt(tasks, context);
        String response = aiClient.sendMessage(prompt);
        
        return parsePrioritizedTasks(response, tasks);
    }
      /**
     * Decide how to handle an error or exception
     */
    public ErrorHandlingDecision decideErrorHandling(String error, String context, int retryCount) throws Exception {
        ensureAiClientReady();
        String prompt = buildErrorHandlingPrompt(error, context, retryCount);
        String response = aiClient.sendMessage(prompt);
        
        return parseErrorHandlingDecision(response);
    }
    
    /**
     * Get decision history
     */
    public Collection<DecisionHistory> getDecisionHistory() {
        return new ArrayList<>(decisionHistory.values());
    }
    
    /**
     * Get recent decisions
     */
    public List<DecisionHistory> getRecentDecisions(int limit) {
        return decisionHistory.values().stream()
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(limit)
            .collect(Collectors.toList());
    }
      private String processDecisionRequest(DecisionRequest request) throws Exception {
        ensureAiClientReady();
        String prompt = buildDecisionPrompt(request);
        return aiClient.sendMessage(prompt);
    }
    
    private String buildDecisionPrompt(DecisionRequest request) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are an AI agent making a decision. Please analyze the situation and provide a clear decision.\n\n");
        
        prompt.append("CONTEXT:\n");
        prompt.append(request.getContext()).append("\n\n");
        
        prompt.append("QUESTION:\n");
        prompt.append(request.getQuestion()).append("\n\n");
        
        if (!request.getOptions().isEmpty()) {
            prompt.append("OPTIONS:\n");
            for (int i = 0; i < request.getOptions().size(); i++) {
                prompt.append(String.format("%d. %s\n", i + 1, request.getOptions().get(i)));
            }
            prompt.append("\n");
        }
        
        // Add agent state context
        Map<String, Object> agentState = request.getAgentState();
        if (agentState != null && !agentState.isEmpty()) {
            prompt.append("AGENT STATE:\n");
            agentState.forEach((key, value) -> 
                prompt.append(String.format("- %s: %s\n", key, value)));
            prompt.append("\n");
        }
        
        // Add recent decision history for context
        List<DecisionHistory> recentDecisions = getRecentDecisions(3);
        if (!recentDecisions.isEmpty()) {
            prompt.append("RECENT DECISIONS:\n");
            recentDecisions.forEach(decision -> 
                prompt.append(String.format("- %s: %s\n", 
                    decision.getRequest().getQuestion(), decision.getDecision())));
            prompt.append("\n");
        }
        
        prompt.append("Please provide your decision with reasoning. ");
        prompt.append("Format your response as: DECISION: [your decision] REASONING: [your reasoning] CONFIDENCE: [0-100]");
        
        return prompt.toString();
    }
    
    private String buildStrategyPrompt(String situation, Map<String, Object> context) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are an AI agent deciding on the best strategy for the current situation.\n\n");
        
        prompt.append("SITUATION:\n");
        prompt.append(situation).append("\n\n");
        
        prompt.append("CONTEXT:\n");
        context.forEach((key, value) -> 
            prompt.append(String.format("- %s: %s\n", key, value)));
        prompt.append("\n");
        
        prompt.append("AVAILABLE STRATEGIES:\n");
        for (AgentStrategy.StrategyType strategy : AgentStrategy.StrategyType.values()) {
            prompt.append(String.format("- %s: %s\n", strategy.name(), getStrategyDescription(strategy)));
        }
        prompt.append("\n");
        
        prompt.append("Choose the most appropriate strategy and explain your reasoning. ");
        prompt.append("Format: STRATEGY: [strategy name] REASONING: [explanation] PRIORITY: [HIGH/MEDIUM/LOW]");
        
        return prompt.toString();
    }
    
    private String buildActionEvaluationPrompt(String action, String context, Map<String, Object> metadata) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are an AI agent evaluating whether to take a specific action.\n\n");
        
        prompt.append("PROPOSED ACTION:\n");
        prompt.append(action).append("\n\n");
        
        prompt.append("CONTEXT:\n");
        prompt.append(context).append("\n\n");
        
        if (metadata != null && !metadata.isEmpty()) {
            prompt.append("METADATA:\n");
            metadata.forEach((key, value) -> 
                prompt.append(String.format("- %s: %s\n", key, value)));
            prompt.append("\n");
        }
        
        prompt.append("Consider the risks, benefits, and potential consequences. ");
        prompt.append("Should this action be taken? ");
        prompt.append("Format: DECISION: [YES/NO] REASONING: [explanation] RISK_LEVEL: [LOW/MEDIUM/HIGH]");
        
        return prompt.toString();
    }
    
    private String buildPrioritizationPrompt(List<String> tasks, String context) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are an AI agent prioritizing tasks based on importance and urgency.\n\n");
        
        prompt.append("CONTEXT:\n");
        prompt.append(context).append("\n\n");
        
        prompt.append("TASKS TO PRIORITIZE:\n");
        for (int i = 0; i < tasks.size(); i++) {
            prompt.append(String.format("%d. %s\n", i + 1, tasks.get(i)));
        }
        prompt.append("\n");
        
        prompt.append("Rank these tasks in order of priority (most important first). ");
        prompt.append("Provide the task numbers in priority order, separated by commas.");
        
        return prompt.toString();
    }
    
    private String buildErrorHandlingPrompt(String error, String context, int retryCount) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are an AI agent deciding how to handle an error.\n\n");
        
        prompt.append("ERROR:\n");
        prompt.append(error).append("\n\n");
        
        prompt.append("CONTEXT:\n");
        prompt.append(context).append("\n\n");
        
        prompt.append("RETRY COUNT: ").append(retryCount).append("\n\n");
        
        prompt.append("How should this error be handled? ");
        prompt.append("Format: ACTION: [RETRY/ABORT/ESCALATE/MODIFY] REASONING: [explanation] DELAY: [seconds if retry]");
        
        return prompt.toString();
    }
    
    private double extractConfidence(String response) {
        try {
            String[] parts = response.split("CONFIDENCE:");
            if (parts.length > 1) {
                String confidenceStr = parts[1].trim().split("\\s+")[0];
                return Double.parseDouble(confidenceStr.replaceAll("[^0-9.]", ""));
            }
        } catch (Exception e) {
            log.debug("Could not extract confidence from response", e);
        }
        return 50.0; // Default confidence
    }
    
    private AgentStrategy parseStrategyResponse(String response) {
        AgentStrategy.StrategyType type = AgentStrategy.StrategyType.BALANCED;
        String reasoning = response;
        AgentStrategy.Priority priority = AgentStrategy.Priority.MEDIUM;
        
        try {
            if (response.contains("STRATEGY:")) {
                String strategyName = response.split("STRATEGY:")[1].split("REASONING:")[0].trim();
                type = AgentStrategy.StrategyType.valueOf(strategyName.toUpperCase());
            }
            
            if (response.contains("REASONING:")) {
                reasoning = response.split("REASONING:")[1].split("PRIORITY:")[0].trim();
            }
            
            if (response.contains("PRIORITY:")) {
                String priorityName = response.split("PRIORITY:")[1].trim().split("\\s+")[0];
                priority = AgentStrategy.Priority.valueOf(priorityName.toUpperCase());
            }
        } catch (Exception e) {
            log.warn("Error parsing strategy response, using defaults", e);
        }
        
        return AgentStrategy.builder()
            .type(type)
            .reasoning(reasoning)
            .priority(priority)
            .timestamp(Instant.now())
            .build();
    }
    
    private ActionDecision parseActionDecision(String response) {
        boolean shouldProceed = true;
        String reasoning = response;
        ActionDecision.RiskLevel riskLevel = ActionDecision.RiskLevel.MEDIUM;
        
        try {
            if (response.contains("DECISION:")) {
                String decision = response.split("DECISION:")[1].split("REASONING:")[0].trim();
                shouldProceed = decision.toUpperCase().contains("YES");
            }
            
            if (response.contains("REASONING:")) {
                reasoning = response.split("REASONING:")[1].split("RISK_LEVEL:")[0].trim();
            }
            
            if (response.contains("RISK_LEVEL:")) {
                String risk = response.split("RISK_LEVEL:")[1].trim().split("\\s+")[0];
                riskLevel = ActionDecision.RiskLevel.valueOf(risk.toUpperCase());
            }
        } catch (Exception e) {
            log.warn("Error parsing action decision, using defaults", e);
        }
        
        return ActionDecision.builder()
            .shouldProceed(shouldProceed)
            .reasoning(reasoning)
            .riskLevel(riskLevel)
            .timestamp(Instant.now())
            .build();
    }
    
    private List<String> parsePrioritizedTasks(String response, List<String> originalTasks) {
        try {
            // Extract numbers from response
            String[] parts = response.replaceAll("[^0-9,]", "").split(",");
            List<String> prioritized = new ArrayList<>();
            
            for (String part : parts) {
                int index = Integer.parseInt(part.trim()) - 1;
                if (index >= 0 && index < originalTasks.size()) {
                    prioritized.add(originalTasks.get(index));
                }
            }
            
            // Add any missing tasks at the end
            for (String task : originalTasks) {
                if (!prioritized.contains(task)) {
                    prioritized.add(task);
                }
            }
            
            return prioritized;
        } catch (Exception e) {
            log.warn("Error parsing prioritized tasks, returning original order", e);
            return originalTasks;
        }
    }
    
    // Helper method to parse error handling decision
    private ErrorHandlingDecision parseErrorHandlingDecision(String response) {
        try {
            // Simple parsing - in real implementation would use proper JSON parsing
            boolean shouldRetry = response.toLowerCase().contains("retry");
            boolean shouldStop = response.toLowerCase().contains("stop");
            long retryDelayMs = 5000; // Default
            
            ErrorHandlingDecision.ErrorAction action = ErrorHandlingDecision.ErrorAction.RETRY;
            if (response.toLowerCase().contains("skip")) {
                action = ErrorHandlingDecision.ErrorAction.SKIP;
            } else if (response.toLowerCase().contains("stop")) {
                action = ErrorHandlingDecision.ErrorAction.STOP;
            } else if (response.toLowerCase().contains("escalate")) {
                action = ErrorHandlingDecision.ErrorAction.ESCALATE;
            }
            
            return ErrorHandlingDecision.builder()
                .action(action)
                .reason("AI decision: " + response.substring(0, Math.min(100, response.length())))
                .retryDelayMs((int) retryDelayMs)
                .timestamp(Instant.now())
                .build();
                
        } catch (Exception e) {
            log.error("Error parsing error handling decision", e);
            return ErrorHandlingDecision.builder()
                .action(ErrorHandlingDecision.ErrorAction.RETRY)
                .reason("Default retry strategy")
                .retryDelayMs(5000)
                .timestamp(Instant.now())
                .build();
        }
    }
    
    private String getStrategyDescription(AgentStrategy.StrategyType strategy) {
        return switch (strategy) {
            case AGGRESSIVE -> "Take bold actions, high risk/reward";
            case CONSERVATIVE -> "Cautious approach, minimize risks";
            case BALANCED -> "Moderate approach, balance risks and benefits";
            case REACTIVE -> "Respond to events as they occur";
            case PROACTIVE -> "Anticipate and prepare for future events";
            case EXPLORATORY -> "Focus on discovery and learning";
            case MAINTENANCE -> "Focus on stability and upkeep";
        };
    }
    
    private String generateDecisionId(DecisionRequest request) {
        return UUID.nameUUIDFromBytes(
            (request.getQuestion() + request.getTimestamp()).getBytes()
        ).toString();
    }
    
    /**
     * Make a decision based on context (simplified version)
     */
    public Object makeDecision(String context) {
        try {
            List<String> defaultOptions = List.of("proceed", "wait", "abort");
            return makeDecision(context, "general", defaultOptions);
        } catch (Exception e) {
            log.error("Error making decision", e);
            return "proceed"; // Default safe decision
        }
    }
      /**
     * Handle error and provide decision
     */
    public ErrorHandlingDecision handleError(Exception error, String context) {
        try {
            ensureAiClientReady();
            String errorContext = String.format("Error: %s, Context: %s", error.getMessage(), context);
            
            String prompt = String.format("""
                An error occurred in the agent system. Please analyze and provide handling recommendation:
                
                Error: %s
                Context: %s
                
                Respond with JSON:
                {
                  "action": "RETRY|SKIP|STOP|ESCALATE",
                  "reason": "explanation",
                  "retryDelayMs": 5000,
                  "shouldRetry": true/false,
                  "shouldStop": true/false
                }
                """, error.getMessage(), context);
                
            String response = aiClient.sendMessage(prompt);
            return parseErrorHandlingDecision(response);
            
        } catch (Exception e) {
            log.error("Error in error handling decision", e);
            return ErrorHandlingDecision.builder()
                .action(ErrorHandlingDecision.ErrorAction.RETRY)
                .reason("Default retry strategy")
                .retryDelayMs(5000)
                .timestamp(Instant.now())
                .build();
        }
    }
}
