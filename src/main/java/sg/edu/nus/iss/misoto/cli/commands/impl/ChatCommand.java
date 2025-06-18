package sg.edu.nus.iss.misoto.cli.commands.impl;

import sg.edu.nus.iss.misoto.cli.commands.Command;
import sg.edu.nus.iss.misoto.cli.ai.AiClient;
import sg.edu.nus.iss.misoto.cli.errors.UserError;
import sg.edu.nus.iss.misoto.cli.terminal.Terminal;
import sg.edu.nus.iss.misoto.cli.terminal.TerminalInterface;
import sg.edu.nus.iss.misoto.cli.utils.FormattingUtil;
import sg.edu.nus.iss.misoto.cli.utils.ValidationUtil;
import sg.edu.nus.iss.misoto.cli.agent.AgentService;
import sg.edu.nus.iss.misoto.cli.agent.config.AgentConfiguration;
import sg.edu.nus.iss.misoto.cli.agent.task.AgentTask;
import sg.edu.nus.iss.misoto.cli.agent.task.TaskQueueService;
import sg.edu.nus.iss.misoto.cli.agent.task.TaskQueueStats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.Instant;

/**
 * Interactive chat command for conversing with Claude AI
 */
@Component
@Slf4j
public class ChatCommand implements Command {
    
    @Autowired
    private AiClient aiClient;
      @Autowired
    private Terminal terminal;
    
    @Autowired(required = false)
    private AgentService agentService;
    
    @Autowired(required = false) 
    private AgentConfiguration agentConfig;
    
    @Autowired(required = false)
    private TaskQueueService taskQueue;
    
    @Autowired(required = false)
    private sg.edu.nus.iss.misoto.cli.agent.planning.PlanningService planningService;
    
    private static final String SYSTEM_PROMPT = "You are Claude, a helpful AI assistant. You're having a conversational chat with a user. Be conversational, helpful, and engaging. Keep your responses clear and concise unless the user asks for detailed explanations.";    private static final String CHAT_WELCOME = """
        %s╭─────────────────────────────────────────────────────────╮%s
        %s│                 💬 Interactive Chat Mode                │%s
        %s╰─────────────────────────────────────────────────────────╯%s
        
        %sWelcome to Claude Chat!%s
        
        • Type your messages and press Enter to chat
        • Use %s/help%s to see chat commands
        • Use %s/exit%s or %s/quit%s to end the chat session
        • Use %s/clear%s to clear the conversation history
        • Use %s/model%s to see current AI model information
        • Use %s/usage%s to see token usage and cost information
        • Use %s/agent%s to toggle agent mode (autonomous assistance)
        
        %sStart chatting below:%s
        """;
    
    // Session tracking variables
    private int sessionTotalInputTokens = 0;
    private int sessionTotalOutputTokens = 0;
    private double sessionTotalCost = 0.0;
    private int messageCount = 0;
    
    @Override
    public String getName() {
        return "chat";
    }
    
    @Override
    public String getDescription() {
        return "Start an interactive chat session with Claude AI";
    }
    
    @Override
    public String getCategory() {
        return "AI Assistance";
    }
    
    @Override
    public boolean isHidden() {
        return false;
    }
    
    @Override
    public boolean requiresAuth() {
        return true;
    }
    
    @Override
    public String getUsage() {
        return "claude-code chat";
    }
    
    @Override
    public List<String> getExamples() {
        return List.of(
            "claude-code chat",
            "claude-code chat  # Start interactive chat session"
        );
    }
    
    @Override
    public void execute(List<String> args) throws Exception {
        // Chat command doesn't need arguments, but we can allow optional initial message
        String initialMessage = args.isEmpty() ? null : String.join(" ", args);
        
        try {
            startChatSession(initialMessage);
        } catch (Exception e) {
            log.error("Failed to start chat session", e);
            throw new UserError("Failed to start chat session: " + e.getMessage());
        }
    }    private void startChatSession(String initialMessage) throws IOException {
        // Clear screen before starting chat
        clearScreen();
        
        // Temporarily pause agent service to prevent API calls during chat
        boolean agentWasRunning = false;
        if (agentService != null && agentService.isRunning()) {
            agentWasRunning = true;
            showAgentShutdownProgress("Pausing agent for chat");
            agentService.stopAgent();
            log.info("Temporarily stopped agent service for chat session");
        }
        
        List<ChatMessage> conversationHistory = new ArrayList<>();
        BufferedReader reader = null;
        final boolean finalAgentWasRunning = agentWasRunning;
        
        try {
            reader = new BufferedReader(new InputStreamReader(System.in));
            
            // Reset session tracking
            sessionTotalInputTokens = 0;
            sessionTotalOutputTokens = 0;
            sessionTotalCost = 0.0;
            messageCount = 0;
        
        // Display welcome message
        displayWelcomeMessage();
          // Process initial message if provided
        if (initialMessage != null && !initialMessage.trim().isEmpty()) {
            System.out.println(FormattingUtil.formatWithColor("You: " + initialMessage, FormattingUtil.ANSI_CYAN + FormattingUtil.ANSI_BOLD));
            processUserMessage(initialMessage, conversationHistory);
        }
              // Main chat loop
            while (true) {
                try {
                    // Show prompt at bottom
                    System.out.print(FormattingUtil.formatWithColor("\n" + "─".repeat(60) + "\n", FormattingUtil.ANSI_GRAY));
                    System.out.print(FormattingUtil.formatWithColor("You: ", FormattingUtil.ANSI_CYAN + FormattingUtil.ANSI_BOLD));
                    String userInput = reader.readLine();
                      // Check for null input (Ctrl+C or EOF)
                    if (userInput == null) {
                        performCleanup(); // Perform cleanup before exit
                        displaySessionSummary();
                        System.out.println("\n" + FormattingUtil.formatWithColor("Chat session ended.", FormattingUtil.ANSI_YELLOW));
                        break;
                    }
                    
                    userInput = userInput.trim();
                    
                    // Skip empty input
                    if (userInput.isEmpty()) {
                        continue;
                    }
                      // Handle chat commands
                    if (userInput.startsWith("/")) {
                        if (handleChatCommand(userInput, conversationHistory)) {
                            displaySessionSummary();
                            break; // Exit chat if command returns true
                        }
                        continue;
                    }
                    
                    // Process user message (this will show Claude's response above)
                    processUserMessage(userInput, conversationHistory);
                    
                } catch (IOException e) {
                    // Check if this is due to stream being closed during shutdown
                    if (e.getMessage() != null && e.getMessage().contains("Stream closed")) {
                        log.info("Chat session terminated due to application shutdown");
                        System.out.println("\n" + FormattingUtil.formatWithColor("Chat session ended.", FormattingUtil.ANSI_YELLOW));
                        break;
                    } else {
                        log.error("Error reading user input in chat", e);
                        System.err.println(FormattingUtil.formatWithColor("Error reading input. Ending chat session.", FormattingUtil.ANSI_RED));
                        break;
                    }
                } catch (Exception e) {
                    log.error("Error processing chat message", e);
                    System.err.println(FormattingUtil.formatWithColor("Error: " + e.getMessage(), FormattingUtil.ANSI_RED));
                    // Continue the chat loop even after errors
                }
            }        } catch (Exception e) {
            log.error("Failed to initialize chat session", e);
            throw new IOException("Failed to start chat session", e);
        } finally {
            // Don't close System.in as it's shared across the application
            reader = null;
            // Perform cleanup operations
            performCleanup();
            
            // Restart agent service if it was running before
            if (finalAgentWasRunning && agentService != null) {
                try {
                    agentService.startAgent();
                    log.info("Restarted agent service after chat session");
                } catch (Exception e) {
                    log.error("Failed to restart agent service", e);
                }
            }
        }
    }
    
    private void displayWelcomeMessage() {
        String blue = FormattingUtil.ANSI_BLUE;
        String cyan = FormattingUtil.ANSI_CYAN;
        String green = FormattingUtil.ANSI_GREEN;
        String yellow = FormattingUtil.ANSI_YELLOW;
        String bold = FormattingUtil.ANSI_BOLD;
        String reset = FormattingUtil.ANSI_RESET;        System.out.printf(CHAT_WELCOME,
            blue, reset,           // Box top
            blue, reset,           // Box middle
            blue, reset,           // Box bottom
            cyan + bold, reset,    // Welcome message
            green, reset,          // /help
            green, reset,          // /exit
            green, reset,          // /quit
            green, reset,          // /clear
            green, reset,          // /model
            green, reset,          // /usage
            green, reset,          // /agent
            yellow + bold, reset   // Start chatting message
        );
    }
    
    private boolean handleChatCommand(String command, List<ChatMessage> conversationHistory) {
        String cmd = command.toLowerCase();
        
        switch (cmd) {            case "/exit":
            case "/quit":
                System.out.println(FormattingUtil.formatWithColor("\n👋 Thanks for chatting! Goodbye!", FormattingUtil.ANSI_GREEN));
                performCleanup(); // Perform immediate cleanup
                return true;
                
            case "/help":
                displayChatHelp();
                return false;
                  case "/clear":
                conversationHistory.clear();
                // Reset session tracking when clearing history
                sessionTotalInputTokens = 0;
                sessionTotalOutputTokens = 0;
                sessionTotalCost = 0.0;
                messageCount = 0;
                System.out.println(FormattingUtil.formatWithColor("\n🧹 Conversation history cleared.", FormattingUtil.ANSI_YELLOW));
                return false;
                
            case "/model":
                try {
                    String modelInfo = aiClient.getModelInfo();
                    System.out.println(FormattingUtil.formatWithColor("\n🤖 " + modelInfo, FormattingUtil.ANSI_BLUE));
                } catch (Exception e) {
                    System.out.println(FormattingUtil.formatWithColor("\n❌ Error getting model info: " + e.getMessage(), FormattingUtil.ANSI_RED));
                }
                return false;
                  case "/history":
                displayConversationHistory(conversationHistory);
                return false;
                  case "/usage":
            case "/cost":
                displayUsageInfo();
                return false;
                
            case "/agent":
                handleAgentCommand(conversationHistory);
                return false;
                  default:
                System.out.println(FormattingUtil.formatWithColor("\n❓ Unknown command: " + command, FormattingUtil.ANSI_RED));
                System.out.println(FormattingUtil.formatWithColor("   Use /help to see available commands.", FormattingUtil.ANSI_GRAY));
                return false;
        }
    }
    
    private void displayChatHelp() {
        String help = """
            
            %s📋 Chat Commands:%s
              %s/help%s     - Show this help message
            %s/exit%s     - Exit the chat session
            %s/quit%s     - Exit the chat session
            %s/clear%s    - Clear conversation history            %s/model%s    - Show current AI model information
            %s/usage%s    - Show token usage and cost information
            %s/agent%s    - Toggle agent mode or submit agent tasks
            %s/history%s  - Show conversation history
            
            %sTips:%s
            • Just type normally to chat with Claude
            • Press Ctrl+C to force exit
            • Your conversation history is maintained during the session
            """;
              System.out.printf(help,
            FormattingUtil.ANSI_CYAN + FormattingUtil.ANSI_BOLD, FormattingUtil.ANSI_RESET,
            FormattingUtil.ANSI_GREEN, FormattingUtil.ANSI_RESET,            FormattingUtil.ANSI_GREEN, FormattingUtil.ANSI_RESET,
            FormattingUtil.ANSI_GREEN, FormattingUtil.ANSI_RESET,
            FormattingUtil.ANSI_GREEN, FormattingUtil.ANSI_RESET,
            FormattingUtil.ANSI_GREEN, FormattingUtil.ANSI_RESET,
            FormattingUtil.ANSI_GREEN, FormattingUtil.ANSI_RESET,
            FormattingUtil.ANSI_GREEN, FormattingUtil.ANSI_RESET,
            FormattingUtil.ANSI_GREEN, FormattingUtil.ANSI_RESET,
            FormattingUtil.ANSI_YELLOW + FormattingUtil.ANSI_BOLD, FormattingUtil.ANSI_RESET
        );
    }
    
    private void displayConversationHistory(List<ChatMessage> conversationHistory) {
        if (conversationHistory.isEmpty()) {
            System.out.println(FormattingUtil.formatWithColor("📝 No conversation history yet.", FormattingUtil.ANSI_GRAY));
            return;
        }
        
        System.out.println(FormattingUtil.formatWithColor("\n📝 Conversation History:", FormattingUtil.ANSI_CYAN + FormattingUtil.ANSI_BOLD));
        
        for (int i = 0; i < conversationHistory.size(); i++) {
            ChatMessage msg = conversationHistory.get(i);
            String role = msg.isUser() ? "You" : "Claude";
            String color = msg.isUser() ? FormattingUtil.ANSI_CYAN : FormattingUtil.ANSI_GREEN;
            
            System.out.println(FormattingUtil.formatWithColor(
                String.format("\n[%d] %s: %s", i + 1, role, 
                    FormattingUtil.truncate(msg.getContent(), 100)), 
                color
            ));
        }
        System.out.println();
    }    private void processUserMessage(String userInput, List<ChatMessage> conversationHistory) {
        try {
            // Add user message to history
            conversationHistory.add(new ChatMessage(true, userInput));
            
            // Show thinking indicator above the input
            System.out.println(FormattingUtil.formatWithColor("🤔 Claude is thinking...", FormattingUtil.ANSI_YELLOW));
            System.out.println(); // Empty line for better spacing
            
            // Build conversation context for Claude
            StringBuilder conversationContext = new StringBuilder();
            
            // Add recent conversation history (last 10 messages to avoid token limits)
            int startIndex = Math.max(0, conversationHistory.size() - 10);
            for (int i = startIndex; i < conversationHistory.size() - 1; i++) { // -1 to exclude the current message
                ChatMessage msg = conversationHistory.get(i);
                conversationContext.append(msg.isUser() ? "Human: " : "Assistant: ")
                                 .append(msg.getContent())
                                 .append("\n\n");
            }
            
            // Add the current user message
            conversationContext.append("Human: ").append(userInput);
            
            // Track timing
            long startTime = System.currentTimeMillis();
            
            // Get response from Claude with usage information
            sg.edu.nus.iss.misoto.cli.ai.provider.AiResponse aiResponse = aiClient.sendMessageWithUsage(SYSTEM_PROMPT, conversationContext.toString());
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Add Claude's response to history
            conversationHistory.add(new ChatMessage(false, aiResponse.getText()));
            
            // Update session tracking
            messageCount++;
            if (aiResponse.getUsage() != null) {
                if (aiResponse.getUsage().getInputTokens() != null) {
                    sessionTotalInputTokens += aiResponse.getUsage().getInputTokens();
                }
                if (aiResponse.getUsage().getOutputTokens() != null) {
                    sessionTotalOutputTokens += aiResponse.getUsage().getOutputTokens();
                }
                if (aiResponse.getUsage().getEstimatedCost() != null) {
                    sessionTotalCost += aiResponse.getUsage().getEstimatedCost();
                }
            }
            
            // Display Claude's response with clear formatting
            System.out.println(FormattingUtil.formatWithColor("Claude:", FormattingUtil.ANSI_GREEN + FormattingUtil.ANSI_BOLD));
            System.out.println(FormattingUtil.formatWithColor("─".repeat(60), FormattingUtil.ANSI_GREEN));
            System.out.println(aiResponse.getText());
            System.out.println(); // Empty line for spacing
            
            // Show timing and usage information
            String formattedDuration = FormattingUtil.formatDuration(duration);
            System.out.println(FormattingUtil.formatWithColor("⏱️ Response time: " + formattedDuration, FormattingUtil.ANSI_GRAY));
            
            // Display token usage and cost
            if (aiResponse.getUsage() != null && aiResponse.getUsage().getInputTokens() != null && aiResponse.getUsage().getOutputTokens() != null) {
                String usageInfo = String.format("📊 Tokens: %d in + %d out = %d total", 
                    aiResponse.getUsage().getInputTokens(), aiResponse.getUsage().getOutputTokens(), aiResponse.getUsage().getTotalTokens());
                System.out.println(FormattingUtil.formatWithColor(usageInfo, FormattingUtil.ANSI_BLUE));
                if (aiResponse.getUsage().getEstimatedCost() != null) {
                    String costInfo = String.format("💰 Estimated cost: $%.6f", aiResponse.getUsage().getEstimatedCost());
                    System.out.println(FormattingUtil.formatWithColor(costInfo, FormattingUtil.ANSI_PURPLE));
                }
            }
            
        } catch (Exception e) {
            log.error("Error getting Claude response", e);
            System.err.println(FormattingUtil.formatWithColor("❌ Error getting response from Claude: " + e.getMessage(), FormattingUtil.ANSI_RED));
            
            // Remove the user message from history if we failed to get a response
            if (!conversationHistory.isEmpty() && conversationHistory.get(conversationHistory.size() - 1).isUser()) {
                conversationHistory.remove(conversationHistory.size() - 1);
            }
        }
    }
    
    private void displayUsageInfo() {
        System.out.println(FormattingUtil.formatWithColor("\n📊 Token Usage Information:", FormattingUtil.ANSI_CYAN + FormattingUtil.ANSI_BOLD));
        System.out.println(FormattingUtil.formatWithColor("─".repeat(40), FormattingUtil.ANSI_CYAN));
        
        if (messageCount == 0) {
            System.out.println(FormattingUtil.formatWithColor("No messages sent yet.", FormattingUtil.ANSI_GRAY));
            return;
        }
        
        System.out.println(FormattingUtil.formatWithColor(String.format("Messages in session: %d", messageCount), FormattingUtil.ANSI_WHITE));
        System.out.println(FormattingUtil.formatWithColor(String.format("Input tokens: %,d", sessionTotalInputTokens), FormattingUtil.ANSI_BLUE));
        System.out.println(FormattingUtil.formatWithColor(String.format("Output tokens: %,d", sessionTotalOutputTokens), FormattingUtil.ANSI_GREEN));
        System.out.println(FormattingUtil.formatWithColor(String.format("Total tokens: %,d", sessionTotalInputTokens + sessionTotalOutputTokens), FormattingUtil.ANSI_YELLOW));
        
        if (sessionTotalCost > 0) {
            System.out.println(FormattingUtil.formatWithColor(String.format("Estimated cost: $%.6f", sessionTotalCost), FormattingUtil.ANSI_PURPLE));
            
            if (messageCount > 1) {
                double avgCostPerMessage = sessionTotalCost / messageCount;
                System.out.println(FormattingUtil.formatWithColor(String.format("Average cost per message: $%.6f", avgCostPerMessage), FormattingUtil.ANSI_GRAY));
            }
        }
        System.out.println();
    }
    
    private void displaySessionSummary() {
        // Show progress indicator for exiting chat
        if (agentService != null) {
            showAgentShutdownProgress("exiting");
        }
        
        if (messageCount > 0) {
            System.out.println(FormattingUtil.formatWithColor("\n🎯 Session Summary:", FormattingUtil.ANSI_CYAN + FormattingUtil.ANSI_BOLD));
            System.out.println(FormattingUtil.formatWithColor("─".repeat(40), FormattingUtil.ANSI_CYAN));
            System.out.println(FormattingUtil.formatWithColor(String.format("Messages exchanged: %d", messageCount), FormattingUtil.ANSI_WHITE));
            System.out.println(FormattingUtil.formatWithColor(String.format("Total tokens used: %,d", sessionTotalInputTokens + sessionTotalOutputTokens), FormattingUtil.ANSI_YELLOW));
            
            if (sessionTotalCost > 0) {
                System.out.println(FormattingUtil.formatWithColor(String.format("Total estimated cost: $%.6f", sessionTotalCost), FormattingUtil.ANSI_PURPLE));
            }
        }
    }
    
    /**
     * Perform cleanup operations when chat session ends
     */
    private void performCleanup() {
        try {
            // Clear any temporary resources
            log.debug("Performing chat session cleanup");
            
            // If agent service is available, perform any necessary cleanup
            if (agentService != null) {
                // No specific cleanup needed for agent service in chat mode
                log.debug("Agent service cleanup check completed");
            }
            
            // If AI client needs cleanup
            if (aiClient != null) {
                // AI client cleanup is handled by the client itself
                log.debug("AI client cleanup check completed");
            }
            
            // Force garbage collection to help with memory cleanup
            System.gc();
            
            log.debug("Chat session cleanup completed successfully");
            
        } catch (Exception e) {
            log.warn("Error during chat session cleanup: {}", e.getMessage());
            // Don't throw exception during cleanup to avoid blocking exit
        }
    }

    /**
     * Simple chat message container
     */
    private static class ChatMessage {
        private final boolean isUser;
        private final String content;
        
        public ChatMessage(boolean isUser, String content) {
            this.isUser = isUser;
            this.content = content;
        }
        
        public boolean isUser() {
            return isUser;
        }
        
        public String getContent() {
            return content;
        }
    }
    
    private void handleAgentCommand(List<ChatMessage> conversationHistory) {
        if (agentService == null || agentConfig == null) {
            System.out.println(FormattingUtil.formatWithColor(
                "\n❌ Agent system is not available. Enable it in configuration and restart.", 
                FormattingUtil.ANSI_RED));
            return;
        }
        
        boolean isAgentRunning = agentService.isRunning();
        
        System.out.println(FormattingUtil.formatWithColor("\n🤖 Agent Mode Control", FormattingUtil.ANSI_CYAN + FormattingUtil.ANSI_BOLD));
        System.out.println(FormattingUtil.formatWithColor("─".repeat(40), FormattingUtil.ANSI_CYAN));
        
        System.out.println(FormattingUtil.formatWithColor(
            String.format("Status: %s", isAgentRunning ? "🟢 Running" : "🔴 Stopped"), 
            FormattingUtil.ANSI_WHITE));
            
        if (agentConfig.isAgentModeEnabled()) {
            System.out.println(FormattingUtil.formatWithColor("Configuration: ✅ Enabled", FormattingUtil.ANSI_GREEN));
        } else {
            System.out.println(FormattingUtil.formatWithColor("Configuration: ❌ Disabled", FormattingUtil.ANSI_RED));
        }
        
        System.out.println(FormattingUtil.formatWithColor(
            String.format("Mode: %s", agentConfig.getMode().name()), 
            FormattingUtil.ANSI_BLUE));        System.out.println(FormattingUtil.formatWithColor("\nAgent Commands:", FormattingUtil.ANSI_YELLOW));
        System.out.println("• Type 'start' to start the agent");
        System.out.println("• Type 'stop' to stop the agent");
        System.out.println("• Type 'status' to see detailed agent status");
        System.out.println("• Type 'task <description>' to submit a task to the agent");
        System.out.println("• Type 'tasks [status]' to list all tasks (optional filter: ALL, PENDING, COMPLETED, FAILED)");
        System.out.println("• Type 'plan <goal>' to create and execute a ReAct-based plan");
        System.out.println("• Type 'plans' to list active plans");
        System.out.println("• Type 'mode <INTERACTIVE|AUTONOMOUS|SUPERVISED|MANUAL>' to change mode");
        System.out.println("• Type 'exit' or 'back' to return to chat");
        System.out.println("• Press Enter without typing anything to return to chat");
        
        BufferedReader agentReader = null;
        try {
            agentReader = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                System.out.print(FormattingUtil.formatWithColor("\nAgent> ", FormattingUtil.ANSI_PURPLE + FormattingUtil.ANSI_BOLD));
                String agentInput = agentReader.readLine();
                
                if (agentInput == null || agentInput.trim().isEmpty()) {
                    System.out.println(FormattingUtil.formatWithColor("\n↩️ Returning to chat mode...", FormattingUtil.ANSI_GRAY));
                    return;
                }
                
                if (!processAgentCommand(agentInput.trim(), conversationHistory)) {
                    System.out.println(FormattingUtil.formatWithColor("\n↩️ Returning to chat mode...", FormattingUtil.ANSI_GRAY));
                    return;
                }
            }
            
        } catch (IOException e) {
            log.error("Error reading agent command", e);
            System.err.println(FormattingUtil.formatWithColor("Error reading command", FormattingUtil.ANSI_RED));
        } finally {
            // Don't close the reader as it wraps System.in which is shared
            agentReader = null;
        }
    }
      private boolean processAgentCommand(String command, List<ChatMessage> conversationHistory) {
        String[] parts = command.split("\\s+", 2);
        String action = parts[0].toLowerCase();
        
        switch (action) {
            case "exit":
            case "back":
            case "return":
                return false; // Signal to exit agent mode
                
            case "start":
                if (agentService.isRunning()) {
                    System.out.println(FormattingUtil.formatWithColor("\n🤖 Agent is already running", FormattingUtil.ANSI_YELLOW));
                } else {
                    try {
                        agentService.startAgent();
                        System.out.println(FormattingUtil.formatWithColor("\n🚀 Agent started successfully!", FormattingUtil.ANSI_GREEN));
                    } catch (Exception e) {
                        System.out.println(FormattingUtil.formatWithColor("\n❌ Failed to start agent: " + e.getMessage(), FormattingUtil.ANSI_RED));
                    }                }
                break;
                
            case "stop":
                if (!agentService.isRunning()) {
                    System.out.println(FormattingUtil.formatWithColor("\n🤖 Agent is not running", FormattingUtil.ANSI_YELLOW));
                } else {
                    try {
                        agentService.stopAgent();
                        System.out.println(FormattingUtil.formatWithColor("\n🛑 Agent stopped successfully!", FormattingUtil.ANSI_GREEN));
                    } catch (Exception e) {
                        System.out.println(FormattingUtil.formatWithColor("\n❌ Failed to stop agent: " + e.getMessage(), FormattingUtil.ANSI_RED));
                    }
                }
                break;
                
            case "status":
                displayAgentStatus();
                break;
                
            case "task":
                if (parts.length < 2) {
                    System.out.println(FormattingUtil.formatWithColor("\n❌ Please provide a task description", FormattingUtil.ANSI_RED));
                } else {
                    submitAgentTask(parts[1], conversationHistory);
                }
                break;
                
            case "tasks":
            case "list":
                String statusFilter = parts.length > 1 ? parts[1].toUpperCase() : "ALL";
                displayTaskList(statusFilter);
                break;
                
            case "mode":
                if (parts.length < 2) {
                    System.out.println(FormattingUtil.formatWithColor("\n❌ Please specify a mode: INTERACTIVE, AUTONOMOUS, SUPERVISED, or MANUAL", FormattingUtil.ANSI_RED));
                } else {
                    changeAgentMode(parts[1]);
                }
                break;
                
            case "plan":
                if (parts.length < 2) {
                    System.out.println(FormattingUtil.formatWithColor("\n❌ Please provide a goal for the plan", FormattingUtil.ANSI_RED));
                } else {
                    createAndExecutePlan(parts[1], conversationHistory);
                }
                break;
                
            case "plans":
                displayActivePlans();
                break;
                
            default:
                System.out.println(FormattingUtil.formatWithColor("\n❓ Unknown agent command: " + action, FormattingUtil.ANSI_RED));
                System.out.println(FormattingUtil.formatWithColor("   Type 'exit' to return to chat or try: start, stop, status, task, tasks, plan, plans, mode", FormattingUtil.ANSI_GRAY));
                break;
        }
        return true; // Continue in agent mode
    }
    
    private void displayTaskList(String statusFilter) {
        if (taskQueue == null) {
            System.out.println(FormattingUtil.formatWithColor("\n❌ Task queue is not available", FormattingUtil.ANSI_RED));
            return;
        }
        
        try {
            Collection<AgentTask> allTasks = taskQueue.getAllTasks();
            
            // Debug: Show all task statuses
            System.out.println(FormattingUtil.formatWithColor(
                "\n🔍 Debug: All tasks in system (" + allTasks.size() + " total):", 
                FormattingUtil.ANSI_BLUE));
            for (AgentTask task : allTasks) {
                System.out.println(FormattingUtil.formatWithColor(
                    "  • " + task.getId().substring(0, Math.min(8, task.getId().length())) + 
                    " [" + task.getStatus() + "] " + task.getType(), 
                    FormattingUtil.ANSI_GRAY));
            }
            
            if (allTasks.isEmpty()) {
                System.out.println(FormattingUtil.formatWithColor("\n📋 No tasks found", FormattingUtil.ANSI_GRAY));
                return;
            }
            
            // Filter tasks based on status
            List<AgentTask> filteredTasks = allTasks.stream()
                .filter(task -> {
                    if ("ALL".equals(statusFilter)) {
                        return true;
                    } else if ("PENDING".equals(statusFilter)) {
                        return task.getStatus() == AgentTask.TaskStatus.PENDING || 
                               task.getStatus() == AgentTask.TaskStatus.QUEUED;
                    } else if ("COMPLETED".equals(statusFilter)) {
                        return task.getStatus() == AgentTask.TaskStatus.COMPLETED;
                    } else if ("FAILED".equals(statusFilter)) {
                        return task.getStatus() == AgentTask.TaskStatus.FAILED;
                    } else if ("RUNNING".equals(statusFilter)) {
                        return task.getStatus() == AgentTask.TaskStatus.RUNNING;
                    } else {
                        // Try to match exact status
                        try {
                            AgentTask.TaskStatus filterStatus = AgentTask.TaskStatus.valueOf(statusFilter);
                            return task.getStatus() == filterStatus;
                        } catch (IllegalArgumentException e) {
                            return true; // Invalid filter, show all
                        }
                    }
                })
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())) // Most recent first
                .collect(Collectors.toList());
            
            if (filteredTasks.isEmpty()) {
                System.out.println(FormattingUtil.formatWithColor(
                    String.format("\n📋 No tasks found with status: %s", statusFilter), 
                    FormattingUtil.ANSI_GRAY));
                return;
            }
            
            // Display header
            System.out.println(FormattingUtil.formatWithColor(
                String.format("\n📋 Task List (%s) - %d task(s)", statusFilter, filteredTasks.size()), 
                FormattingUtil.ANSI_CYAN + FormattingUtil.ANSI_BOLD));
            System.out.println(FormattingUtil.formatWithColor("─".repeat(80), FormattingUtil.ANSI_CYAN));
            
            // Display tasks
            for (int i = 0; i < filteredTasks.size(); i++) {
                AgentTask task = filteredTasks.get(i);
                displayTaskSummary(task, i + 1);
                
                if (i < filteredTasks.size() - 1) {
                    System.out.println(FormattingUtil.formatWithColor("  " + "─".repeat(76), FormattingUtil.ANSI_GRAY));
                }
            }
            
            // Display statistics
            System.out.println(FormattingUtil.formatWithColor("\n📊 Task Statistics:", FormattingUtil.ANSI_BLUE));
            TaskQueueStats stats = taskQueue.getStatistics();
            System.out.println(String.format("  Total: %d | Pending: %d | Running: %d | Completed: %d | Failed: %d",
                stats.getTotalTasks(),
                stats.getPendingTasks(),
                stats.getRunningTasks(),
                stats.getCompletedTasks(),
                stats.getFailedTasks()));
            
        } catch (Exception e) {
            System.out.println(FormattingUtil.formatWithColor(
                "\n❌ Error retrieving task list: " + e.getMessage(), 
                FormattingUtil.ANSI_RED));
            log.error("Error displaying task list", e);
        }
    }
    
    private void displayTaskSummary(AgentTask task, int index) {
        // Status with color coding
        String statusColor = getStatusColor(task.getStatus());
        String statusText = String.format("[%s]", task.getStatus().name());
        
        // Priority with color coding
        String priorityColor = getPriorityColor(task.getPriority());
        String priorityText = task.getPriority().name();
        
        // Task header line
        System.out.println(String.format("  %s%d.%s %s%s%s %s(%s)%s %s%s%s",
            FormattingUtil.ANSI_WHITE + FormattingUtil.ANSI_BOLD,
            index,
            FormattingUtil.ANSI_RESET,
            statusColor,
            statusText,
            FormattingUtil.ANSI_RESET,
            priorityColor,
            priorityText,
            FormattingUtil.ANSI_RESET,
            FormattingUtil.ANSI_CYAN,
            task.getType().name(),
            FormattingUtil.ANSI_RESET));
        
        // Task name and description
        System.out.println(String.format("     %sName:%s %s", 
            FormattingUtil.ANSI_BOLD, FormattingUtil.ANSI_RESET, 
            task.getName() != null ? task.getName() : "Unnamed Task"));
        
        if (task.getDescription() != null && !task.getDescription().trim().isEmpty()) {
            String description = task.getDescription().length() > 100 ? 
                task.getDescription().substring(0, 97) + "..." : task.getDescription();
            System.out.println(String.format("     %sDesc:%s %s", 
                FormattingUtil.ANSI_BOLD, FormattingUtil.ANSI_RESET, description));
        }
        
        // Task ID and timing
        System.out.println(String.format("     %sID:%s %s | %sCreated:%s %s",
            FormattingUtil.ANSI_GRAY, FormattingUtil.ANSI_RESET,
            task.getId().substring(0, Math.min(8, task.getId().length())),
            FormattingUtil.ANSI_GRAY, FormattingUtil.ANSI_RESET,
            formatInstant(task.getCreatedAt())));
        
        // Additional timing info based on status
        if (task.getStartedAt() != null) {
            System.out.println(String.format("     %sStarted:%s %s",
                FormattingUtil.ANSI_GRAY, FormattingUtil.ANSI_RESET,
                formatInstant(task.getStartedAt())));
        }
        
        if (task.getCompletedAt() != null) {
            System.out.println(String.format("     %sCompleted:%s %s",
                FormattingUtil.ANSI_GRAY, FormattingUtil.ANSI_RESET,
                formatInstant(task.getCompletedAt())));
        }
        
        // Error message for failed tasks
        if (task.getStatus() == AgentTask.TaskStatus.FAILED && task.getErrorMessage() != null) {
            String errorMsg = task.getErrorMessage().length() > 80 ? 
                task.getErrorMessage().substring(0, 77) + "..." : task.getErrorMessage();
            System.out.println(String.format("     %sError:%s %s%s%s",
                FormattingUtil.ANSI_RED + FormattingUtil.ANSI_BOLD, FormattingUtil.ANSI_RESET,
                FormattingUtil.ANSI_RED, errorMsg, FormattingUtil.ANSI_RESET));
        }
        
        // Task result summary for completed tasks
        if (task.getStatus() == AgentTask.TaskStatus.COMPLETED && task.getResult() != null) {
            AgentTask.TaskResult result = task.getResult();
            if (result.getOutput() != null && !result.getOutput().trim().isEmpty()) {
                String output = result.getOutput().length() > 80 ? 
                    result.getOutput().substring(0, 77) + "..." : result.getOutput();
                // Replace newlines with spaces for summary display
                output = output.replace("\n", " ").replace("\r", " ");
                System.out.println(String.format("     %sOutput:%s %s%s%s",
                    FormattingUtil.ANSI_GREEN + FormattingUtil.ANSI_BOLD, FormattingUtil.ANSI_RESET,
                    FormattingUtil.ANSI_GREEN, output, FormattingUtil.ANSI_RESET));
            }
            
            // Show files created/modified
            if (result.getFilesCreated() != null && !result.getFilesCreated().isEmpty()) {
                System.out.println(String.format("     %sFiles Created:%s %s",
                    FormattingUtil.ANSI_BLUE, FormattingUtil.ANSI_RESET,
                    String.join(", ", result.getFilesCreated())));
            }
            
            if (result.getCommandsExecuted() != null && !result.getCommandsExecuted().isEmpty()) {
                System.out.println(String.format("     %sCommands:%s %s",
                    FormattingUtil.ANSI_PURPLE, FormattingUtil.ANSI_RESET,
                    String.join(", ", result.getCommandsExecuted())));
            }
        }
    }
    
    private String getStatusColor(AgentTask.TaskStatus status) {
        return switch (status) {
            case PENDING, QUEUED, WAITING_FOR_DEPENDENCIES -> FormattingUtil.ANSI_YELLOW;
            case RUNNING -> FormattingUtil.ANSI_BLUE;
            case COMPLETED -> FormattingUtil.ANSI_GREEN;
            case FAILED, TIMEOUT -> FormattingUtil.ANSI_RED;
            case CANCELLED -> FormattingUtil.ANSI_GRAY;
            case PAUSED -> FormattingUtil.ANSI_PURPLE;
            case WAITING_FOR_APPROVAL -> FormattingUtil.ANSI_CYAN;
            default -> FormattingUtil.ANSI_WHITE;
        };
    }
    
    private String getPriorityColor(AgentTask.TaskPriority priority) {
        return switch (priority) {
            case CRITICAL -> FormattingUtil.ANSI_RED + FormattingUtil.ANSI_BOLD;
            case HIGH -> FormattingUtil.ANSI_RED;
            case MEDIUM -> FormattingUtil.ANSI_YELLOW;
            case LOW -> FormattingUtil.ANSI_GREEN;
            case BACKGROUND -> FormattingUtil.ANSI_GRAY;
            default -> FormattingUtil.ANSI_WHITE;
        };
    }
    
    private String formatInstant(Instant instant) {
        if (instant == null) return "N/A";
        
        // Calculate time difference
        long diffSeconds = Instant.now().getEpochSecond() - instant.getEpochSecond();
        
        if (diffSeconds < 60) {
            return diffSeconds + "s ago";
        } else if (diffSeconds < 3600) {
            return (diffSeconds / 60) + "m ago";
        } else if (diffSeconds < 86400) {
            return (diffSeconds / 3600) + "h ago";
        } else {
            return (diffSeconds / 86400) + "d ago";
        }
    }
    
    private void displayAgentStatus() {
        try {
            var status = agentService.getStatus();
            System.out.println(FormattingUtil.formatWithColor("\n🤖 Detailed Agent Status", FormattingUtil.ANSI_CYAN + FormattingUtil.ANSI_BOLD));
            System.out.println(FormattingUtil.formatWithColor("─".repeat(50), FormattingUtil.ANSI_CYAN));
            
            System.out.println(FormattingUtil.formatWithColor(
                String.format("Running: %s", status.isRunning() ? "✅ Yes" : "❌ No"), 
                FormattingUtil.ANSI_WHITE));
            System.out.println(FormattingUtil.formatWithColor(
                String.format("Shutting Down: %s", status.isShuttingDown() ? "🔄 Yes" : "✅ No"), 
                FormattingUtil.ANSI_WHITE));
            System.out.println(FormattingUtil.formatWithColor(
                String.format("Tasks Executed: %d", status.getTotalTasksExecuted()), 
                FormattingUtil.ANSI_BLUE));            // Additional status info would go here when available
            System.out.println(FormattingUtil.formatWithColor(
                String.format("Uptime: %d ms", status.getUptime()), 
                FormattingUtil.ANSI_BLUE));
            
            if (status.getLastActivity() != null) {
                System.out.println(FormattingUtil.formatWithColor(
                    String.format("Last Activity: %s", status.getLastActivity()), 
                    FormattingUtil.ANSI_GRAY));
            }
            
        } catch (Exception e) {
            System.out.println(FormattingUtil.formatWithColor("\n❌ Error getting agent status: " + e.getMessage(), FormattingUtil.ANSI_RED));
        }
    }
      private void submitAgentTask(String taskDescription, List<ChatMessage> conversationHistory) {
        try {
            // Check if agent service is available
            if (agentService == null) {
                System.out.println(FormattingUtil.formatWithColor(
                    "\n❌ Agent service is not available. Make sure agent mode is enabled in configuration.", 
                    FormattingUtil.ANSI_RED));
                return;
            }
            
            if (taskQueue == null) {
                System.out.println(FormattingUtil.formatWithColor(
                    "\n❌ Task queue service is not available. Make sure agent mode is enabled in configuration.", 
                    FormattingUtil.ANSI_RED));
                return;
            }
            
            // Check if agent is running, if not, start it
            if (!agentService.isRunning()) {
                System.out.println(FormattingUtil.formatWithColor(
                    "\n🚀 Agent is not running. Starting agent...", 
                    FormattingUtil.ANSI_YELLOW));
                try {
                    agentService.startAgent();
                    System.out.println(FormattingUtil.formatWithColor(
                        "✓ Agent started successfully", 
                        FormattingUtil.ANSI_GREEN));
                    // Give agent a moment to initialize
                    Thread.sleep(2000);
                } catch (Exception e) {
                    System.out.println(FormattingUtil.formatWithColor(
                        "\n❌ Failed to start agent: " + e.getMessage(), 
                        FormattingUtil.ANSI_RED));
                    return;
                }
            }
            // Create an agent task based on the current conversation context
            String contextString = buildConversationContext(conversationHistory, 5); // Last 5 messages
            
            // Create TaskContext with conversation context
            AgentTask.TaskContext taskContext = AgentTask.TaskContext.builder()
                .sessionId("chat-session-" + System.currentTimeMillis())
                .triggerSource("interactive-chat")
                .build();
            
            // Store the conversation context in parameters
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("conversation_context", contextString);
            parameters.put("task_description", taskDescription);
              AgentTask task = AgentTask.builder()
                .id("chat-task-" + System.currentTimeMillis())
                .name("Chat Task: " + taskDescription.substring(0, Math.min(50, taskDescription.length())))
                .type(AgentTask.TaskType.CODE_GENERATION) // More appropriate for file creation tasks
                .description(taskDescription)
                .context(taskContext)
                .parameters(parameters)
                .priority(AgentTask.TaskPriority.MEDIUM)
                .createdAt(Instant.now())
                .build();
                
            agentService.submitTask(task);
            
            System.out.println(FormattingUtil.formatWithColor(
                String.format("\n📤 Task submitted to agent: %s", taskDescription), 
                FormattingUtil.ANSI_GREEN));
            System.out.println(FormattingUtil.formatWithColor(
                String.format("Task ID: %s", task.getId()), 
                FormattingUtil.ANSI_GRAY));
            
            // Wait for task completion and display results
            waitForTaskCompletionAndDisplayResults(task.getId());
                
        } catch (Exception e) {
            System.out.println(FormattingUtil.formatWithColor(
                "\n❌ Failed to submit task: " + e.getMessage(), 
                FormattingUtil.ANSI_RED));
        }
    }
    
    private void changeAgentMode(String modeStr) {
        try {
            AgentConfiguration.AgentMode newMode = AgentConfiguration.AgentMode.valueOf(modeStr.toUpperCase());
            agentConfig.setMode(newMode);
            
            System.out.println(FormattingUtil.formatWithColor(
                String.format("\n🔄 Agent mode changed to: %s", newMode.name()), 
                FormattingUtil.ANSI_GREEN));
            System.out.println(FormattingUtil.formatWithColor(
                "Note: Changes take effect immediately for new tasks", 
                FormattingUtil.ANSI_GRAY));
                
        } catch (IllegalArgumentException e) {
            System.out.println(FormattingUtil.formatWithColor(
                "\n❌ Invalid mode. Valid modes: INTERACTIVE, AUTONOMOUS, SUPERVISED, MANUAL", 
                FormattingUtil.ANSI_RED));
        }
    }
    
    private String buildConversationContext(List<ChatMessage> history, int maxMessages) {
        if (history.isEmpty()) {
            return "";
        }
        
        StringBuilder context = new StringBuilder();
        int startIndex = Math.max(0, history.size() - maxMessages);
        
        for (int i = startIndex; i < history.size(); i++) {
            ChatMessage msg = history.get(i);
            context.append(msg.isUser() ? "Human: " : "Assistant: ")
                   .append(msg.getContent())
                   .append("\n\n");
        }
        
        return context.toString();
    }
    
    /**
     * Wait for task completion and display verbose results
     */
    private void waitForTaskCompletionAndDisplayResults(String taskId) {
        if (taskQueue == null) {
            System.out.println(FormattingUtil.formatWithColor(
                "\n❌ Cannot monitor task: Task queue service is not available", 
                FormattingUtil.ANSI_RED));
            return;
        }
        
        System.out.println(FormattingUtil.formatWithColor(
            "\n⏳ Waiting for task completion...", 
            FormattingUtil.ANSI_YELLOW));
        
        try {
            AgentTask task = null;
            int attempts = 0;
            int maxAttempts = 60; // Wait up to 60 seconds
            
            // First check if task exists at all
            task = taskQueue.getTask(taskId);
            if (task == null) {
                System.out.println(FormattingUtil.formatWithColor(
                    "\n❌ Task not found immediately: " + taskId, 
                    FormattingUtil.ANSI_RED));
                System.out.println(FormattingUtil.formatWithColor(
                    "Checking all tasks in queue for debugging...", 
                    FormattingUtil.ANSI_GRAY));
                
                // Debug: List all tasks in queue
                var allTasks = taskQueue.getAllTasks();
                System.out.println(FormattingUtil.formatWithColor(
                    "Total tasks in queue: " + allTasks.size(), 
                    FormattingUtil.ANSI_GRAY));
                for (AgentTask t : allTasks) {
                    System.out.println(FormattingUtil.formatWithColor(
                        "  Task: " + t.getId() + " Status: " + t.getStatus() + " Type: " + t.getType(), 
                        FormattingUtil.ANSI_GRAY));
                }
                return;
            }
            
            System.out.println(FormattingUtil.formatWithColor(
                "✓ Task found. Initial status: " + task.getStatus(), 
                FormattingUtil.ANSI_BLUE));
            
            while (attempts < maxAttempts) {
                Thread.sleep(1000); // Wait 1 second between checks
                task = taskQueue.getTask(taskId);
                
                if (task != null) {
                    // Debug: Show current status every 10 attempts
                    if (attempts % 10 == 0) {
                        System.out.println(FormattingUtil.formatWithColor(
                            "\n🔍 Task status update: " + task.getStatus() + 
                            " (attempt " + attempts + "/" + maxAttempts + ")", 
                            FormattingUtil.ANSI_BLUE));
                    }
                    
                    if (task.isCompleted()) {
                        System.out.println(FormattingUtil.formatWithColor(
                            "\n✓ Task completed after " + attempts + " seconds with status: " + task.getStatus(), 
                            FormattingUtil.ANSI_GREEN));
                        break;
                    }
                }
                
                // Show progress indicator
                if (attempts % 5 == 0 && attempts > 0) {
                    System.out.print(".");
                }
                
                attempts++;
            }
            
            if (task == null) {
                System.out.println(FormattingUtil.formatWithColor(
                    "\n❌ Task disappeared during monitoring: " + taskId, 
                    FormattingUtil.ANSI_RED));
                return;
            }
            
            if (!task.isCompleted()) {
                System.out.println(FormattingUtil.formatWithColor(
                    "\n⏱️ Task is still running after " + maxAttempts + " seconds", 
                    FormattingUtil.ANSI_YELLOW));
                return;
            }
            
            // Display task results
            displayTaskResults(task);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println(FormattingUtil.formatWithColor(
                "\n❌ Task monitoring interrupted", 
                FormattingUtil.ANSI_RED));
        } catch (Exception e) {
            System.out.println(FormattingUtil.formatWithColor(
                "\n❌ Error monitoring task: " + e.getMessage(), 
                FormattingUtil.ANSI_RED));
        }
    }
    
    /**
     * Display detailed task execution results
     */
    private void displayTaskResults(AgentTask task) {
        System.out.println(); // Add spacing
        
        if (task.getStatus() == AgentTask.TaskStatus.COMPLETED) {
            System.out.println(FormattingUtil.formatWithColor(
                "✅ Task completed successfully!", 
                FormattingUtil.ANSI_GREEN));
        } else if (task.getStatus() == AgentTask.TaskStatus.FAILED) {
            System.out.println(FormattingUtil.formatWithColor(
                "❌ Task failed!", 
                FormattingUtil.ANSI_RED));
            if (task.getErrorMessage() != null) {
                System.out.println(FormattingUtil.formatWithColor(
                    "Error: " + task.getErrorMessage(), 
                    FormattingUtil.ANSI_RED));
            }
        }
        
        // Display task result if available
        AgentTask.TaskResult result = task.getResult();
        if (result != null) {
            // Display main output
            if (result.getOutput() != null && !result.getOutput().trim().isEmpty()) {
                System.out.println(FormattingUtil.formatWithColor(
                    "\n📄 Task Output:", 
                    FormattingUtil.ANSI_CYAN));
                System.out.println(FormattingUtil.formatWithColor(
                    "─".repeat(50), 
                    FormattingUtil.ANSI_GRAY));
                System.out.println(result.getOutput());
                System.out.println(FormattingUtil.formatWithColor(
                    "─".repeat(50), 
                    FormattingUtil.ANSI_GRAY));
            }
            
            // Display files created
            if (result.getFilesCreated() != null && !result.getFilesCreated().isEmpty()) {
                System.out.println(FormattingUtil.formatWithColor(
                    "\n📁 Files Created:", 
                    FormattingUtil.ANSI_GREEN));
                for (String file : result.getFilesCreated()) {
                    System.out.println(FormattingUtil.formatWithColor(
                        "  • " + file, 
                        FormattingUtil.ANSI_GREEN));
                }
            }
            
            // Display commands executed
            if (result.getCommandsExecuted() != null && !result.getCommandsExecuted().isEmpty()) {
                System.out.println(FormattingUtil.formatWithColor(
                    "\n⚡ Commands Executed:", 
                    FormattingUtil.ANSI_BLUE));
                for (String command : result.getCommandsExecuted()) {
                    System.out.println(FormattingUtil.formatWithColor(
                        "  • " + command, 
                        FormattingUtil.ANSI_BLUE));
                }
            }
            
            // Display exit code if available
            if (result.getExitCode() != null) {
                String exitCodeColor = result.getExitCode() == 0 ? FormattingUtil.ANSI_GREEN : FormattingUtil.ANSI_RED;
                System.out.println(FormattingUtil.formatWithColor(
                    "\n🔢 Exit Code: " + result.getExitCode(), 
                    exitCodeColor));
            }
            
            // Display execution time if available
            if (result.getExecutionTimeMs() > 0) {
                System.out.println(FormattingUtil.formatWithColor(
                    "⏱️  Execution Time: " + result.getExecutionTimeMs() + "ms", 
                    FormattingUtil.ANSI_GRAY));
            }
        }
        
        System.out.println(); // Add spacing after results
    }
    
    /**
     * Create and execute a ReAct-based plan
     */
    private void createAndExecutePlan(String goal, List<ChatMessage> conversationHistory) {
        if (planningService == null) {
            System.out.println(FormattingUtil.formatWithColor(
                "\n❌ Planning service is not available. Make sure agent mode is enabled in configuration.", 
                FormattingUtil.ANSI_RED));
            return;
        }
        
        try {
            // Check if agent is running, if not, start it
            if (!agentService.isRunning()) {
                System.out.println(FormattingUtil.formatWithColor(
                    "\n🚀 Agent is not running. Starting agent for plan execution...", 
                    FormattingUtil.ANSI_YELLOW));
                agentService.startAgent();
                Thread.sleep(2000);
            }
            
            System.out.println(FormattingUtil.formatWithColor(
                "\n🧠 Creating ReAct plan for goal: " + goal, 
                FormattingUtil.ANSI_CYAN + FormattingUtil.ANSI_BOLD));
            
            // Build context from conversation history
            Map<String, Object> context = new HashMap<>();
            if (!conversationHistory.isEmpty()) {
                context.put("conversation_context", buildConversationContext(conversationHistory, 5));
            }
            context.put("chat_mode", true);
            context.put("timestamp", java.time.Instant.now().toString());
            
            // Phase 1: Create the plan
            System.out.println(FormattingUtil.formatWithColor(
                "📋 Phase 1: Task Decomposition and Planning...", 
                FormattingUtil.ANSI_BLUE));
            
            var plan = planningService.createPlan(goal, context);
            
            System.out.println(FormattingUtil.formatWithColor(
                String.format("✅ Plan created with %d subtasks (ID: %s)", 
                    plan.getSubTasks().size(), plan.getId()), 
                FormattingUtil.ANSI_GREEN));
            
            // Display the plan
            displayPlanDetails(plan);
            
            // Phase 2: Execute the plan
            System.out.println(FormattingUtil.formatWithColor(
                "\n⚡ Phase 2: ReAct Execution (Reasoning + Acting cycles)...", 
                FormattingUtil.ANSI_BLUE));
            
            var execution = planningService.executePlan(plan.getId());
            
            // Display execution results
            displayPlanExecutionResults(execution, plan);
            
        } catch (Exception e) {
            System.out.println(FormattingUtil.formatWithColor(
                "\n❌ Failed to create or execute plan: " + e.getMessage(), 
                FormattingUtil.ANSI_RED));
            log.error("Error in plan creation/execution", e);
        }
    }
    
    /**
     * Display active plans
     */
    private void displayActivePlans() {
        if (planningService == null) {
            System.out.println(FormattingUtil.formatWithColor(
                "\n❌ Planning service is not available", FormattingUtil.ANSI_RED));
            return;
        }
        
        try {
            var activePlans = planningService.getActivePlans();
            
            if (activePlans.isEmpty()) {
                System.out.println(FormattingUtil.formatWithColor(
                    "\n📋 No active plans found", FormattingUtil.ANSI_GRAY));
                return;
            }
            
            System.out.println(FormattingUtil.formatWithColor(
                String.format("\n🗂️ Active Plans (%d total)", activePlans.size()), 
                FormattingUtil.ANSI_CYAN + FormattingUtil.ANSI_BOLD));
            System.out.println(FormattingUtil.formatWithColor("─".repeat(60), FormattingUtil.ANSI_CYAN));
            
            for (var plan : activePlans) {
                displayPlanSummary(plan);
                System.out.println(FormattingUtil.formatWithColor("  " + "─".repeat(56), FormattingUtil.ANSI_GRAY));
            }
            
        } catch (Exception e) {
            System.out.println(FormattingUtil.formatWithColor(
                "\n❌ Error retrieving active plans: " + e.getMessage(), 
                FormattingUtil.ANSI_RED));
        }
    }
    
    /**
     * Display detailed plan information
     */
    private void displayPlanDetails(sg.edu.nus.iss.misoto.cli.agent.planning.ExecutionPlan plan) {
        System.out.println(FormattingUtil.formatWithColor(
            "\n📊 Plan Details:", FormattingUtil.ANSI_CYAN));
        System.out.println(FormattingUtil.formatWithColor("─".repeat(50), FormattingUtil.ANSI_GRAY));
        
        System.out.println(FormattingUtil.formatWithColor(
            String.format("Goal: %s", plan.getGoal()), FormattingUtil.ANSI_WHITE));
        System.out.println(FormattingUtil.formatWithColor(
            String.format("Plan ID: %s", plan.getId()), FormattingUtil.ANSI_GRAY));
        System.out.println(FormattingUtil.formatWithColor(
            String.format("Status: %s", plan.getStatus()), getPlanStatusColor(plan.getStatus().name())));
        
        System.out.println(FormattingUtil.formatWithColor(
            "\n🔍 Subtasks:", FormattingUtil.ANSI_BLUE));
        
        for (int i = 0; i < plan.getSubTasks().size(); i++) {
            var subTask = plan.getSubTasks().get(i);
            String priorityColor = getSubTaskPriorityColor(subTask.getPriority().name());
            System.out.println(String.format("  %d. %s%s%s %s(%s)%s", 
                i + 1,
                FormattingUtil.ANSI_WHITE,
                subTask.getDescription(),
                FormattingUtil.ANSI_RESET,
                priorityColor,
                subTask.getPriority(),
                FormattingUtil.ANSI_RESET));
            
            if (subTask.getExpectedOutcome() != null && !subTask.getExpectedOutcome().isEmpty()) {
                System.out.println(FormattingUtil.formatWithColor(
                    String.format("     Expected: %s", subTask.getExpectedOutcome()), 
                    FormattingUtil.ANSI_GRAY));
            }
        }
    }
    
    /**
     * Display plan execution results
     */
    private void displayPlanExecutionResults(
            sg.edu.nus.iss.misoto.cli.agent.planning.PlanExecution execution, 
            sg.edu.nus.iss.misoto.cli.agent.planning.ExecutionPlan plan) {
        
        System.out.println(FormattingUtil.formatWithColor(
            "\n🎯 Plan Execution Results:", FormattingUtil.ANSI_CYAN + FormattingUtil.ANSI_BOLD));
        System.out.println(FormattingUtil.formatWithColor("─".repeat(60), FormattingUtil.ANSI_CYAN));
        
        System.out.println(FormattingUtil.formatWithColor(
            String.format("Execution Status: %s", execution.getStatus()), 
            getExecutionStatusColor(execution.getStatus().name())));
        
        if (execution.getStartedAt() != null && execution.getCompletedAt() != null) {
            long durationMs = execution.getCompletedAt().toEpochMilli() - execution.getStartedAt().toEpochMilli();
            System.out.println(FormattingUtil.formatWithColor(
                String.format("Total Execution Time: %d ms", durationMs), 
                FormattingUtil.ANSI_BLUE));
        }
        
        System.out.println(FormattingUtil.formatWithColor(
            String.format("Steps Completed: %d", execution.getSteps().size()), 
            FormattingUtil.ANSI_WHITE));
        
        // Display ReAct cycle details
        System.out.println(FormattingUtil.formatWithColor(
            "\n🔄 ReAct Cycles (Reasoning → Acting → Observation):", 
            FormattingUtil.ANSI_BLUE));
        
        for (int i = 0; i < execution.getSteps().size(); i++) {
            var step = execution.getSteps().get(i);
            System.out.println(FormattingUtil.formatWithColor(
                String.format("\nCycle %d: %s", i + 1, step.getSubTaskId()), 
                FormattingUtil.ANSI_YELLOW + FormattingUtil.ANSI_BOLD));
            
            if (step.getReasoning() != null) {
                System.out.println(FormattingUtil.formatWithColor("  🤔 Reasoning:", FormattingUtil.ANSI_CYAN));
                System.out.println(FormattingUtil.formatWithColor(
                    "     " + truncateText(step.getReasoning(), 100), FormattingUtil.ANSI_WHITE));
            }
            
            if (step.getAction() != null) {
                System.out.println(FormattingUtil.formatWithColor("  ⚡ Action:", FormattingUtil.ANSI_GREEN));
                System.out.println(FormattingUtil.formatWithColor(
                    "     " + step.getAction(), FormattingUtil.ANSI_WHITE));
            }
            
            if (step.getObservation() != null) {
                System.out.println(FormattingUtil.formatWithColor("  👁️ Observation:", FormattingUtil.ANSI_PURPLE));
                System.out.println(FormattingUtil.formatWithColor(
                    "     " + truncateText(step.getObservation(), 100), FormattingUtil.ANSI_WHITE));
            }
            
            System.out.println(FormattingUtil.formatWithColor(
                String.format("  Status: %s", step.getStatus()), 
                getStepStatusColor(step.getStatus().name())));
        }
        
        // Display working memory if available
        if (!execution.getWorkingMemory().isEmpty()) {
            System.out.println(FormattingUtil.formatWithColor(
                "\n🧠 Final Working Memory:", FormattingUtil.ANSI_BLUE));
            for (var entry : execution.getWorkingMemory().entrySet()) {
                System.out.println(FormattingUtil.formatWithColor(
                    String.format("  • %s: %s", entry.getKey(), truncateText(entry.getValue().toString(), 50)), 
                    FormattingUtil.ANSI_GRAY));
            }
        }
    }
    
    /**
     * Display plan summary
     */
    private void displayPlanSummary(sg.edu.nus.iss.misoto.cli.agent.planning.ExecutionPlan plan) {
        System.out.println(String.format("  %s%s%s %s[%s]%s", 
            FormattingUtil.ANSI_WHITE + FormattingUtil.ANSI_BOLD,
            plan.getGoal().length() > 50 ? plan.getGoal().substring(0, 47) + "..." : plan.getGoal(),
            FormattingUtil.ANSI_RESET,
            getPlanStatusColor(plan.getStatus().name()),
            plan.getStatus(),
            FormattingUtil.ANSI_RESET));
        
        System.out.println(FormattingUtil.formatWithColor(
            String.format("    ID: %s | Subtasks: %d | Created: %s", 
                plan.getId().substring(0, Math.min(8, plan.getId().length())),
                plan.getSubTasks().size(),
                formatInstant(plan.getCreatedAt())), 
            FormattingUtil.ANSI_GRAY));
    }
    
    /**
     * Helper methods for color coding
     */
    private String getExecutionStatusColor(String status) {
        return switch (status) {
            case "RUNNING" -> FormattingUtil.ANSI_BLUE;
            case "COMPLETED" -> FormattingUtil.ANSI_GREEN;
            case "FAILED" -> FormattingUtil.ANSI_RED;
            case "PAUSED" -> FormattingUtil.ANSI_YELLOW;
            case "CANCELLED" -> FormattingUtil.ANSI_GRAY;
            default -> FormattingUtil.ANSI_WHITE;
        };
    }
    
    private String getStepStatusColor(String status) {
        return switch (status) {
            case "RUNNING" -> FormattingUtil.ANSI_BLUE;
            case "COMPLETED" -> FormattingUtil.ANSI_GREEN;
            case "FAILED" -> FormattingUtil.ANSI_RED;
            case "SKIPPED" -> FormattingUtil.ANSI_GRAY;
            default -> FormattingUtil.ANSI_WHITE;
        };
    }
    
    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength - 3) + "..." : text;
    }
    
    /**
     * Color methods for planning enums
     */
    private String getPlanStatusColor(String status) {
        return switch (status) {
            case "CREATED" -> FormattingUtil.ANSI_YELLOW;
            case "EXECUTING" -> FormattingUtil.ANSI_BLUE;
            case "COMPLETED" -> FormattingUtil.ANSI_GREEN;
            case "FAILED" -> FormattingUtil.ANSI_RED;
            case "CANCELLED" -> FormattingUtil.ANSI_GRAY;
            default -> FormattingUtil.ANSI_WHITE;
        };
    }
    
    private String getSubTaskPriorityColor(String priority) {
        return switch (priority) {
            case "CRITICAL" -> FormattingUtil.ANSI_RED + FormattingUtil.ANSI_BOLD;
            case "HIGH" -> FormattingUtil.ANSI_RED;
            case "MEDIUM" -> FormattingUtil.ANSI_YELLOW;
            case "LOW" -> FormattingUtil.ANSI_GREEN;
            default -> FormattingUtil.ANSI_WHITE;
        };
    }
    
    /**
     * Clear the terminal screen
     */
    private void clearScreen() {
        try {
            // Clear screen using ANSI escape codes
            System.out.print("\033[2J\033[H");
            System.out.flush();
            
            // Alternative approach for Windows compatibility
            final String os = System.getProperty("os.name");
            if (os.contains("Windows")) {
                // For Windows systems, also try using cls command
                try {
                    new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                } catch (Exception e) {
                    // Fall back to ANSI codes only
                }
            }
        } catch (Exception e) {
            // If clearing fails, just add some newlines for spacing
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        }
    }

    /**
     * Show animated progress bar for agent shutdown
     */
    private void showAgentShutdownProgress(String action) {
        final int barWidth = 40;
        final String greenBar = "\u001B[42m \u001B[0m";  // Green background
        final String grayBar = "\u001B[100m \u001B[0m";   // Gray background
        final String yellow = "\u001B[33m";
        final String green = "\u001B[32m";
        final String red = "\u001B[31m";
        final String reset = "\u001B[0m";
        
        System.out.print(yellow + "Preset: " + action + " " + reset);
        System.out.flush();
        
        long startTime = System.currentTimeMillis();
        
        // Always animate progress bar from 0 to 100%
        int totalSteps = 100; // 100 steps for smooth animation
        for (int i = 0; i <= totalSteps; i++) {
            // Calculate progress
            int progress = (i * barWidth) / totalSteps;
            int percentage = i;
            long elapsed = System.currentTimeMillis() - startTime;
            
            // Clear line and redraw
            System.out.print("\r" + yellow + "Preset: " + action + " " + reset);
            
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
}
