package sg.edu.nus.iss.misoto.cli.commands.impl;

import sg.edu.nus.iss.misoto.cli.commands.Command;
import sg.edu.nus.iss.misoto.cli.auth.AuthManager;
import sg.edu.nus.iss.misoto.cli.utils.FormattingUtil;
import sg.edu.nus.iss.misoto.cli.utils.ValidationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Command for showing system and application status
 */
@Component
@Slf4j
public class StatusCommand implements Command {
    
    @Autowired
    private AuthManager authManager;
    
    @Override
    public String getName() {
        return "status";
    }
    
    @Override
    public String getDescription() {
        return "Show system and application status information";
    }
    
    @Override
    public String getCategory() {
        return "System";
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
        return "claude-code status [--verbose]";
    }
    
    @Override
    public List<String> getExamples() {
        return List.of(
            "claude-code status",
            "claude-code status --verbose"
        );
    }
    
    @Override
    public void execute(List<String> args) throws Exception {
        boolean verbose = args.contains("--verbose") || args.contains("-v");
        
        System.out.println(FormattingUtil.formatWithColor("Claude Code CLI Status", FormattingUtil.ANSI_CYAN + FormattingUtil.ANSI_BOLD));
        System.out.println(FormattingUtil.formatWithColor("=======================", FormattingUtil.ANSI_CYAN));
        System.out.println();
        
        // Authentication Status
        displayAuthStatus();
        System.out.println();
        
        // System Information
        displaySystemInfo(verbose);
        System.out.println();
        
        // Application Information
        displayAppInfo(verbose);
        System.out.println();
        
        // Working Directory Info
        displayWorkingDirInfo();
        
        if (verbose) {
            System.out.println();
            displayDetailedSystemInfo();
        }
        
        System.out.println();
        System.out.println(FormattingUtil.formatWithColor("Status check completed at: " + 
            FormattingUtil.formatDate(LocalDateTime.now()), FormattingUtil.ANSI_BLUE));
    }
    
    private void displayAuthStatus() {
        System.out.println(FormattingUtil.formatWithColor("Authentication:", FormattingUtil.ANSI_GREEN + FormattingUtil.ANSI_BOLD));
        
        try {
            boolean isAuthenticated = authManager.isAuthenticated();
            if (isAuthenticated) {
                String user = authManager.getCurrentUser().orElse("Unknown");
                System.out.println("  Status: " + FormattingUtil.formatWithColor("✓ Authenticated", FormattingUtil.ANSI_GREEN));
                System.out.println("  User: " + FormattingUtil.formatWithColor(user, FormattingUtil.ANSI_CYAN));
            } else {
                System.out.println("  Status: " + FormattingUtil.formatWithColor("✗ Not authenticated", FormattingUtil.ANSI_RED));
                System.out.println("  " + FormattingUtil.formatWithColor("Use 'claude-code login' to authenticate", FormattingUtil.ANSI_YELLOW));
            }
        } catch (Exception e) {
            System.out.println("  Status: " + FormattingUtil.formatWithColor("⚠ Error checking auth status", FormattingUtil.ANSI_YELLOW));
            log.debug("Error checking authentication status", e);
        }
    }
    
    private void displaySystemInfo(boolean verbose) {
        System.out.println(FormattingUtil.formatWithColor("System Information:", FormattingUtil.ANSI_GREEN + FormattingUtil.ANSI_BOLD));
        
        // Java Information
        String javaVersion = System.getProperty("java.version");
        String javaVendor = System.getProperty("java.vendor");
        System.out.println("  Java Version: " + FormattingUtil.formatWithColor(javaVersion, FormattingUtil.ANSI_CYAN));
        if (verbose) {
            System.out.println("  Java Vendor: " + FormattingUtil.formatWithColor(javaVendor, FormattingUtil.ANSI_CYAN));
        }
        
        // OS Information
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String osArch = System.getProperty("os.arch");
        System.out.println("  Operating System: " + FormattingUtil.formatWithColor(osName + " " + osVersion, FormattingUtil.ANSI_CYAN));
        if (verbose) {
            System.out.println("  Architecture: " + FormattingUtil.formatWithColor(osArch, FormattingUtil.ANSI_CYAN));
        }
        
        // Memory Information
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
        
        System.out.println("  Memory Usage: " + FormattingUtil.formatWithColor(
            FormattingUtil.formatFileSize(usedMemory) + " / " + FormattingUtil.formatFileSize(maxMemory),
            FormattingUtil.ANSI_CYAN
        ));
    }
    
    private void displayAppInfo(boolean verbose) {
        System.out.println(FormattingUtil.formatWithColor("Application Information:", FormattingUtil.ANSI_GREEN + FormattingUtil.ANSI_BOLD));
        
        // Runtime Information
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        long uptime = runtimeBean.getUptime();
        
        System.out.println("  Uptime: " + FormattingUtil.formatWithColor(FormattingUtil.formatDuration(uptime), FormattingUtil.ANSI_CYAN));
        
        if (verbose) {
            String vmName = runtimeBean.getVmName();
            String vmVersion = runtimeBean.getVmVersion();
            System.out.println("  JVM: " + FormattingUtil.formatWithColor(vmName + " " + vmVersion, FormattingUtil.ANSI_CYAN));
            
            // Available processors
            int processors = Runtime.getRuntime().availableProcessors();
            System.out.println("  Available Processors: " + FormattingUtil.formatWithColor(String.valueOf(processors), FormattingUtil.ANSI_CYAN));
        }
    }
    
    private void displayWorkingDirInfo() {
        System.out.println(FormattingUtil.formatWithColor("Working Directory:", FormattingUtil.ANSI_GREEN + FormattingUtil.ANSI_BOLD));
        
        String currentDir = System.getProperty("user.dir");
        System.out.println("  Path: " + FormattingUtil.formatWithColor(currentDir, FormattingUtil.ANSI_CYAN));
        
        try {
            Path currentPath = Paths.get(currentDir);
            if (Files.exists(currentPath)) {
                // Check for common project files
                checkProjectFile(currentPath, "pom.xml", "Maven Project");
                checkProjectFile(currentPath, "build.gradle", "Gradle Project");
                checkProjectFile(currentPath, "package.json", "Node.js Project");
                checkProjectFile(currentPath, "requirements.txt", "Python Project");
                checkProjectFile(currentPath, ".git", "Git Repository");
            }
        } catch (Exception e) {
            log.debug("Error checking working directory", e);
        }
    }
    
    private void checkProjectFile(Path basePath, String fileName, String projectType) {
        Path filePath = basePath.resolve(fileName);
        if (Files.exists(filePath)) {
            System.out.println("  " + FormattingUtil.formatWithColor("✓ " + projectType + " detected", FormattingUtil.ANSI_GREEN));
        }
    }
    
    private void displayDetailedSystemInfo() {
        System.out.println(FormattingUtil.formatWithColor("Detailed System Information:", FormattingUtil.ANSI_GREEN + FormattingUtil.ANSI_BOLD));
        
        // Environment Variables (selected ones)
        String[] envVars = {"JAVA_HOME", "PATH", "USER", "HOME"};
        for (String envVar : envVars) {
            String value = System.getenv(envVar);
            if (ValidationUtil.isNonEmptyString(value)) {
                String displayValue = envVar.equals("PATH") ? FormattingUtil.truncate(value, 100) : value;
                System.out.println("  " + envVar + ": " + FormattingUtil.formatWithColor(displayValue, FormattingUtil.ANSI_CYAN));
            }
        }
        
        // System Properties (selected ones)
        String[] sysProp = {"user.name", "user.home", "java.class.path"};
        for (String prop : sysProp) {
            String value = System.getProperty(prop);
            if (ValidationUtil.isNonEmptyString(value)) {
                String displayValue = prop.equals("java.class.path") ? FormattingUtil.truncate(value, 100) : value;
                System.out.println("  " + prop + ": " + FormattingUtil.formatWithColor(displayValue, FormattingUtil.ANSI_CYAN));
            }
        }
    }
}
