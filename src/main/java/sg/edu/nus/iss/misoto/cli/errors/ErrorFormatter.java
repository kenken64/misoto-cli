package sg.edu.nus.iss.misoto.cli.errors;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.io.StringWriter;
import java.io.PrintWriter;

/**
 * Service for formatting errors for display
 */
@Service
@Slf4j
public class ErrorFormatter {
    
    /**
     * Format an error for display to the user
     */
    public String formatErrorForDisplay(Exception error) {
        if (error instanceof UserError) {
            return "Error: " + error.getMessage();
        }
        
        // For unexpected errors, provide a generic message but log the full details
        log.error("Unexpected error", error);
        
        StringBuilder formatted = new StringBuilder();
        formatted.append("An unexpected error occurred: ");
        formatted.append(error.getMessage());
        
        // In development mode, you might want to include stack trace
        if (isDevelopmentMode()) {
            formatted.append("\n\nStack trace:\n");
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            error.printStackTrace(pw);
            formatted.append(sw.toString());
        }
        
        return formatted.toString();
    }
    
    /**
     * Check if we're in development mode
     */
    private boolean isDevelopmentMode() {
        // You can configure this based on Spring profiles or system properties
        String profile = System.getProperty("spring.profiles.active", "");
        return "dev".equals(profile) || "development".equals(profile);
    }
}
