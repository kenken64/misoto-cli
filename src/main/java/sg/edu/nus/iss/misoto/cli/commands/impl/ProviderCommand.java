package sg.edu.nus.iss.misoto.cli.commands.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import sg.edu.nus.iss.misoto.cli.ai.provider.*;
import sg.edu.nus.iss.misoto.cli.commands.Command;
import sg.edu.nus.iss.misoto.cli.utils.FormattingUtil;

import java.util.List;
import java.util.Map;

/**
 * Command for managing AI providers
 */
@Component
@Slf4j
public class ProviderCommand implements Command {
    
    @Autowired
    private AiProviderManager providerManager;
    
    @Autowired
    private Environment environment;
    
    @Override
    public String getName() {
        return "provider";
    }
    
    @Override
    public String getDescription() {
        return "Manage AI providers (Anthropic, Ollama, etc.)";
    }
    
    @Override
    public String getCategory() {
        return "AI";
    }
    
    @Override
    public boolean isHidden() {
        return false;
    }
    
    @Override
    public boolean requiresAuth() {
        return false;
    }
    
    @Override
    public String getUsage() {
        return "provider <subcommand> [options]";
    }
    
    @Override
    public List<String> getExamples() {
        return List.of(
            "provider list",
            "provider switch anthropic",
            "provider switch ollama", 
            "provider models",
            "provider model llama3.2",
            "provider status",
            "provider test"
        );
    }
    
    @Override
    public void execute(List<String> args) throws Exception {
        if (args.isEmpty()) {
            showUsage();
            return;
        }
        
        String subCommand = args.get(0).toLowerCase();
        
        switch (subCommand) {
            case "list":
            case "ls":
                handleList();
                break;
            case "switch":
            case "use":
                handleSwitch(args);
                break;
            case "models":
                handleModels();
                break;
            case "model":
                handleSetModel(args);
                break;
            case "status":
                handleStatus();
                break;
            case "test":
                handleTest(args);
                break;
            default:
                System.err.println("Unknown subcommand: " + subCommand);
                showUsage();
                break;
        }
    }
    
    private void handleList() {
        System.out.println(FormattingUtil.formatWithColor("\n🤖 Available AI Providers:", FormattingUtil.ANSI_CYAN + FormattingUtil.ANSI_BOLD));
        System.out.println(FormattingUtil.formatWithColor("─".repeat(50), FormattingUtil.ANSI_CYAN));
        
        Map<String, ProviderStatus> statuses = providerManager.getProviderStatuses();
        
        for (ProviderStatus status : statuses.values()) {
            String indicator = status.isCurrent() ? "👉" : "  ";
            String availability = status.isAvailable() ? "🟢" : "🔴";
            String modelInfo = status.getCurrentModel() != null ? " (" + status.getCurrentModel() + ")" : "";
            
            System.out.printf("%s %s %s - %s%s%n", 
                indicator, 
                availability,
                FormattingUtil.formatWithColor(status.getDisplayName(), FormattingUtil.ANSI_WHITE + FormattingUtil.ANSI_BOLD),
                status.getName(),
                FormattingUtil.formatWithColor(modelInfo, FormattingUtil.ANSI_GRAY)
            );
        }
        
        System.out.println(FormattingUtil.formatWithColor("\nLegend: 👉 Current  🟢 Available  🔴 Unavailable", FormattingUtil.ANSI_GRAY));
    }
    
    private void handleSwitch(List<String> args) {
        if (args.size() < 2) {
            System.err.println("Usage: provider switch <provider-name>");
            System.err.println("Available providers: " + String.join(", ", providerManager.getAvailableProviders()));
            return;
        }
        
        String providerName = args.get(1).toLowerCase();
        
        System.out.println(FormattingUtil.formatWithColor("Switching to provider: " + providerName, FormattingUtil.ANSI_YELLOW));
        
        boolean success = providerManager.switchProvider(providerName);
        
        if (success) {
            AiProvider currentProvider = providerManager.getCurrentProvider();
            System.out.println(FormattingUtil.formatWithColor(
                String.format("✅ Successfully switched to %s (%s)", 
                    currentProvider.getDisplayName(), 
                    currentProvider.getCurrentModel()), 
                FormattingUtil.ANSI_GREEN));
        } else {
            System.err.println(FormattingUtil.formatWithColor(
                "❌ Failed to switch to provider: " + providerName, 
                FormattingUtil.ANSI_RED));
            System.err.println("Available providers: " + String.join(", ", providerManager.getAvailableProviders()));
        }
    }
    
    private void handleModels() {
        AiProvider currentProvider = providerManager.getCurrentProvider();
        if (currentProvider == null) {
            System.err.println("No provider selected");
            return;
        }
        
        System.out.println(FormattingUtil.formatWithColor(
            String.format("\n📋 Available Models for %s:", currentProvider.getDisplayName()), 
            FormattingUtil.ANSI_CYAN + FormattingUtil.ANSI_BOLD));
        System.out.println(FormattingUtil.formatWithColor("─".repeat(40), FormattingUtil.ANSI_CYAN));
        
        List<String> models = currentProvider.getAvailableModels();
        String currentModel = currentProvider.getCurrentModel();
        
        for (String model : models) {
            String indicator = model.equals(currentModel) ? "👉" : "  ";
            System.out.printf("%s %s%n", 
                indicator, 
                FormattingUtil.formatWithColor(model, 
                    model.equals(currentModel) ? FormattingUtil.ANSI_GREEN + FormattingUtil.ANSI_BOLD : FormattingUtil.ANSI_WHITE));
        }
        
        System.out.println(FormattingUtil.formatWithColor("\n👉 Current model", FormattingUtil.ANSI_GRAY));
    }
    
    private void handleSetModel(List<String> args) {
        if (args.size() < 2) {
            System.err.println("Usage: provider model <model-name>");
            handleModels();
            return;
        }
        
        String modelName = args.get(1);
        
        System.out.println(FormattingUtil.formatWithColor("Setting model to: " + modelName, FormattingUtil.ANSI_YELLOW));
        
        boolean success = providerManager.setModel(modelName);
        
        if (success) {
            System.out.println(FormattingUtil.formatWithColor(
                "✅ Model changed to: " + modelName, 
                FormattingUtil.ANSI_GREEN));
        } else {
            System.err.println(FormattingUtil.formatWithColor(
                "❌ Failed to set model: " + modelName, 
                FormattingUtil.ANSI_RED));
            handleModels();
        }
    }
    
    private void handleStatus() {
        System.out.println(FormattingUtil.formatWithColor("\n🔍 AI Provider Status:", FormattingUtil.ANSI_CYAN + FormattingUtil.ANSI_BOLD));
        System.out.println(FormattingUtil.formatWithColor("─".repeat(50), FormattingUtil.ANSI_CYAN));
        
        AiProvider currentProvider = providerManager.getCurrentProvider();
        if (currentProvider == null) {
            System.out.println(FormattingUtil.formatWithColor("❌ No provider selected", FormattingUtil.ANSI_RED));
            return;
        }
        
        // Force provider initialization by checking availability
        currentProvider.isAvailable();
        
        System.out.println(FormattingUtil.formatWithColor("Current Provider: " + currentProvider.getDisplayName(), FormattingUtil.ANSI_WHITE + FormattingUtil.ANSI_BOLD));
        System.out.println(FormattingUtil.formatWithColor("Provider Name: " + currentProvider.getProviderName(), FormattingUtil.ANSI_WHITE));
        System.out.println(FormattingUtil.formatWithColor("Current Model: " + currentProvider.getCurrentModel(), FormattingUtil.ANSI_WHITE));
        System.out.println(FormattingUtil.formatWithColor("Available: " + (currentProvider.isAvailable() ? "✅ Yes" : "❌ No"), FormattingUtil.ANSI_WHITE));
        
        ProviderCapabilities caps = currentProvider.getCapabilities();
        System.out.println(FormattingUtil.formatWithColor("\nCapabilities:", FormattingUtil.ANSI_YELLOW));
        System.out.println("  • Chat: " + (caps.isSupportsChat() ? "✅" : "❌"));
        System.out.println("  • Streaming: " + (caps.isSupportsStreaming() ? "✅" : "❌"));
        System.out.println("  • Function Calling: " + (caps.isSupportsFunctionCalling() ? "✅" : "❌"));
        System.out.println("  • System Prompts: " + (caps.isSupportsSystemPrompts() ? "✅" : "❌"));
        System.out.println("  • History: " + (caps.isSupportsHistory() ? "✅" : "❌"));
        
        if (caps.getMaxTokens() != null) {
            System.out.println("  • Max Tokens: " + caps.getMaxTokens());
        }
        
        // Show environment configuration for specific providers
        if ("ollama".equals(currentProvider.getProviderName())) {
            System.out.println(FormattingUtil.formatWithColor("\nEnvironment Configuration:", FormattingUtil.ANSI_YELLOW));
            String ollamaHost = environment.getProperty("OLLAMA_HOST");
            String misotoOllamaUrl = environment.getProperty("MISOTO_AI_OLLAMA_URL");
            String misotoOllamaModel = environment.getProperty("MISOTO_AI_OLLAMA_MODEL");
            String defaultProvider = environment.getProperty("MISOTO_AI_DEFAULT_PROVIDER");
            
            System.out.println("  • OLLAMA_HOST: " + (ollamaHost != null ? ollamaHost : "not set"));
            System.out.println("  • MISOTO_AI_OLLAMA_URL: " + (misotoOllamaUrl != null ? misotoOllamaUrl : "not set"));
            System.out.println("  • MISOTO_AI_OLLAMA_MODEL: " + (misotoOllamaModel != null ? misotoOllamaModel : "not set"));
            System.out.println("  • MISOTO_AI_DEFAULT_PROVIDER: " + (defaultProvider != null ? defaultProvider : "not set"));
        } else if ("anthropic".equals(currentProvider.getProviderName())) {
            System.out.println(FormattingUtil.formatWithColor("\nEnvironment Configuration:", FormattingUtil.ANSI_YELLOW));
            String anthropicApiKey = environment.getProperty("ANTHROPIC_API_KEY");
            String defaultProvider = environment.getProperty("MISOTO_AI_DEFAULT_PROVIDER");
            
            System.out.println("  • ANTHROPIC_API_KEY: " + (anthropicApiKey != null ? "✅ Set" : "❌ Not set"));
            System.out.println("  • MISOTO_AI_DEFAULT_PROVIDER: " + (defaultProvider != null ? defaultProvider : "not set"));
        }
        
        AiUsage lastUsage = currentProvider.getLastUsage();
        if (lastUsage != null) {
            System.out.println(FormattingUtil.formatWithColor("\nLast Usage:", FormattingUtil.ANSI_YELLOW));
            System.out.println("  • Input Tokens: " + lastUsage.getInputTokens());
            System.out.println("  • Output Tokens: " + lastUsage.getOutputTokens());
            System.out.println("  • Total Tokens: " + lastUsage.getTotalTokens());
            if (lastUsage.getEstimatedCost() != null && lastUsage.getEstimatedCost() > 0) {
                System.out.println(String.format("  • Estimated Cost: $%.6f", lastUsage.getEstimatedCost()));
            }
        }
    }
    
    private void handleTest(List<String> args) {
        AiProvider currentProvider = providerManager.getCurrentProvider();
        if (currentProvider == null) {
            System.err.println("No provider selected");
            return;
        }
        
        String testMessage = args.size() > 1 ? String.join(" ", args.subList(1, args.size())) : "Hello! Please respond with 'Hello from " + currentProvider.getDisplayName() + "!' to confirm you're working.";
        
        System.out.println(FormattingUtil.formatWithColor(
            String.format("🧪 Testing %s with message: %s", currentProvider.getDisplayName(), testMessage), 
            FormattingUtil.ANSI_YELLOW));
        
        try {
            long startTime = System.currentTimeMillis();
            AiResponse response = providerManager.sendMessage(testMessage);
            long duration = System.currentTimeMillis() - startTime;
            
            if (response.isSuccess()) {
                System.out.println(FormattingUtil.formatWithColor("\n✅ Test successful!", FormattingUtil.ANSI_GREEN + FormattingUtil.ANSI_BOLD));
                System.out.println(FormattingUtil.formatWithColor("Response:", FormattingUtil.ANSI_CYAN));
                System.out.println(response.getText());
                System.out.println(FormattingUtil.formatWithColor(String.format("\n⏱️  Response time: %dms", duration), FormattingUtil.ANSI_GRAY));
                
                if (response.getUsage() != null) {
                    AiUsage usage = response.getUsage();
                    System.out.println(FormattingUtil.formatWithColor(
                        String.format("📊 Tokens - Input: %d, Output: %d, Total: %d", 
                            usage.getInputTokens(), usage.getOutputTokens(), usage.getTotalTokens()), 
                        FormattingUtil.ANSI_GRAY));
                }
            } else {
                System.err.println(FormattingUtil.formatWithColor("❌ Test failed: " + response.getErrorMessage(), FormattingUtil.ANSI_RED));
            }
        } catch (Exception e) {
            System.err.println(FormattingUtil.formatWithColor("❌ Test failed with exception: " + e.getMessage(), FormattingUtil.ANSI_RED));
        }
    }
    
    private void showUsage() {
        System.out.println(FormattingUtil.formatWithColor("\n🤖 AI Provider Management", FormattingUtil.ANSI_CYAN + FormattingUtil.ANSI_BOLD));
        System.out.println(FormattingUtil.formatWithColor("─".repeat(40), FormattingUtil.ANSI_CYAN));
        System.out.println("Commands:");
        System.out.println("  provider list           - List all available providers");
        System.out.println("  provider switch <name>  - Switch to a different provider");
        System.out.println("  provider models         - List available models for current provider");
        System.out.println("  provider model <name>   - Set model for current provider");
        System.out.println("  provider status         - Show detailed provider status");
        System.out.println("  provider test [message] - Test current provider");
        System.out.println(FormattingUtil.formatWithColor("\nExamples:", FormattingUtil.ANSI_YELLOW));
        for (String example : getExamples()) {
            System.out.println("  " + example);
        }
    }
}
