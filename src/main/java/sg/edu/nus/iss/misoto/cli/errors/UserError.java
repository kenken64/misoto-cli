package sg.edu.nus.iss.misoto.cli.errors;

/**
 * Exception for user-facing errors
 */
public class UserError extends RuntimeException {
    
    public UserError(String message) {
        super(message);
    }
    
    public UserError(String message, Throwable cause) {
        super(message, cause);
    }
}
