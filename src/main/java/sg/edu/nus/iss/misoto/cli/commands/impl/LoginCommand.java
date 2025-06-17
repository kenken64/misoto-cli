package sg.edu.nus.iss.misoto.cli.commands.impl;

import sg.edu.nus.iss.misoto.cli.commands.Command;
import sg.edu.nus.iss.misoto.cli.auth.AuthManager;
import sg.edu.nus.iss.misoto.cli.errors.UserError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Scanner;

/**
 * Command for user authentication
 */
@Component
@Slf4j
public class LoginCommand implements Command {
    
    @Autowired
    private AuthManager authManager;
    
    @Override
    public String getName() {
        return "login";
    }
    
    @Override
    public String getDescription() {
        return "Authenticate with Claude AI API";
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
        return "claude-code login [--token <api-token>]";
    }
    
    @Override
    public List<String> getExamples() {
        return List.of(
            "claude-code login",
            "claude-code login --token sk-ant-api03-..."
        );
    }
    
    @Override
    public void execute(List<String> args) throws Exception {
        String token = null;
        
        // Check if token is provided as argument
        for (int i = 0; i < args.size(); i++) {
            if ("--token".equals(args.get(i)) && i + 1 < args.size()) {
                token = args.get(i + 1);
                break;
            }
        }
        
        // If no token provided, prompt for it
        if (token == null) {
            System.out.println("Please enter your Claude API token:");
            System.out.println("You can get your API token from: https://console.anthropic.com/");
            System.out.print("API Token: ");
            
            Scanner scanner = new Scanner(System.in);
            token = scanner.nextLine().trim();
        }
        
        if (token.isEmpty()) {
            throw new UserError("API token cannot be empty");
        }
        
        // Validate token format (basic check)
        if (!token.startsWith("sk-ant-")) {
            throw new UserError("Invalid API token format. Claude API tokens should start with 'sk-ant-'");
        }
        
        try {
            // Save the token
            authManager.setToken(token);
            
            System.out.println("Successfully authenticated with Claude AI!");
            System.out.println("You can now use commands that require AI assistance.");
            
        } catch (Exception e) {
            throw new UserError("Failed to save authentication token: " + e.getMessage());
        }
    }
}
