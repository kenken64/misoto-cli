package sg.edu.nus.iss.misoto.cli.terminal;

import java.util.List;
import java.util.Map;

/**
 * Terminal Interface
 * 
 * Provides methods for interacting with the terminal, displaying content,
 * managing colors, and handling user input.
 */
public interface TerminalInterface {
    
    /**
     * Clear the terminal screen
     */
    void clear();
    
    /**
     * Display formatted content
     */
    void display(String content);
    
    /**
     * Display a welcome message
     */
    void displayWelcome();
    
    /**
     * Display a message with emphasis
     */
    void emphasize(String message);
    
    /**
     * Display an informational message
     */
    void info(String message);
    
    /**
     * Display a success message
     */
    void success(String message);
    
    /**
     * Display a warning message
     */
    void warn(String message);
    
    /**
     * Display an error message
     */
    void error(String message);
    
    /**
     * Create a clickable link in the terminal if supported
     */
    String link(String text, String url);
    
    /**
     * Display a table of data
     */
    void table(List<List<String>> data, TableOptions options);
    
    /**
     * Prompt user for input
     */
    <T> T prompt(PromptOptions<T> options);
    
    /**
     * Create a progress spinner
     */
    SpinnerInstance spinner(String text);
    
    /**
     * Get terminal dimensions
     */
    TerminalDimensions getDimensions();
    
    /**
     * Check if terminal supports colors
     */
    boolean supportsColors();
    
    /**
     * Check if terminal is interactive
     */
    boolean isInteractive();
    
    /**
     * Terminal dimensions
     */
    class TerminalDimensions {
        private final int width;
        private final int height;
        
        public TerminalDimensions(int width, int height) {
            this.width = width;
            this.height = height;
        }
        
        public int getWidth() { return width; }
        public int getHeight() { return height; }
    }
    
    /**
     * Table display options
     */
    class TableOptions {
        private List<String> headers;
        private boolean showBorder = true;
        private boolean alternateRowColors = false;
        
        public List<String> getHeaders() { return headers; }
        public void setHeaders(List<String> headers) { this.headers = headers; }
        
        public boolean isShowBorder() { return showBorder; }
        public void setShowBorder(boolean showBorder) { this.showBorder = showBorder; }
        
        public boolean isAlternateRowColors() { return alternateRowColors; }
        public void setAlternateRowColors(boolean alternateRowColors) { this.alternateRowColors = alternateRowColors; }
    }
    
    /**
     * Prompt types
     */
    enum PromptType {
        INPUT, PASSWORD, CONFIRM, LIST, CHECKBOX
    }
    
    /**
     * Prompt options
     */
    class PromptOptions<T> {
        private PromptType type;
        private String name;
        private String message;
        private T defaultValue;
        private List<String> choices;
        private boolean required = false;
        
        public PromptType getType() { return type; }
        public void setType(PromptType type) { this.type = type; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public T getDefaultValue() { return defaultValue; }
        public void setDefaultValue(T defaultValue) { this.defaultValue = defaultValue; }
        
        public List<String> getChoices() { return choices; }
        public void setChoices(List<String> choices) { this.choices = choices; }
        
        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }
    }
    
    /**
     * Spinner instance for progress indicators
     */
    interface SpinnerInstance {
        
        /**
         * Update spinner text
         */
        SpinnerInstance update(String text);
        
        /**
         * Mark spinner as successful and stop
         */
        SpinnerInstance succeed(String text);
        
        /**
         * Mark spinner as failed and stop
         */
        SpinnerInstance fail(String text);
        
        /**
         * Mark spinner with warning and stop
         */
        SpinnerInstance warn(String text);
        
        /**
         * Mark spinner with info and stop
         */
        SpinnerInstance info(String text);
        
        /**
         * Stop spinner without any indicator
         */
        SpinnerInstance stop();
    }
}
