package sg.edu.nus.iss.misoto.cli.commands.impl;

import sg.edu.nus.iss.misoto.cli.commands.Command;
import sg.edu.nus.iss.misoto.cli.auth.AuthManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Command for user logout
 */
@Component
@Slf4j
public class LogoutCommand implements Command {
    
    @Autowired
    private AuthManager authManager;
    
    @Override
    public String getName() {
        return "logout";
    }
    
    @Override
    public String getDescription() {
        return "Clear authentication credentials";
    }
    
    @Override
    public String getCategory() {
        return "Authentication";
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
        return "claude-code logout";
    }
    
    @Override
    public List<String> getExamples() {
        return List.of("claude-code logout");
    }
    
    @Override
    public void execute(List<String> args) throws Exception {
        try {
            authManager.clearToken();
            System.out.println("Successfully logged out.");
            System.out.println("Use 'claude-code login' to authenticate again.");
        } catch (Exception e) {
            System.err.println("Warning: Failed to clear authentication token: " + e.getMessage());
            System.out.println("You have been logged out, but the token file may still exist.");
        }
    }
}
