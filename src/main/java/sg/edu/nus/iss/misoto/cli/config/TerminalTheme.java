package sg.edu.nus.iss.misoto.cli.config;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Terminal Theme Enumeration
 */
public enum TerminalTheme {
    DARK("dark"),
    LIGHT("light"),
    SYSTEM("system");
    
    private final String value;
    
    TerminalTheme(String value) {
        this.value = value;
    }
    
    @JsonValue
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
    
    /**
     * Parse terminal theme from string
     */
    public static TerminalTheme fromString(String value) {
        if (value == null) return SYSTEM;
        
        for (TerminalTheme theme : values()) {
            if (theme.value.equalsIgnoreCase(value)) {
                return theme;
            }
        }
        
        return SYSTEM; // Default fallback
    }
    
    /**
     * Resolve the actual theme based on system preference
     */
    public TerminalTheme resolve() {
        if (this == SYSTEM) {
            // Try to detect system theme preference
            // This is a simplified detection - in reality, you might check
            // system properties, environment variables, or OS-specific settings
            String osTheme = System.getProperty("os.theme", "dark");
            return "light".equalsIgnoreCase(osTheme) ? LIGHT : DARK;
        }
        return this;
    }
}
