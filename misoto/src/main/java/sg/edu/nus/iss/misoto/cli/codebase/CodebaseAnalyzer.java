package sg.edu.nus.iss.misoto.cli.codebase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.misoto.cli.errors.UserError;
import sg.edu.nus.iss.misoto.cli.utils.FormattingUtil;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Codebase Analyzer
 * 
 * Provides utilities for analyzing and understanding code structure,
 * dependencies, and metrics about a codebase.
 */
@Service
@Slf4j
public class CodebaseAnalyzer {
    
    /**
     * Default ignore patterns
     */
    private static final List<String> DEFAULT_IGNORE_PATTERNS = List.of(
        "node_modules", "dist", "build", ".git", ".vscode", ".idea", "coverage",
        "*.min.js", "*.bundle.js", "*.map", "target", "bin", "obj", ".gradle",
        "__pycache__", "*.pyc", ".next", ".nuxt", ".out"
    );
    
    /**
     * Language detection by file extension
     */
    private static final Map<String, String> EXTENSION_TO_LANGUAGE = Map.ofEntries(
        Map.entry("ts", "TypeScript"),
        Map.entry("tsx", "TypeScript (React)"),
        Map.entry("js", "JavaScript"),
        Map.entry("jsx", "JavaScript (React)"),
        Map.entry("py", "Python"),
        Map.entry("java", "Java"),
        Map.entry("c", "C"),
        Map.entry("cpp", "C++"),
        Map.entry("cc", "C++"),
        Map.entry("cxx", "C++"),
        Map.entry("cs", "C#"),
        Map.entry("go", "Go"),
        Map.entry("rs", "Rust"),
        Map.entry("php", "PHP"),
        Map.entry("rb", "Ruby"),
        Map.entry("swift", "Swift"),
        Map.entry("kt", "Kotlin"),
        Map.entry("scala", "Scala"),
        Map.entry("html", "HTML"),
        Map.entry("htm", "HTML"),
        Map.entry("css", "CSS"),
        Map.entry("scss", "SCSS"),
        Map.entry("sass", "SASS"),
        Map.entry("less", "Less"),
        Map.entry("json", "JSON"),
        Map.entry("md", "Markdown"),
        Map.entry("yml", "YAML"),
        Map.entry("yaml", "YAML"),
        Map.entry("xml", "XML"),
        Map.entry("sql", "SQL"),
        Map.entry("sh", "Shell"),
        Map.entry("bash", "Shell"),
        Map.entry("bat", "Batch"),
        Map.entry("cmd", "Batch"),
        Map.entry("ps1", "PowerShell")
    );
    
    /**
     * Options for codebase analysis
     */
    public static class AnalysisOptions {
        private List<String> ignorePatterns = new ArrayList<>(DEFAULT_IGNORE_PATTERNS);
        private int maxFiles = 1000;
        private long maxSizePerFile = 1024 * 1024; // 1MB
        private boolean includeHidden = false;
        
        // Getters and setters
        public List<String> getIgnorePatterns() { return ignorePatterns; }
        public void setIgnorePatterns(List<String> ignorePatterns) { this.ignorePatterns = ignorePatterns; }
        public int getMaxFiles() { return maxFiles; }
        public void setMaxFiles(int maxFiles) { this.maxFiles = maxFiles; }
        public long getMaxSizePerFile() { return maxSizePerFile; }
        public void setMaxSizePerFile(long maxSizePerFile) { this.maxSizePerFile = maxSizePerFile; }
        public boolean isIncludeHidden() { return includeHidden; }
        public void setIncludeHidden(boolean includeHidden) { this.includeHidden = includeHidden; }
    }
    
    /**
     * Analyze a codebase
     */
    public ProjectStructure analyzeCodebase(String directory, AnalysisOptions options) {
        log.info("Analyzing codebase in directory: {}", directory);
        
        Path dirPath = Paths.get(directory);
        if (!Files.exists(dirPath)) {
            throw new UserError("Directory does not exist: " + directory);
        }
        
        if (!Files.isDirectory(dirPath)) {
            throw new UserError("Path is not a directory: " + directory);
        }
        
        ProjectStructure structure = new ProjectStructure();
        structure.setRoot(directory);
        structure.setDependencies(new ArrayList<>());
        
        // Convert ignore patterns to regex patterns
        List<Pattern> ignoreRegexes = compileIgnorePatterns(options.getIgnorePatterns());
        
        try {
            // Find all files
            List<Path> allFiles = findAllFiles(dirPath, ignoreRegexes, options);
            
            // Limit file count if needed
            if (allFiles.size() > options.getMaxFiles()) {
                log.warn("Codebase has too many files ({}), limiting to {} files", 
                        allFiles.size(), options.getMaxFiles());
                allFiles = allFiles.subList(0, options.getMaxFiles());
            }
            
            structure.setTotalFiles(allFiles.size());
            
            // Analyze each file
            analyzeFiles(structure, allFiles, options);
            
            log.info("Codebase analysis complete: {} files analyzed, {} lines of code",
                    structure.getTotalFiles(), structure.getTotalLinesOfCode());
            
        } catch (IOException e) {
            throw new UserError("Failed to analyze codebase: " + e.getMessage());
        }
        
        return structure;
    }
    
    /**
     * Analyze a codebase with default options
     */
    public ProjectStructure analyzeCodebase(String directory) {
        return analyzeCodebase(directory, new AnalysisOptions());
    }
      /**
     * Find files by content search
     */
    public List<FileSearchResult> findFilesByContent(String directory, String searchTerm, 
                                                    FileSearchOptions searchOptions) {
        List<FileSearchResult> results = new ArrayList<>();
        Path dirPath = Paths.get(directory);
        
        if (!Files.exists(dirPath)) {
            throw new UserError("Directory does not exist: " + directory);
        }
        
        Pattern searchPattern = Pattern.compile(searchTerm, 
            searchOptions.isCaseSensitive() ? 0 : Pattern.CASE_INSENSITIVE);
        List<Pattern> ignoreRegexes = compileIgnorePatterns(searchOptions.getIgnorePatterns());
        
        try {
            // Convert FileSearchOptions to AnalysisOptions for file finding
            AnalysisOptions analysisOptions = new AnalysisOptions();
            analysisOptions.setIgnorePatterns(searchOptions.getIgnorePatterns());
            analysisOptions.setIncludeHidden(searchOptions.isIncludeHidden());
            analysisOptions.setMaxSizePerFile(searchOptions.getMaxFileSize());
            
            List<Path> files = findAllFiles(dirPath, ignoreRegexes, analysisOptions);
            
            for (Path file : files) {
                if (results.size() >= searchOptions.getMaxResults()) {
                    break;
                }
                
                // Filter by file extension if specified
                if (!searchOptions.getFileExtensions().isEmpty()) {
                    String ext = getFileExtension(file.toString());
                    if (!searchOptions.getFileExtensions().contains(ext)) {
                        continue;
                    }
                }
                
                searchInFile(file, searchPattern, results, searchOptions.getMaxResults());
            }
            
        } catch (IOException e) {
            throw new UserError("Failed to search files: " + e.getMessage());
        }
        
        return results;
    }
    
    /**
     * Analyze project dependencies from package files
     */
    public Map<String, String> analyzeProjectDependencies(String directory) {
        Map<String, String> dependencies = new HashMap<>();
        Path dirPath = Paths.get(directory);
        
        try {
            // Check for package.json (Node.js)
            Path packageJson = dirPath.resolve("package.json");
            if (Files.exists(packageJson)) {
                parseDependenciesFromPackageJson(packageJson, dependencies);
            }
            
            // Check for pom.xml (Maven)
            Path pomXml = dirPath.resolve("pom.xml");
            if (Files.exists(pomXml)) {
                parseDependenciesFromPomXml(pomXml, dependencies);
            }
            
            // Check for requirements.txt (Python)
            Path requirements = dirPath.resolve("requirements.txt");
            if (Files.exists(requirements)) {
                parseDependenciesFromRequirements(requirements, dependencies);
            }
            
            // Check for Gemfile (Ruby)
            Path gemfile = dirPath.resolve("Gemfile");
            if (Files.exists(gemfile)) {
                parseDependenciesFromGemfile(gemfile, dependencies);
            }
            
        } catch (IOException e) {
            log.warn("Failed to analyze project dependencies", e);
        }
        
        return dependencies;
    }
    
    // Private helper methods
    
    private List<Pattern> compileIgnorePatterns(List<String> patterns) {
        return patterns.stream()
                .map(pattern -> Pattern.compile(
                    pattern.replace(".", "\\.")
                           .replace("*", ".*")
                           .replace("?", ".")))
                .toList();
    }
    
    private List<Path> findAllFiles(Path directory, List<Pattern> ignoreRegexes, 
                                   AnalysisOptions options) throws IOException {
        List<Path> files = new ArrayList<>();
        
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String relativePath = directory.relativize(file).toString();
                
                // Check if file should be ignored
                if (shouldIgnoreFile(relativePath, ignoreRegexes, options)) {
                    return FileVisitResult.CONTINUE;
                }
                
                // Check file size
                if (attrs.size() > options.getMaxSizePerFile()) {
                    log.debug("Skipping file (too large): {} ({})", 
                            file, FormattingUtil.formatFileSize(attrs.size()));
                    return FileVisitResult.CONTINUE;
                }
                
                files.add(file);
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                String relativePath = directory.relativize(dir).toString();
                
                // Skip hidden directories unless explicitly included
                if (!options.isIncludeHidden() && dir.getFileName().toString().startsWith(".")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                
                // Check if directory should be ignored
                if (shouldIgnoreFile(relativePath, ignoreRegexes, options)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                
                return FileVisitResult.CONTINUE;
            }
        });
        
        return files;
    }
    
    private boolean shouldIgnoreFile(String relativePath, List<Pattern> ignoreRegexes, 
                                    AnalysisOptions options) {
        for (Pattern regex : ignoreRegexes) {
            if (regex.matcher(relativePath).matches()) {
                return true;
            }
        }
        return false;
    }
    
    private void analyzeFiles(ProjectStructure structure, List<Path> files, AnalysisOptions options) {
        int processedFiles = 0;
        int skippedFiles = 0;
        
        for (Path file : files) {
            try {
                String relativePath = Paths.get(structure.getRoot()).relativize(file).toString();
                String dirPath = file.getParent() != null 
                    ? Paths.get(structure.getRoot()).relativize(file.getParent()).toString()
                    : "";
                
                // Update directory structure
                structure.getDirectories().computeIfAbsent(dirPath, k -> new ArrayList<>()).add(relativePath);
                
                // Get file info
                String extension = getFileExtension(relativePath);
                String language = EXTENSION_TO_LANGUAGE.getOrDefault(extension, "Other");
                
                // Update language stats
                structure.getFilesByLanguage().merge(language, 1, Integer::sum);
                
                // Read file and count lines if it's a text file
                if (isTextFile(extension)) {
                    try {
                        String content = Files.readString(file);
                        int lineCount = content.split("\n").length;
                        structure.setTotalLinesOfCode(structure.getTotalLinesOfCode() + lineCount);
                        
                        // Find dependencies
                        List<DependencyInfo> dependencies = findDependencies(content, relativePath, extension);
                        structure.getDependencies().addAll(dependencies);
                        
                    } catch (IOException e) {
                        log.debug("Failed to read file for analysis: {}", file, e);
                        skippedFiles++;
                        continue;
                    }
                }
                
                processedFiles++;
                
                // Log progress periodically
                if (processedFiles % 50 == 0) {
                    log.debug("Analyzed {} files...", processedFiles);
                }
                
            } catch (Exception e) {
                log.warn("Failed to analyze file: {}", file, e);
                skippedFiles++;
            }
        }
        
        log.debug("File analysis complete: {} processed, {} skipped", processedFiles, skippedFiles);
    }
    
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1 || lastDot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDot + 1).toLowerCase();
    }
    
    private boolean isTextFile(String extension) {
        return EXTENSION_TO_LANGUAGE.containsKey(extension) || 
               List.of("txt", "md", "rst", "cfg", "conf", "ini", "log").contains(extension);
    }
    
    private List<DependencyInfo> findDependencies(String content, String filePath, String extension) {
        List<DependencyInfo> dependencies = new ArrayList<>();
        
        try {
            switch (extension) {
                case "js", "jsx", "ts", "tsx" -> findJavaScriptDependencies(content, filePath, dependencies);
                case "py" -> findPythonDependencies(content, filePath, dependencies);
                case "java" -> findJavaDependencies(content, filePath, dependencies);
                case "rb" -> findRubyDependencies(content, filePath, dependencies);
                case "go" -> findGoDependencies(content, filePath, dependencies);
            }
        } catch (Exception e) {
            log.warn("Failed to parse dependencies in {}", filePath, e);
        }
        
        return dependencies;
    }
    
    private void findJavaScriptDependencies(String content, String filePath, List<DependencyInfo> dependencies) {
        // ES module imports
        Pattern esImportPattern = Pattern.compile("import\\s+(?:[\\w\\s{},*]*\\s+from\\s+)?['\"]([^'\"]+)['\"]");
        Matcher matcher = esImportPattern.matcher(content);
        while (matcher.find()) {
            String importPath = matcher.group(1);
            dependencies.add(new DependencyInfo(
                getPackageName(importPath), "import", filePath, importPath, isExternalDependency(importPath)
            ));
        }
        
        // Require statements
        Pattern requirePattern = Pattern.compile("(?:const|let|var)\\s+(?:[\\w\\s{},*]*)\\s*=\\s*require\\s*\\(\\s*['\"]([^'\"]+)['\"]\\s*\\)");
        matcher = requirePattern.matcher(content);
        while (matcher.find()) {
            String importPath = matcher.group(1);
            dependencies.add(new DependencyInfo(
                getPackageName(importPath), "require", filePath, importPath, isExternalDependency(importPath)
            ));
        }
    }
    
    private void findPythonDependencies(String content, String filePath, List<DependencyInfo> dependencies) {
        Pattern importPattern = Pattern.compile("^\\s*(?:import\\s+(\\S+)|from\\s+(\\S+)\\s+import)", Pattern.MULTILINE);
        Matcher matcher = importPattern.matcher(content);
        while (matcher.find()) {
            String importPath = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            if (importPath != null) {
                dependencies.add(new DependencyInfo(
                    importPath.split("\\.")[0], "import", filePath, importPath, isExternalPythonModule(importPath)
                ));
            }
        }
    }
    
    private void findJavaDependencies(String content, String filePath, List<DependencyInfo> dependencies) {
        Pattern importPattern = Pattern.compile("^\\s*import\\s+([^;]+);", Pattern.MULTILINE);
        Matcher matcher = importPattern.matcher(content);
        while (matcher.find()) {
            String importPath = matcher.group(1);
            dependencies.add(new DependencyInfo(
                importPath.split("\\.")[0], "import", filePath, importPath, true // Consider all imports as external for Java
            ));
        }
    }
    
    private void findRubyDependencies(String content, String filePath, List<DependencyInfo> dependencies) {
        Pattern requirePattern = Pattern.compile("^\\s*require\\s+['\"]([^'\"]+)['\"]", Pattern.MULTILINE);
        Matcher matcher = requirePattern.matcher(content);
        while (matcher.find()) {
            String importPath = matcher.group(1);
            dependencies.add(new DependencyInfo(
                importPath, "require", filePath, importPath, true // Consider all requires as external for Ruby
            ));
        }
    }
    
    private void findGoDependencies(String content, String filePath, List<DependencyInfo> dependencies) {
        Pattern importPattern = Pattern.compile("import\\s+(?:\\w+\\s+)?\"([^\"]+)\"");
        Matcher matcher = importPattern.matcher(content);
        while (matcher.find()) {
            String importPath = matcher.group(1);
            dependencies.add(new DependencyInfo(
                importPath.split("/")[0], "import", filePath, importPath, !importPath.startsWith(".")
            ));
        }
    }
    
    private String getPackageName(String importPath) {
        if (importPath.startsWith(".") || importPath.startsWith("/")) {
            return "internal";
        }
        
        if (importPath.startsWith("@")) {
            String[] parts = importPath.split("/");
            if (parts.length >= 2) {
                return parts[0] + "/" + parts[1];
            }
        }
        
        return importPath.split("/")[0];
    }
    
    private boolean isExternalDependency(String importPath) {
        return !(importPath.startsWith(".") || importPath.startsWith("/"));
    }
    
    private boolean isExternalPythonModule(String importPath) {
        List<String> stdlibModules = List.of(
            "os", "sys", "re", "math", "datetime", "time", "random", "json", "csv",
            "collections", "itertools", "functools", "pathlib", "shutil", "glob",
            "pickle", "urllib", "http", "logging", "argparse", "unittest", "subprocess",
            "threading", "multiprocessing", "typing", "enum", "io", "tempfile"
        );
        
        String moduleName = importPath.split("\\.")[0];
        return !stdlibModules.contains(moduleName) && !importPath.startsWith(".");
    }
    
    // Additional helper methods for searching and dependency parsing would go here...
      private void searchInFile(Path file, Pattern searchPattern, List<FileSearchResult> results, int maxResults) {
        try {
            String content = Files.readString(file);
            String[] lines = content.split("\n");
            
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (searchPattern.matcher(line).find()) {
                    String relativePath = Paths.get(".").relativize(file).toString();
                    results.add(new FileSearchResult(relativePath, i + 1, line.trim()));
                    
                    if (results.size() >= maxResults) {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            log.debug("Failed to search in file: {}", file, e);
        }
    }
    
    private void parseDependenciesFromPackageJson(Path packageJson, Map<String, String> dependencies) throws IOException {
        // Parse package.json dependencies
        // Implementation would parse JSON and extract dependencies
    }
    
    private void parseDependenciesFromPomXml(Path pomXml, Map<String, String> dependencies) throws IOException {
        // Parse Maven pom.xml dependencies
        // Implementation would parse XML and extract dependencies
    }
    
    private void parseDependenciesFromRequirements(Path requirements, Map<String, String> dependencies) throws IOException {
        // Parse Python requirements.txt
        // Implementation would parse the requirements file
    }
    
    private void parseDependenciesFromGemfile(Path gemfile, Map<String, String> dependencies) throws IOException {
        // Parse Ruby Gemfile
        // Implementation would parse the Gemfile
    }
}
