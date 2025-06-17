package sg.edu.nus.iss.misoto.cli.terminal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.misoto.cli.config.ApplicationConfig;
import sg.edu.nus.iss.misoto.cli.utils.FormattingUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Terminal implementation for user interaction
 * 
 * Provides terminal interface functionality including display formatting,
 * color support, user input prompts, and progress indicators.
 */
@Service("cliTerminal")
@Slf4j
public class Terminal implements TerminalInterface {
    
    private final TerminalConfig config;
    private final boolean isInteractive;
    private final boolean supportsColors;
    private final Map<String, SpinnerInstance> activeSpinners = new HashMap<>();
    private final AtomicInteger spinnerId = new AtomicInteger(0);
    
    /**
     * Create a terminal with specified configuration
     */
    public Terminal(TerminalConfig config) {
        this.config = config;
        this.isInteractive = detectInteractivity();
        this.supportsColors = detectColorSupport();
        
        log.debug("Terminal initialized - interactive: {}, colors: {}", isInteractive, supportsColors);
    }
    
    /**
     * Create a terminal from application configuration
     */
    public Terminal(ApplicationConfig appConfig) {
        this(createTerminalConfig(appConfig));
    }
    
    /**
     * Default constructor for Spring
     */
    public Terminal() {
        this(new TerminalConfig());
    }
    
    private static TerminalConfig createTerminalConfig(ApplicationConfig appConfig) {
        TerminalConfig config = new TerminalConfig();
        if (appConfig.getTerminal() != null) {
            config.setTheme(appConfig.getTerminal().getTheme());
            config.setUseColors(appConfig.getTerminal().isUseColors());
            config.setShowProgressIndicators(appConfig.getTerminal().isShowProgressIndicators());
            config.setCodeHighlighting(appConfig.getTerminal().isCodeHighlighting());
            config.setMaxHeight(appConfig.getTerminal().getMaxHeight());
            config.setMaxWidth(appConfig.getTerminal().getMaxWidth());
        }
        return config;
    }
    
    @Override
    public void clear() {
        if (isInteractive && config.isUseColors()) {
            // ANSI escape sequence to clear screen and move cursor to top-left
            System.out.print("\033[2J\033[0;0H");
            System.out.flush();
        }
    }
    
    @Override
    public void display(String content) {
        if (content == null || content.isEmpty()) {
            return;
        }
        
        String formatted = formatContent(content);
        System.out.println(formatted);
    }
    
    @Override
    public void displayWelcome() {
        clear();
        
        String version = getClass().getPackage().getImplementationVersion();
        if (version == null) version = "0.0.1-SNAPSHOT";
        
        if (supportsColors && config.isUseColors()) {
            System.out.println(FormattingUtil.ANSI_BLUE + FormattingUtil.ANSI_BOLD + "\n  Claude Code CLI" + FormattingUtil.ANSI_RESET);
            System.out.println(FormattingUtil.ANSI_GRAY + "  Version " + version + " (Research Preview)\n" + FormattingUtil.ANSI_RESET);
            
            System.out.println(FormattingUtil.ANSI_WHITE + "  Welcome! Type " + FormattingUtil.ANSI_CYAN + "help" + FormattingUtil.ANSI_WHITE + " to see available commands." + FormattingUtil.ANSI_RESET);
            System.out.println(FormattingUtil.ANSI_WHITE + "  You can ask Claude to explain code, fix issues, or perform tasks." + FormattingUtil.ANSI_RESET);
            System.out.println(FormattingUtil.ANSI_WHITE + "  Example: \"" + FormattingUtil.ANSI_ITALIC + "Please analyze this codebase and explain its structure." + FormattingUtil.ANSI_RESET + FormattingUtil.ANSI_WHITE + "\"\n" + FormattingUtil.ANSI_RESET);
            
            System.out.println(FormattingUtil.ANSI_DIM + "  Pro tip: Use Ctrl+C to interrupt Claude and start over.\n" + FormattingUtil.ANSI_RESET);
        } else {
            System.out.println("\n  Claude Code CLI");
            System.out.println("  Version " + version + " (Research Preview)\n");
            
            System.out.println("  Welcome! Type 'help' to see available commands.");
            System.out.println("  You can ask Claude to explain code, fix issues, or perform tasks.");
            System.out.println("  Example: \"Please analyze this codebase and explain its structure.\"\n");
            
            System.out.println("  Pro tip: Use Ctrl+C to interrupt Claude and start over.\n");
        }
    }
    
    @Override
    public void emphasize(String message) {
        if (supportsColors && config.isUseColors()) {
            System.out.println(FormattingUtil.ANSI_CYAN + FormattingUtil.ANSI_BOLD + message + FormattingUtil.ANSI_RESET);
        } else {
            System.out.println(message.toUpperCase());
        }
    }
    
    @Override
    public void info(String message) {
        if (supportsColors && config.isUseColors()) {
            System.out.println(FormattingUtil.ANSI_BLUE + "ℹ " + message + FormattingUtil.ANSI_RESET);
        } else {
            System.out.println("INFO: " + message);
        }
    }
    
    @Override
    public void success(String message) {
        if (supportsColors && config.isUseColors()) {
            System.out.println(FormattingUtil.ANSI_GREEN + "✓ " + message + FormattingUtil.ANSI_RESET);
        } else {
            System.out.println("SUCCESS: " + message);
        }
    }
    
    @Override
    public void warn(String message) {
        if (supportsColors && config.isUseColors()) {
            System.out.println(FormattingUtil.ANSI_YELLOW + "⚠ " + message + FormattingUtil.ANSI_RESET);
        } else {
            System.out.println("WARNING: " + message);
        }
    }
    
    @Override
    public void error(String message) {
        if (supportsColors && config.isUseColors()) {
            System.out.println(FormattingUtil.ANSI_RED + "✗ " + message + FormattingUtil.ANSI_RESET);
        } else {
            System.out.println("ERROR: " + message);
        }
    }
    
    @Override
    public String link(String text, String url) {
        if (supportsColors && config.isUseColors() && isInteractive) {
            // OSC 8 hyperlink format: \033]8;;URL\033\\TEXT\033]8;;\033\\
            return "\033]8;;" + url + "\033\\" + text + "\033]8;;\033\\";
        } else {
            return text + " (" + url + ")";
        }
    }
    
    @Override
    public void table(List<List<String>> data, TableOptions options) {
        if (data == null || data.isEmpty()) {
            return;
        }
        
        // Calculate column widths
        int columns = data.get(0).size();
        int[] widths = new int[columns];
        
        // Include headers in width calculation
        if (options.getHeaders() != null && !options.getHeaders().isEmpty()) {
            for (int i = 0; i < Math.min(columns, options.getHeaders().size()); i++) {
                widths[i] = Math.max(widths[i], options.getHeaders().get(i).length());
            }
        }
        
        // Calculate data column widths
        for (List<String> row : data) {
            for (int i = 0; i < Math.min(columns, row.size()); i++) {
                widths[i] = Math.max(widths[i], row.get(i) != null ? row.get(i).length() : 0);
            }
        }
        
        // Print table
        if (options.isShowBorder()) {
            printTableBorder(widths, "top");
        }
        
        if (options.getHeaders() != null && !options.getHeaders().isEmpty()) {
            printTableRow(options.getHeaders(), widths, true);
            if (options.isShowBorder()) {
                printTableBorder(widths, "middle");
            }
        }
        
        for (int i = 0; i < data.size(); i++) {
            boolean alternate = options.isAlternateRowColors() && i % 2 == 1;
            printTableRow(data.get(i), widths, false, alternate);
        }
        
        if (options.isShowBorder()) {
            printTableBorder(widths, "bottom");
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T prompt(PromptOptions<T> options) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        
        try {
            switch (options.getType()) {
                case INPUT:
                    return (T) promptInput(reader, options);
                case PASSWORD:
                    return (T) promptPassword(reader, options);
                case CONFIRM:
                    return (T) Boolean.valueOf(promptConfirm(reader, options));
                case LIST:
                    return (T) promptList(reader, options);
                case CHECKBOX:
                    return (T) promptCheckbox(reader, options);
                default:
                    throw new IllegalArgumentException("Unsupported prompt type: " + options.getType());
            }
        } catch (IOException e) {
            log.error("Error reading user input", e);
            return options.getDefaultValue();
        }
    }
    
    @Override
    public SpinnerInstance spinner(String text) {
        if (!config.isShowProgressIndicators()) {
            return createDummySpinner(text);
        }
        
        String id = "spinner-" + spinnerId.incrementAndGet();
        SpinnerInstance spinner = new SimpleSpinner(id, text, this);
        activeSpinners.put(id, spinner);
        return spinner;
    }
    
    @Override
    public TerminalDimensions getDimensions() {
        try {
            // Try to get terminal size using system properties or environment
            String columns = System.getenv("COLUMNS");
            String lines = System.getenv("LINES");
            
            int width = 80; // default
            int height = 24; // default
            
            if (columns != null) {
                try {
                    width = Integer.parseInt(columns);
                } catch (NumberFormatException ignored) {}
            }
            
            if (lines != null) {
                try {
                    height = Integer.parseInt(lines);
                } catch (NumberFormatException ignored) {}
            }
            
            // Apply config overrides
            if (config.getMaxWidth() != null) {
                width = Math.min(width, config.getMaxWidth());
            }
            if (config.getMaxHeight() != null) {
                height = Math.min(height, config.getMaxHeight());
            }
            
            return new TerminalDimensions(width, height);
        } catch (Exception e) {
            log.debug("Could not determine terminal size", e);
            return new TerminalDimensions(80, 24);
        }
    }
    
    @Override
    public boolean supportsColors() {
        return supportsColors;
    }
    
    @Override
    public boolean isInteractive() {
        return isInteractive;
    }
    
    // Private helper methods
    
    private boolean detectInteractivity() {
        // Check if both stdin and stdout are connected to a terminal
        return System.console() != null;
    }
    
    private boolean detectColorSupport() {
        if (!config.isUseColors()) {
            return false;
        }
        
        // Check environment variables for color support
        String term = System.getenv("TERM");
        String colorTerm = System.getenv("COLORTERM");
        
        if (colorTerm != null && !colorTerm.isEmpty()) {
            return true;
        }
        
        if (term != null) {
            return term.contains("color") || term.contains("256") || term.equals("xterm");
        }
        
        // Default to true for interactive sessions
        return isInteractive;
    }
    
    private String formatContent(String content) {
        if (!supportsColors || !config.isUseColors()) {
            return content;
        }
        
        // Apply basic markdown-like formatting
        String formatted = content;
        
        // Format inline code
        formatted = formatted.replaceAll("`([^`]+)`", FormattingUtil.ANSI_CYAN + "$1" + FormattingUtil.ANSI_RESET);
        
        // Format bold text
        formatted = formatted.replaceAll("\\*\\*([^*]+)\\*\\*", FormattingUtil.ANSI_BOLD + "$1" + FormattingUtil.ANSI_RESET);
        
        // Format headers
        formatted = formatted.replaceAll("^(#+)\\s+(.+)$", 
            FormattingUtil.ANSI_BLUE + FormattingUtil.ANSI_BOLD + "$2" + FormattingUtil.ANSI_RESET);
        
        return formatted;
    }
    
    private void printTableBorder(int[] widths, String position) {
        if (!config.isUseColors() || !supportsColors) {
            // Simple ASCII border
            for (int i = 0; i < widths.length; i++) {
                System.out.print("+" + "-".repeat(widths[i] + 2));
            }
            System.out.println("+");
        } else {
            // Unicode box drawing characters
            String left = "├", middle = "┼", right = "┤", horizontal = "─";
            
            if ("top".equals(position)) {
                left = "┌"; middle = "┬"; right = "┐";
            } else if ("bottom".equals(position)) {
                left = "└"; middle = "┴"; right = "┘";
            }
            
            System.out.print(left);
            for (int i = 0; i < widths.length; i++) {
                System.out.print(horizontal.repeat(widths[i] + 2));
                if (i < widths.length - 1) {
                    System.out.print(middle);
                }
            }
            System.out.println(right);
        }
    }
    
    private void printTableRow(List<String> row, int[] widths, boolean isHeader) {
        printTableRow(row, widths, isHeader, false);
    }
    
    private void printTableRow(List<String> row, int[] widths, boolean isHeader, boolean alternate) {
        String prefix = config.isUseColors() && supportsColors ? "│ " : "| ";
        String suffix = config.isUseColors() && supportsColors ? " │" : " |";
        
        System.out.print(prefix);
        
        for (int i = 0; i < widths.length; i++) {
            String cell = i < row.size() && row.get(i) != null ? row.get(i) : "";
            
            if (isHeader && supportsColors && config.isUseColors()) {
                cell = FormattingUtil.ANSI_BOLD + cell + FormattingUtil.ANSI_RESET;
            } else if (alternate && supportsColors && config.isUseColors()) {
                cell = FormattingUtil.ANSI_DIM + cell + FormattingUtil.ANSI_RESET;
            }
            
            System.out.printf("%-" + widths[i] + "s", cell);
            
            if (i < widths.length - 1) {
                System.out.print(config.isUseColors() && supportsColors ? " │ " : " | ");
            }
        }
        
        System.out.println(suffix);
    }
    
    private String promptInput(BufferedReader reader, PromptOptions<?> options) throws IOException {
        String prompt = buildPrompt(options);
        System.out.print(prompt);
        
        String input = reader.readLine();
        if (input == null || input.trim().isEmpty()) {
            if (options.getDefaultValue() != null) {
                return options.getDefaultValue().toString();
            } else if (options.isRequired()) {
                error("Input is required");
                return promptInput(reader, options);
            }
        }
        
        return input != null ? input.trim() : "";
    }
    
    private String promptPassword(BufferedReader reader, PromptOptions<?> options) throws IOException {
        String prompt = buildPrompt(options);
        System.out.print(prompt);
        
        // For password prompts, we'd ideally use System.console().readPassword()
        // but that's not available in all environments
        java.io.Console console = System.console();
        if (console != null) {
            char[] password = console.readPassword();
            return password != null ? new String(password) : "";
        } else {
            // Fallback to regular input (not ideal for passwords but better than nothing)
            warn("Console not available - password will be visible");
            return reader.readLine();
        }
    }
    
    private boolean promptConfirm(BufferedReader reader, PromptOptions<?> options) throws IOException {
        String prompt = buildPrompt(options) + " (y/N) ";
        System.out.print(prompt);
        
        String input = reader.readLine();
        if (input == null || input.trim().isEmpty()) {
            return options.getDefaultValue() != null ? (Boolean) options.getDefaultValue() : false;
        }
        
        return input.trim().toLowerCase().startsWith("y");
    }
    
    private String promptList(BufferedReader reader, PromptOptions<?> options) throws IOException {
        if (options.getChoices() == null || options.getChoices().isEmpty()) {
            throw new IllegalArgumentException("List prompt requires choices");
        }
        
        System.out.println(options.getMessage());
        for (int i = 0; i < options.getChoices().size(); i++) {
            System.out.println("  " + (i + 1) + ") " + options.getChoices().get(i));
        }
        
        System.out.print("Select option (1-" + options.getChoices().size() + "): ");
        String input = reader.readLine();
        
        if (input == null || input.trim().isEmpty()) {
            if (options.getDefaultValue() != null) {
                return options.getDefaultValue().toString();
            }
            error("Selection is required");
            return promptList(reader, options);
        }
        
        try {
            int index = Integer.parseInt(input.trim()) - 1;
            if (index >= 0 && index < options.getChoices().size()) {
                return options.getChoices().get(index);
            } else {
                error("Invalid selection");
                return promptList(reader, options);
            }
        } catch (NumberFormatException e) {
            error("Please enter a number");
            return promptList(reader, options);
        }
    }
    
    private List<String> promptCheckbox(BufferedReader reader, PromptOptions<?> options) throws IOException {
        if (options.getChoices() == null || options.getChoices().isEmpty()) {
            throw new IllegalArgumentException("Checkbox prompt requires choices");
        }
        
        System.out.println(options.getMessage());
        for (int i = 0; i < options.getChoices().size(); i++) {
            System.out.println("  " + (i + 1) + ") " + options.getChoices().get(i));
        }
        
        System.out.print("Select options (comma-separated numbers): ");
        String input = reader.readLine();
        
        List<String> selected = new ArrayList<>();
        if (input != null && !input.trim().isEmpty()) {
            String[] selections = input.split(",");
            for (String selection : selections) {
                try {
                    int index = Integer.parseInt(selection.trim()) - 1;
                    if (index >= 0 && index < options.getChoices().size()) {
                        selected.add(options.getChoices().get(index));
                    }
                } catch (NumberFormatException ignored) {
                    // Skip invalid selections
                }
            }
        }
        
        return selected;
    }
    
    private String buildPrompt(PromptOptions<?> options) {
        StringBuilder prompt = new StringBuilder();
        
        if (options.getMessage() != null) {
            prompt.append(options.getMessage());
        }
        
        if (options.getDefaultValue() != null) {
            prompt.append(" (").append(options.getDefaultValue()).append(")");
        }
        
        prompt.append(": ");
        
        return prompt.toString();
    }
    
    private SpinnerInstance createDummySpinner(String text) {
        return new SpinnerInstance() {
            @Override
            public SpinnerInstance update(String text) {
                return this;
            }
            
            @Override
            public SpinnerInstance succeed(String text) {
                if (text != null) success(text);
                return this;
            }
            
            @Override
            public SpinnerInstance fail(String text) {
                if (text != null) error(text);
                return this;
            }
            
            @Override
            public SpinnerInstance warn(String text) {
                if (text != null) warn(text);
                return this;
            }
            
            @Override
            public SpinnerInstance info(String text) {
                if (text != null) info(text);
                return this;
            }
            
            @Override
            public SpinnerInstance stop() {
                return this;
            }
        };
    }
    
    // Simple spinner implementation
    private static class SimpleSpinner implements SpinnerInstance {
        private final String id;
        private String text;
        private final Terminal terminal;
        private volatile boolean stopped = false;
        
        public SimpleSpinner(String id, String text, Terminal terminal) {
            this.id = id;
            this.text = text;
            this.terminal = terminal;
            start();
        }
        
        private void start() {
            if (terminal.isInteractive() && terminal.supportsColors()) {
                System.out.print(text + " ");
                System.out.flush();
            } else {
                terminal.info(text);
            }
        }
        
        @Override
        public SpinnerInstance update(String text) {
            this.text = text;
            if (terminal.isInteractive() && terminal.supportsColors() && !stopped) {
                System.out.print("\r" + text + " ");
                System.out.flush();
            }
            return this;
        }
        
        @Override
        public SpinnerInstance succeed(String text) {
            stop();
            if (text != null) {
                terminal.success(text);
            }
            return this;
        }
        
        @Override
        public SpinnerInstance fail(String text) {
            stop();
            if (text != null) {
                terminal.error(text);
            }
            return this;
        }
        
        @Override
        public SpinnerInstance warn(String text) {
            stop();
            if (text != null) {
                terminal.warn(text);
            }
            return this;
        }
        
        @Override
        public SpinnerInstance info(String text) {
            stop();
            if (text != null) {
                terminal.info(text);
            }
            return this;
        }
        
        @Override
        public SpinnerInstance stop() {
            if (!stopped) {
                stopped = true;
                if (terminal.isInteractive() && terminal.supportsColors()) {
                    System.out.print("\r");
                    System.out.flush();
                }
            }
            return this;
        }
    }
}
