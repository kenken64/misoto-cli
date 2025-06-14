package sg.edu.nus.iss.misoto.cli.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sg.edu.nus.iss.misoto.cli.execution.ExecutionEnvironment;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring Configuration for Execution Environment
 * 
 * Provides configuration beans for the execution environment
 * including ExecutionConfig settings.
 */
@Configuration
@Slf4j
public class ExecutionConfiguration {
    
    @Autowired
    private ConfigManager configManager;    /**
     * Provides ExecutionConfig bean for ExecutionEnvironment
     */
    @Bean
    public ExecutionEnvironment.ExecutionConfig executionConfig() {
        ExecutionEnvironment.ExecutionConfig execConfig = new ExecutionEnvironment.ExecutionConfig();
        
        // Set default shell (will be overridden if configuration is available)
        String defaultShell = System.getProperty("os.name").toLowerCase().contains("windows") ? "cmd" : "bash";
        execConfig.setShell(defaultShell);
        
        // Try to get configuration if available
        try {
            if (configManager.isConfigLoaded()) {
                ApplicationConfig appConfig = configManager.getConfig();
                execConfig.setShell(appConfig.getExecutionShell());
            }
        } catch (Exception e) {
            // Configuration not loaded yet, use defaults
            log.warn("Configuration not available, using default execution settings: {}", e.getMessage());
        }
        
        // Configure default settings
        execConfig.setAllowedCommands(getDefaultAllowedCommands());
        execConfig.setBlockedCommands(getDefaultBlockedCommands());
        execConfig.setEnableSafetyChecks(true);
        execConfig.setDefaultTimeout(30000); // 30 seconds
        execConfig.setMaxConcurrentProcesses(5);
        
        return execConfig;
    }
    
    /**
     * Get default allowed commands
     */
    private List<String> getDefaultAllowedCommands() {
        List<String> allowedCommands = new ArrayList<>();
        
        // Basic system commands
        allowedCommands.add("ls");
        allowedCommands.add("dir");
        allowedCommands.add("pwd");
        allowedCommands.add("cd");
        allowedCommands.add("echo");
        allowedCommands.add("cat");
        allowedCommands.add("type");
        allowedCommands.add("head");
        allowedCommands.add("tail");
        allowedCommands.add("grep");
        allowedCommands.add("find");
        allowedCommands.add("where");
        allowedCommands.add("which");
        
        // Development tools
        allowedCommands.add("git");
        allowedCommands.add("mvn");
        allowedCommands.add("gradle");
        allowedCommands.add("npm");
        allowedCommands.add("yarn");
        allowedCommands.add("node");
        allowedCommands.add("python");
        allowedCommands.add("java");
        allowedCommands.add("javac");
        
        // File operations (safe)
        allowedCommands.add("cp");
        allowedCommands.add("copy");
        allowedCommands.add("mv");
        allowedCommands.add("move");
        allowedCommands.add("mkdir");
        allowedCommands.add("md");
        allowedCommands.add("touch");
        
        return allowedCommands;
    }
    
    /**
     * Get default blocked commands
     */
    private List<String> getDefaultBlockedCommands() {
        List<String> blockedCommands = new ArrayList<>();
        
        // System modification commands
        blockedCommands.add("sudo");
        blockedCommands.add("su");
        blockedCommands.add("chmod");
        blockedCommands.add("chown");
        blockedCommands.add("passwd");
        
        // Dangerous file operations
        blockedCommands.add("rmdir");
        blockedCommands.add("rd");
        blockedCommands.add("format");
        blockedCommands.add("fdisk");
        blockedCommands.add("parted");
        blockedCommands.add("mkfs");
        blockedCommands.add("dd");
        
        // Network and system control
        blockedCommands.add("reboot");
        blockedCommands.add("shutdown");
        blockedCommands.add("halt");
        blockedCommands.add("poweroff");
        blockedCommands.add("systemctl");
        blockedCommands.add("service");
        
        return blockedCommands;
    }
}
