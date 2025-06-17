package sg.edu.nus.iss.misoto.cli.commands.impl;

import sg.edu.nus.iss.misoto.cli.commands.Command;
import sg.edu.nus.iss.misoto.cli.codebase.CodebaseAnalyzer;
import sg.edu.nus.iss.misoto.cli.codebase.ProjectStructure;
import sg.edu.nus.iss.misoto.cli.errors.UserError;
import sg.edu.nus.iss.misoto.cli.utils.FormattingUtil;
import sg.edu.nus.iss.misoto.cli.utils.ValidationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Command for analyzing codebase structure and metrics
 */
@Component
@Slf4j
public class AnalyzeCommand implements Command {
    
    @Autowired
    private CodebaseAnalyzer codebaseAnalyzer;
    
    @Override
    public String getName() {
        return "analyze";
    }
    
    @Override
    public String getDescription() {
        return "Analyze codebase structure, dependencies, and metrics";
    }
    
    @Override
    public String getCategory() {
        return "Code Analysis";
    }
    
    @Override
    public boolean isHidden() {
        return false;
    }
    
    @Override
    public boolean requiresAuth() {
        return false; // Analysis doesn't require authentication
    }
    
    @Override
    public String getUsage() {
        return "claude-code analyze [directory] [--max-files=<number>] [--max-size=<bytes>] [--verbose]";
    }
    
    @Override
    public List<String> getExamples() {
        return List.of(
            "claude-code analyze",
            "claude-code analyze src/",
            "claude-code analyze . --max-files=500",
            "claude-code analyze /path/to/project --verbose"
        );
    }
    
    @Override
    public void execute(List<String> args) throws Exception {
        // Determine target directory
        String targetDirectory = args.isEmpty() ? System.getProperty("user.dir") : args.get(0);
        
        // Validate directory
        if (!ValidationUtil.isNonEmptyString(targetDirectory)) {
            throw new UserError("Directory path cannot be empty");
        }
        
        if (!Files.exists(Paths.get(targetDirectory))) {
            throw new UserError("Directory does not exist: " + targetDirectory);
        }
        
        if (!Files.isDirectory(Paths.get(targetDirectory))) {
            throw new UserError("Path is not a directory: " + targetDirectory);
        }
        
        // Parse options
        CodebaseAnalyzer.AnalysisOptions options = parseOptions(args);
        boolean verbose = args.contains("--verbose") || args.contains("-v");
        
        try {
            System.out.println(FormattingUtil.formatWithColor("Analyzing codebase: " + targetDirectory, FormattingUtil.ANSI_CYAN));
            System.out.println();
            
            // Track timing
            long startTime = System.currentTimeMillis();
            
            // Analyze the codebase
            ProjectStructure structure = codebaseAnalyzer.analyzeCodebase(targetDirectory, options);
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Display results
            displayAnalysisResults(structure, verbose);
            
            // Show timing information
            String formattedDuration = FormattingUtil.formatDuration(duration);
            System.out.println();
            System.out.println(FormattingUtil.formatWithColor("Analysis completed in: " + formattedDuration, FormattingUtil.ANSI_BLUE));
            
        } catch (Exception e) {
            log.error("Failed to analyze codebase: {}", targetDirectory, e);
            throw new UserError("Failed to analyze codebase: " + e.getMessage());
        }
    }
    
    /**
     * Parse command line options
     */
    private CodebaseAnalyzer.AnalysisOptions parseOptions(List<String> args) {
        CodebaseAnalyzer.AnalysisOptions options = new CodebaseAnalyzer.AnalysisOptions();
        
        for (String arg : args) {
            if (arg.startsWith("--max-files=")) {
                try {
                    int maxFiles = Integer.parseInt(arg.substring("--max-files=".length()));
                    options.setMaxFiles(maxFiles);
                } catch (NumberFormatException e) {
                    throw new UserError("Invalid value for --max-files: " + arg);
                }
            } else if (arg.startsWith("--max-size=")) {
                try {
                    long maxSize = Long.parseLong(arg.substring("--max-size=".length()));
                    options.setMaxSizePerFile(maxSize);
                } catch (NumberFormatException e) {
                    throw new UserError("Invalid value for --max-size: " + arg);
                }
            } else if ("--include-hidden".equals(arg)) {
                options.setIncludeHidden(true);
            }
        }
        
        return options;
    }
    
    /**
     * Display analysis results
     */
    private void displayAnalysisResults(ProjectStructure structure, boolean verbose) {
        // Basic statistics
        System.out.println(FormattingUtil.formatWithColor("Codebase Analysis Results", FormattingUtil.ANSI_GREEN + FormattingUtil.ANSI_BOLD));
        System.out.println(FormattingUtil.formatWithColor("========================", FormattingUtil.ANSI_GREEN));
        System.out.println();
        
        System.out.println(FormattingUtil.formatWithColor("Overview:", FormattingUtil.ANSI_CYAN + FormattingUtil.ANSI_BOLD));
        System.out.println("  Root Directory: " + structure.getRoot());
        System.out.println("  Total Files: " + FormattingUtil.formatWithColor(String.valueOf(structure.getTotalFiles()), FormattingUtil.ANSI_YELLOW));
        System.out.println("  Total Lines of Code: " + FormattingUtil.formatWithColor(FormattingUtil.formatNumber(structure.getTotalLinesOfCode()), FormattingUtil.ANSI_YELLOW));
        System.out.println("  Total Dependencies: " + FormattingUtil.formatWithColor(String.valueOf(structure.getDependencies().size()), FormattingUtil.ANSI_YELLOW));
        System.out.println();
        
        // Language breakdown
        System.out.println(FormattingUtil.formatWithColor("Language Breakdown:", FormattingUtil.ANSI_CYAN + FormattingUtil.ANSI_BOLD));
        Map<String, Integer> sortedLanguages = structure.getFilesByLanguage().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    java.util.LinkedHashMap::new
                ));
        
        for (Map.Entry<String, Integer> entry : sortedLanguages.entrySet()) {
            double percentage = (double) entry.getValue() / structure.getTotalFiles() * 100;
            System.out.printf("  %-20s %s files (%.1f%%)%n", 
                entry.getKey() + ":", 
                FormattingUtil.formatWithColor(String.valueOf(entry.getValue()), FormattingUtil.ANSI_YELLOW),
                percentage);
        }
        System.out.println();
        
        if (verbose) {
            // Directory structure
            System.out.println(FormattingUtil.formatWithColor("Directory Structure:", FormattingUtil.ANSI_CYAN + FormattingUtil.ANSI_BOLD));
            for (Map.Entry<String, List<String>> entry : structure.getDirectories().entrySet()) {
                String dirName = entry.getKey().isEmpty() ? "." : entry.getKey();
                System.out.println("  " + FormattingUtil.formatWithColor(dirName + "/", FormattingUtil.ANSI_BLUE) + 
                                 " (" + entry.getValue().size() + " files)");
            }
            System.out.println();
            
            // Top external dependencies
            Map<String, Long> dependencyCounts = structure.getDependencies().stream()
                    .filter(dep -> dep.isExternal())
                    .collect(java.util.stream.Collectors.groupingBy(
                        dep -> dep.getName(),
                        java.util.stream.Collectors.counting()
                    ));
            
            if (!dependencyCounts.isEmpty()) {
                System.out.println(FormattingUtil.formatWithColor("Top External Dependencies:", FormattingUtil.ANSI_CYAN + FormattingUtil.ANSI_BOLD));
                dependencyCounts.entrySet().stream()
                        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                        .limit(10)
                        .forEach(entry -> 
                            System.out.printf("  %-30s %s references%n",
                                entry.getKey(),
                                FormattingUtil.formatWithColor(String.valueOf(entry.getValue()), FormattingUtil.ANSI_YELLOW)
                            )
                        );
                System.out.println();
            }
        }
        
        // Summary
        System.out.println(FormattingUtil.formatWithColor("Summary:", FormattingUtil.ANSI_GREEN + FormattingUtil.ANSI_BOLD));
        if (structure.getTotalFiles() > 0) {
            double avgLinesPerFile = (double) structure.getTotalLinesOfCode() / structure.getTotalFiles();
            System.out.printf("  Average lines per file: %.1f%n", avgLinesPerFile);
            System.out.println("  Primary language: " + 
                FormattingUtil.formatWithColor(
                    sortedLanguages.entrySet().iterator().next().getKey(),
                    FormattingUtil.ANSI_CYAN
                ));
        }
        
        if (!verbose) {
            System.out.println();
            System.out.println(FormattingUtil.formatWithColor("Use --verbose for detailed directory and dependency information", FormattingUtil.ANSI_BLUE));
        }
    }
}
