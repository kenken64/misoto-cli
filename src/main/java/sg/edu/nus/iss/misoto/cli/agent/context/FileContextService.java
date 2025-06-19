package sg.edu.nus.iss.misoto.cli.agent.context;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.misoto.cli.agent.planning.SubTask;
import sg.edu.nus.iss.misoto.cli.fileops.FileOperations;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for managing file context during agent operations.
 * Handles reading existing file content, preserving context during modifications,
 * and intelligent content merging.
 */
@Service
@Slf4j
public class FileContextService {
    
    @Autowired
    private FileOperations fileOperations;
    
    /**
     * Load file context for a subtask that involves file operations
     */
    public SubTask loadFileContext(SubTask subTask) {
        if (subTask.getFilePath() == null || subTask.getFilePath().trim().isEmpty()) {
            return subTask; // No file operation, return as-is
        }
        
        String filePath = subTask.getFilePath();
        log.debug("Loading file context for: {}", filePath);
        
        try {
            // Check if file exists
            boolean exists = fileOperations.fileExists(filePath);
            subTask.setFileExists(exists);
            
            if (exists) {
                // Read existing content
                String originalContent = fileOperations.readTextFile(filePath);
                subTask.setOriginalFileContent(originalContent);
                
                // Determine operation mode if not set
                if (subTask.getOperationMode() == null) {
                    subTask.setOperationMode(determineOperationMode(subTask, originalContent));
                }
                
                // Set preserve context flag based on operation mode
                if (subTask.getOperationMode() == SubTask.FileOperationMode.MODIFY || 
                    subTask.getOperationMode() == SubTask.FileOperationMode.AUTO) {
                    subTask.setPreserveContext(true);
                }
                
                log.info("📁 File context loaded for {}: {} bytes, mode: {}", 
                        filePath, originalContent.length(), subTask.getOperationMode());
            } else {
                // File doesn't exist, set to CREATE mode
                if (subTask.getOperationMode() == null) {
                    subTask.setOperationMode(SubTask.FileOperationMode.CREATE);
                }
                subTask.setPreserveContext(false);
                
                log.info("📄 New file operation for: {}", filePath);
            }
            
        } catch (Exception e) {
            log.error("Error loading file context for {}: {}", filePath, e.getMessage());
            // Continue with operation, but mark as no context preservation
            subTask.setFileExists(false);
            subTask.setPreserveContext(false);
            if (subTask.getOperationMode() == null) {
                subTask.setOperationMode(SubTask.FileOperationMode.CREATE);
            }
        }
        
        return subTask;
    }
    
    /**
     * Determine the appropriate file operation mode based on the task and existing content
     */
    private SubTask.FileOperationMode determineOperationMode(SubTask subTask, String originalContent) {
        // If mode is explicitly set, use it
        if (subTask.getOperationMode() != null && subTask.getOperationMode() != SubTask.FileOperationMode.AUTO) {
            return subTask.getOperationMode();
        }
        
        // Analyze the task description and new content to determine best mode
        String description = subTask.getDescription() != null ? subTask.getDescription().toLowerCase() : "";
        String newContent = subTask.getFileContent();
        
        // Explicit operation keywords in description
        if (description.contains("replace") || description.contains("overwrite") || description.contains("rewrite")) {
            return SubTask.FileOperationMode.REPLACE;
        }
        if (description.contains("append") || description.contains("add to") || description.contains("add at end")) {
            return SubTask.FileOperationMode.APPEND;
        }
        if (description.contains("modify") || description.contains("update") || description.contains("change") || 
            description.contains("edit") || description.contains("fix")) {
            return SubTask.FileOperationMode.MODIFY;
        }
        
        // Analyze content patterns
        if (newContent != null && originalContent != null) {
            // If new content is much smaller than original, likely a modification
            if (newContent.length() < originalContent.length() * 0.8) {
                return SubTask.FileOperationMode.MODIFY;
            }
            
            // If new content contains parts of original content, likely a modification
            if (containsSignificantOverlap(originalContent, newContent)) {
                return SubTask.FileOperationMode.MODIFY;
            }
            
            // If new content is completely different and larger, likely a replacement
            if (newContent.length() > originalContent.length() * 1.5) {
                return SubTask.FileOperationMode.REPLACE;
            }
        }
        
        // Default to MODIFY for existing files to preserve context
        return SubTask.FileOperationMode.MODIFY;
    }
    
    /**
     * Check if there's significant overlap between original and new content
     */
    private boolean containsSignificantOverlap(String original, String newContent) {
        if (original == null || newContent == null || original.length() < 50) {
            return false;
        }
        
        // Look for common lines or significant text blocks
        String[] originalLines = original.split("\n");
        String[] newLines = newContent.split("\n");
        
        int matchingLines = 0;
        for (String newLine : newLines) {
            String trimmedNew = newLine.trim();
            if (trimmedNew.length() > 10) { // Only consider substantial lines
                for (String originalLine : originalLines) {
                    if (originalLine.trim().equals(trimmedNew)) {
                        matchingLines++;
                        break;
                    }
                }
            }
        }
        
        // If more than 20% of meaningful lines match, consider it an overlap
        return matchingLines > Math.max(1, newLines.length * 0.2);
    }
    
    /**
     * Merge new content with existing content based on operation mode
     */
    public String mergeContent(SubTask subTask) {
        if (!subTask.isFileExists() || !subTask.isPreserveContext()) {
            // No existing content or no preservation needed
            return subTask.getFileContent() != null ? subTask.getFileContent() : "";
        }
        
        String originalContent = subTask.getOriginalFileContent();
        String newContent = subTask.getFileContent();
        SubTask.FileOperationMode mode = subTask.getOperationMode();
        
        if (originalContent == null) {
            return newContent != null ? newContent : "";
        }
        
        if (newContent == null) {
            return originalContent;
        }
        
        return switch (mode) {
            case REPLACE -> newContent;
            case APPEND -> originalContent + "\n" + newContent;
            case MODIFY -> intelligentMerge(originalContent, newContent, subTask);
            case CREATE -> newContent; // Should not happen for existing files
            case AUTO -> intelligentMerge(originalContent, newContent, subTask);
        };
    }
    
    /**
     * Perform intelligent merging of content based on file type and content analysis
     */
    private String intelligentMerge(String originalContent, String newContent, SubTask subTask) {
        String filePath = subTask.getFilePath();
        
        // Determine file type
        String extension = getFileExtension(filePath);
        
        // For code files, try to do intelligent merging
        if (isCodeFile(extension)) {
            return mergeCodeFile(originalContent, newContent, extension, subTask);
        }
        
        // For configuration files, try intelligent merging
        if (isConfigFile(extension)) {
            return mergeConfigFile(originalContent, newContent, extension);
        }
        
        // For text files, use line-based merging
        if (isTextFile(extension)) {
            return mergeTextFile(originalContent, newContent);
        }
        
        // Default: append new content to original
        return originalContent + "\n\n" + newContent;
    }
    
    /**
     * Merge code files intelligently
     */
    private String mergeCodeFile(String originalContent, String newContent, String extension, SubTask subTask) {
        // For Python files, try to merge functions/classes
        if ("py".equals(extension)) {
            return mergePythonFile(originalContent, newContent, subTask);
        }
        
        // For Java files, try to merge methods/classes
        if ("java".equals(extension)) {
            return mergeJavaFile(originalContent, newContent, subTask);
        }
        
        // For other code files, use basic line-based merging
        return mergeTextFile(originalContent, newContent);
    }
    
    /**
     * Merge Python files by detecting functions and classes
     */
    private String mergePythonFile(String originalContent, String newContent, SubTask subTask) {
        // Look for function and class definitions in new content
        Pattern functionPattern = Pattern.compile("^def\\s+([\\w_]+)\\s*\\(", Pattern.MULTILINE);
        Pattern classPattern = Pattern.compile("^class\\s+([\\w_]+)\\s*[\\(:]", Pattern.MULTILINE);
        
        // If new content contains complete function/class definitions, merge them
        Matcher functionMatcher = functionPattern.matcher(newContent);
        Matcher classMatcher = classPattern.matcher(newContent);
        
        if (functionMatcher.find() || classMatcher.find()) {
            // New content has function/class definitions, append to original
            return originalContent + "\n\n# " + subTask.getDescription() + "\n" + newContent;
        }
        
        // Otherwise, use basic text merging
        return mergeTextFile(originalContent, newContent);
    }
    
    /**
     * Merge Java files by detecting methods and classes
     */
    private String mergeJavaFile(String originalContent, String newContent, SubTask subTask) {
        // Look for method and class definitions
        Pattern methodPattern = Pattern.compile("(public|private|protected)?\\s*(static)?\\s*\\w+\\s+\\w+\\s*\\(", Pattern.MULTILINE);
        Pattern classPattern = Pattern.compile("(public|private)?\\s*(class|interface|enum)\\s+\\w+", Pattern.MULTILINE);
        
        Matcher methodMatcher = methodPattern.matcher(newContent);
        Matcher classMatcher = classPattern.matcher(newContent);
        
        if (methodMatcher.find() || classMatcher.find()) {
            // Find insertion point (before the last closing brace)
            int lastBrace = originalContent.lastIndexOf("}");
            if (lastBrace != -1) {
                return originalContent.substring(0, lastBrace) + 
                       "\n    // " + subTask.getDescription() + "\n" + 
                       newContent + "\n" + 
                       originalContent.substring(lastBrace);
            }
        }
        
        // Fallback to appending
        return originalContent + "\n\n// " + subTask.getDescription() + "\n" + newContent;
    }
    
    /**
     * Merge configuration files
     */
    private String mergeConfigFile(String originalContent, String newContent, String extension) {
        // For properties files, merge key-value pairs
        if ("properties".equals(extension)) {
            return mergePropertiesFile(originalContent, newContent);
        }
        
        // For JSON files, try to merge objects
        if ("json".equals(extension)) {
            return mergeJsonFile(originalContent, newContent);
        }
        
        // Default: append
        return originalContent + "\n" + newContent;
    }
    
    /**
     * Merge properties files by combining key-value pairs
     */
    private String mergePropertiesFile(String originalContent, String newContent) {
        // Simple approach: append new properties
        // TODO: Could be enhanced to replace existing keys
        return originalContent + "\n" + newContent;
    }
    
    /**
     * Merge JSON files (basic implementation)
     */
    private String mergeJsonFile(String originalContent, String newContent) {
        // Basic approach: append as comment
        // TODO: Could use JSON parsing for intelligent merging
        return originalContent + "\n// Additional configuration:\n// " + newContent;
    }
    
    /**
     * Merge text files using line-based approach
     */
    private String mergeTextFile(String originalContent, String newContent) {
        // Simple approach: append with separator
        return originalContent + "\n\n" + newContent;
    }
    
    // Utility methods
    
    private String getFileExtension(String filePath) {
        if (filePath == null) return "";
        int lastDot = filePath.lastIndexOf('.');
        return lastDot > 0 ? filePath.substring(lastDot + 1).toLowerCase() : "";
    }
    
    private boolean isCodeFile(String extension) {
        return List.of("py", "java", "js", "ts", "cpp", "c", "h", "cs", "php", "rb", "go", "rs").contains(extension);
    }
    
    private boolean isConfigFile(String extension) {
        return List.of("properties", "json", "yml", "yaml", "xml", "conf", "cfg", "ini").contains(extension);
    }
    
    private boolean isTextFile(String extension) {
        return List.of("txt", "md", "rst", "log", "csv").contains(extension);
    }
    
    /**
     * Create a backup of the original file before modification
     */
    public boolean createBackup(String filePath) {
        try {
            if (!fileOperations.fileExists(filePath)) {
                return true; // No file to backup
            }
            
            String backupPath = filePath + ".backup_" + System.currentTimeMillis();
            fileOperations.copyFile(filePath, backupPath);
            log.info("💾 Created backup: {}", backupPath);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to create backup for {}: {}", filePath, e.getMessage());
            return false;
        }
    }
    
    /**
     * Analyze file content to provide context for AI decision making
     */
    public String analyzeFileForAI(String filePath) {
        try {
            if (!fileOperations.fileExists(filePath)) {
                return "File does not exist: " + filePath;
            }
            
            String content = fileOperations.readTextFile(filePath);
            long size = fileOperations.getFileSize(filePath);
            String extension = getFileExtension(filePath);
            
            StringBuilder analysis = new StringBuilder();
            analysis.append(String.format("File: %s (%s, %d bytes)\n", filePath, extension, size));
            
            // Analyze content structure
            String[] lines = content.split("\n");
            analysis.append(String.format("Lines: %d\n", lines.length));
            
            if (isCodeFile(extension)) {
                analysis.append("File type: Code file\n");
                analyzeCodeStructure(content, extension, analysis);
            } else if (isConfigFile(extension)) {
                analysis.append("File type: Configuration file\n");
            } else {
                analysis.append("File type: Text file\n");
            }
            
            // Show first few lines for context
            analysis.append("\nContent preview:\n");
            for (int i = 0; i < Math.min(5, lines.length); i++) {
                analysis.append(String.format("  %d: %s\n", i + 1, lines[i]));
            }
            if (lines.length > 5) {
                analysis.append("  ... (").append(lines.length - 5).append(" more lines)\n");
            }
            
            return analysis.toString();
            
        } catch (Exception e) {
            return "Error analyzing file " + filePath + ": " + e.getMessage();
        }
    }
    
    /**
     * Analyze code structure for AI context
     */
    private void analyzeCodeStructure(String content, String extension, StringBuilder analysis) {
        if ("py".equals(extension)) {
            analyzePythonStructure(content, analysis);
        } else if ("java".equals(extension)) {
            analyzeJavaStructure(content, analysis);
        }
    }
    
    private void analyzePythonStructure(String content, StringBuilder analysis) {
        Pattern functionPattern = Pattern.compile("^def\\s+([\\w_]+)", Pattern.MULTILINE);
        Pattern classPattern = Pattern.compile("^class\\s+([\\w_]+)", Pattern.MULTILINE);
        
        Matcher functionMatcher = functionPattern.matcher(content);
        Matcher classMatcher = classPattern.matcher(content);
        
        long functionCount = functionMatcher.results().count();
        long classCount = classMatcher.results().count();
        
        analysis.append(String.format("Python structure: %d classes, %d functions\n", classCount, functionCount));
    }
    
    private void analyzeJavaStructure(String content, StringBuilder analysis) {
        Pattern classPattern = Pattern.compile("(class|interface|enum)\\s+(\\w+)", Pattern.MULTILINE);
        Pattern methodPattern = Pattern.compile("(public|private|protected).*?\\s+(\\w+)\\s*\\(", Pattern.MULTILINE);
        
        Matcher classMatcher = classPattern.matcher(content);
        Matcher methodMatcher = methodPattern.matcher(content);
        
        long classCount = classMatcher.results().count();
        long methodCount = methodMatcher.results().count();
        
        analysis.append(String.format("Java structure: %d classes/interfaces, %d methods\n", classCount, methodCount));
    }
}