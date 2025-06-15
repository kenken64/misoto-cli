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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        List<ChatMessage> conversationHistory = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        
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
                log.error("Error reading user input in chat", e);
                System.err.println(FormattingUtil.formatWithColor("Error reading input. Ending chat session.", FormattingUtil.ANSI_RED));
                break;
            } catch (Exception e) {
                log.error("Error processing chat message", e);
                System.err.println(FormattingUtil.formatWithColor("Error: " + e.getMessage(), FormattingUtil.ANSI_RED));
                // Continue the chat loop even after errors
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
            AiClient.AiResponse aiResponse = aiClient.sendMessageWithUsage(SYSTEM_PROMPT, conversationContext.toString());
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Add Claude's response to history
            conversationHistory.add(new ChatMessage(false, aiResponse.getText()));
            
            // Update session tracking
            messageCount++;
            if (aiResponse.getInputTokens() != null) {
                sessionTotalInputTokens += aiResponse.getInputTokens();
            }
            if (aiResponse.getOutputTokens() != null) {
                sessionTotalOutputTokens += aiResponse.getOutputTokens();
            }
            if (aiResponse.getEstimatedCost() != null) {
                sessionTotalCost += aiResponse.getEstimatedCost();
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
            if (aiResponse.getInputTokens() != null && aiResponse.getOutputTokens() != null) {
                String usageInfo = String.format("📊 Tokens: %d in + %d out = %d total", 
                    aiResponse.getInputTokens(), aiResponse.getOutputTokens(), aiResponse.getTotalTokens());
                System.out.println(FormattingUtil.formatWithColor(usageInfo, FormattingUtil.ANSI_BLUE));
                  if (aiResponse.getEstimatedCost() != null) {
                    String costInfo = String.format("💰 Estimated cost: $%.6f", aiResponse.getEstimatedCost());
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
            FormattingUtil.ANSI_BLUE));
        
        System.out.println(FormattingUtil.formatWithColor("\nAgent Commands:", FormattingUtil.ANSI_YELLOW));
        System.out.println("• Type 'start' to start the agent");
        System.out.println("• Type 'stop' to stop the agent");
        System.out.println("• Type 'status' to see detailed agent status");
        System.out.println("• Type 'task <description>' to submit a task to the agent");
        System.out.println("• Type 'mode <INTERACTIVE|AUTONOMOUS|SUPERVISED|MANUAL>' to change mode");
        System.out.println("• Press Enter to return to chat");
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.print(FormattingUtil.formatWithColor("\nAgent> ", FormattingUtil.ANSI_PURPLE + FormattingUtil.ANSI_BOLD));
            String agentInput = reader.readLine();
            
            if (agentInput == null || agentInput.trim().isEmpty()) {
                return;
            }
            
            processAgentCommand(agentInput.trim(), conversationHistory);
            
        } catch (IOException e) {
            log.error("Error reading agent command", e);
            System.err.println(FormattingUtil.formatWithColor("Error reading command", FormattingUtil.ANSI_RED));
        }
    }
    
    private void processAgentCommand(String command, List<ChatMessage> conversationHistory) {
        String[] parts = command.split("\\s+", 2);
        String action = parts[0].toLowerCase();
        
        switch (action) {
            case "start":
                if (agentService.isRunning()) {
                    System.out.println(FormattingUtil.formatWithColor("\n🤖 Agent is already running", FormattingUtil.ANSI_YELLOW));
                } else {
                    try {
                        agentService.startAgent();
                        System.out.println(FormattingUtil.formatWithColor("\n🚀 Agent started successfully!", FormattingUtil.ANSI_GREEN));
                    } catch (Exception e) {
                        System.out.println(FormattingUtil.formatWithColor("\n❌ Failed to start agent: " + e.getMessage(), FormattingUtil.ANSI_RED));
                    }
                }
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
                
            case "mode":
                if (parts.length < 2) {
                    System.out.println(FormattingUtil.formatWithColor("\n❌ Please specify a mode: INTERACTIVE, AUTONOMOUS, SUPERVISED, or MANUAL", FormattingUtil.ANSI_RED));
                } else {
                    changeAgentMode(parts[1]);
                }
                break;
                
            default:
                System.out.println(FormattingUtil.formatWithColor("\n❓ Unknown agent command: " + action, FormattingUtil.ANSI_RED));
                break;
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
                .type(AgentTask.TaskType.AI_ANALYSIS)
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
}
