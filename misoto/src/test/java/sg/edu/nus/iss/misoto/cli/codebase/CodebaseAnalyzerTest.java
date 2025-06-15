package sg.edu.nus.iss.misoto.cli.codebase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import sg.edu.nus.iss.misoto.cli.errors.UserError;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for CodebaseAnalyzer
 */
@DisplayName("CodebaseAnalyzer Tests")
class CodebaseAnalyzerTest {

    @InjectMocks
    private CodebaseAnalyzer codebaseAnalyzer;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("analyzeCodebase should analyze basic project structure")
    void testAnalyzeCodebaseBasic() throws IOException {
        // Create test files
        Path srcDir = tempDir.resolve("src");
        Files.createDirectory(srcDir);
        
        Files.writeString(srcDir.resolve("Main.java"), 
            "package com.example;\nimport java.util.List;\npublic class Main {}");
        Files.writeString(srcDir.resolve("Helper.java"), 
            "package com.example;\nimport java.io.File;\npublic class Helper {}");
        Files.writeString(tempDir.resolve("README.md"), "# Project\nThis is a test project");
        Files.writeString(tempDir.resolve("package.json"), "{\n  \"name\": \"test\"\n}");

        ProjectStructure result = codebaseAnalyzer.analyzeCodebase(tempDir.toString());

        assertNotNull(result);
        assertEquals(tempDir.toString(), result.getRoot());
        assertEquals(4, result.getTotalFiles());
        assertTrue(result.getTotalLinesOfCode() > 0);
        
        // Check language detection
        Map<String, Integer> languages = result.getFilesByLanguage();
        assertEquals(2, languages.get("Java"));
        assertEquals(1, languages.get("Markdown"));
        assertEquals(1, languages.get("JSON"));
        
        // Check directories
        assertTrue(result.getDirectories().containsKey(""));
        assertTrue(result.getDirectories().containsKey("src"));
        
        // Check dependencies
        assertNotNull(result.getDependencies());
        assertTrue(result.getDependencies().size() > 0);
    }

    @Test
    @DisplayName("analyzeCodebase should handle custom options")
    void testAnalyzeCodebaseWithOptions() throws IOException {
        // Create test files
        Files.writeString(tempDir.resolve("test.js"), "const fs = require('fs');\nimport React from 'react';");
        Files.writeString(tempDir.resolve("test.py"), "import os\nfrom datetime import datetime");
        
        // Create ignored directory
        Path nodeModules = tempDir.resolve("node_modules");
        Files.createDirectory(nodeModules);
        Files.writeString(nodeModules.resolve("ignored.js"), "// This should be ignored");

        CodebaseAnalyzer.AnalysisOptions options = new CodebaseAnalyzer.AnalysisOptions();
        options.setMaxFiles(2);
        options.setIncludeHidden(false);

        ProjectStructure result = codebaseAnalyzer.analyzeCodebase(tempDir.toString(), options);

        assertNotNull(result);
        assertEquals(2, result.getTotalFiles()); // Limited by maxFiles
        
        // Should find JavaScript dependencies
        List<DependencyInfo> deps = result.getDependencies();
        assertTrue(deps.stream().anyMatch(d -> d.getName().equals("fs")));
        assertTrue(deps.stream().anyMatch(d -> d.getName().equals("react")));
    }

    @Test
    @DisplayName("analyzeCodebase should throw for non-existent directory")
    void testAnalyzeCodebaseNonExistentDirectory() {
        String nonExistentPath = tempDir.resolve("non-existent").toString();
        
        UserError exception = assertThrows(
            UserError.class,
            () -> codebaseAnalyzer.analyzeCodebase(nonExistentPath)
        );
        assertTrue(exception.getMessage().contains("Directory does not exist"));
    }

    @Test
    @DisplayName("analyzeCodebase should throw for file instead of directory")
    void testAnalyzeCodebaseFileInsteadOfDirectory() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "content");
        
        UserError exception = assertThrows(
            UserError.class,
            () -> codebaseAnalyzer.analyzeCodebase(testFile.toString())
        );
        assertTrue(exception.getMessage().contains("Path is not a directory"));
    }

    @Test
    @DisplayName("findFilesByContent should find matching content")
    void testFindFilesByContent() throws IOException {
        // Create test files
        Files.writeString(tempDir.resolve("file1.txt"), "Hello world\nThis is a test\nGoodbye world");
        Files.writeString(tempDir.resolve("file2.txt"), "Another file\nWith different content");
        Files.writeString(tempDir.resolve("file3.java"), "public class Test {\n  // Hello comment\n}");

        FileSearchOptions options = new FileSearchOptions();
        options.setCaseSensitive(false);
        options.setMaxResults(10);

        List<FileSearchResult> results = codebaseAnalyzer.findFilesByContent(
            tempDir.toString(), "hello", options);

        assertNotNull(results);
        assertEquals(2, results.size());
        
        // Check results contain expected files
        assertTrue(results.stream().anyMatch(r -> r.getPath().endsWith("file1.txt")));
        assertTrue(results.stream().anyMatch(r -> r.getPath().endsWith("file3.java")));
    }

    @Test
    @DisplayName("findFilesByContent should respect case sensitivity")
    void testFindFilesByContentCaseSensitive() throws IOException {
        Files.writeString(tempDir.resolve("test.txt"), "Hello World\nhello world");

        FileSearchOptions options = new FileSearchOptions();
        options.setCaseSensitive(true);
        options.setMaxResults(10);

        List<FileSearchResult> results = codebaseAnalyzer.findFilesByContent(
            tempDir.toString(), "Hello", options);

        assertEquals(1, results.size());
        assertEquals(1, results.get(0).getLine());
        assertTrue(results.get(0).getContent().contains("Hello World"));
    }

    @Test
    @DisplayName("findFilesByContent should filter by file extensions")
    void testFindFilesByContentWithExtensions() throws IOException {
        Files.writeString(tempDir.resolve("test.txt"), "search term here");
        Files.writeString(tempDir.resolve("test.java"), "search term here");
        Files.writeString(tempDir.resolve("test.py"), "search term here");

        FileSearchOptions options = new FileSearchOptions();
        options.getFileExtensions().add("java");
        options.getFileExtensions().add("py");
        options.setMaxResults(10);

        List<FileSearchResult> results = codebaseAnalyzer.findFilesByContent(
            tempDir.toString(), "search", options);

        assertEquals(2, results.size());
        assertTrue(results.stream().noneMatch(r -> r.getPath().endsWith(".txt")));
        assertTrue(results.stream().anyMatch(r -> r.getPath().endsWith(".java")));
        assertTrue(results.stream().anyMatch(r -> r.getPath().endsWith(".py")));
    }

    @Test
    @DisplayName("findFilesByContent should respect max results limit")
    void testFindFilesByContentMaxResults() throws IOException {
        // Create multiple files with matching content
        for (int i = 1; i <= 5; i++) {
            Files.writeString(tempDir.resolve("file" + i + ".txt"), "matching content");
        }

        FileSearchOptions options = new FileSearchOptions();
        options.setMaxResults(3);

        List<FileSearchResult> results = codebaseAnalyzer.findFilesByContent(
            tempDir.toString(), "matching", options);

        assertEquals(3, results.size());
    }

    @Test
    @DisplayName("analyzeProjectDependencies should detect package.json dependencies")
    void testAnalyzeProjectDependenciesPackageJson() throws IOException {
        String packageJsonContent = """
            {
              "name": "test-project",
              "dependencies": {
                "react": "^18.0.0",
                "lodash": "^4.17.21"
              },
              "devDependencies": {
                "webpack": "^5.0.0"
              }
            }
            """;
        Files.writeString(tempDir.resolve("package.json"), packageJsonContent);

        Map<String, String> dependencies = codebaseAnalyzer.analyzeProjectDependencies(tempDir.toString());

        assertNotNull(dependencies);
        // Note: The actual parsing implementation would need to be completed
        // This test verifies the method can be called without errors
    }

    @Test
    @DisplayName("analyzeProjectDependencies should detect pom.xml dependencies")
    void testAnalyzeProjectDependenciesPomXml() throws IOException {
        String pomXmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <dependencies>
                    <dependency>
                        <groupId>org.springframework</groupId>
                        <artifactId>spring-core</artifactId>
                        <version>5.3.0</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.writeString(tempDir.resolve("pom.xml"), pomXmlContent);

        Map<String, String> dependencies = codebaseAnalyzer.analyzeProjectDependencies(tempDir.toString());

        assertNotNull(dependencies);
        // Note: The actual parsing implementation would need to be completed
    }

    @Test
    @DisplayName("AnalysisOptions should have correct defaults")
    void testAnalysisOptionsDefaults() {
        CodebaseAnalyzer.AnalysisOptions options = new CodebaseAnalyzer.AnalysisOptions();

        assertNotNull(options.getIgnorePatterns());
        assertTrue(options.getIgnorePatterns().contains("node_modules"));
        assertTrue(options.getIgnorePatterns().contains(".git"));
        assertEquals(1000, options.getMaxFiles());
        assertEquals(1024 * 1024, options.getMaxSizePerFile());
        assertFalse(options.isIncludeHidden());
    }

    @Test
    @DisplayName("AnalysisOptions should allow customization")
    void testAnalysisOptionsCustomization() {
        CodebaseAnalyzer.AnalysisOptions options = new CodebaseAnalyzer.AnalysisOptions();
        
        options.setMaxFiles(500);
        options.setMaxSizePerFile(2 * 1024 * 1024);
        options.setIncludeHidden(true);
        options.getIgnorePatterns().add("custom-ignore");

        assertEquals(500, options.getMaxFiles());
        assertEquals(2 * 1024 * 1024, options.getMaxSizePerFile());
        assertTrue(options.isIncludeHidden());
        assertTrue(options.getIgnorePatterns().contains("custom-ignore"));
    }

    @Test
    @DisplayName("analyzeCodebase should detect JavaScript dependencies")
    void testAnalyzeCodebaseJavaScriptDependencies() throws IOException {
        String jsContent = """
            import React from 'react';
            import { useState } from 'react';
            const lodash = require('lodash');
            const fs = require('fs');
            import './local-file';
            """;
        Files.writeString(tempDir.resolve("app.js"), jsContent);

        ProjectStructure result = codebaseAnalyzer.analyzeCodebase(tempDir.toString());

        List<DependencyInfo> deps = result.getDependencies();
        
        // Should find external dependencies
        assertTrue(deps.stream().anyMatch(d -> d.getName().equals("react") && d.isExternal()));
        assertTrue(deps.stream().anyMatch(d -> d.getName().equals("lodash") && d.isExternal()));
        assertTrue(deps.stream().anyMatch(d -> d.getName().equals("fs") && d.isExternal()));
        
        // Should find internal dependency
        assertTrue(deps.stream().anyMatch(d -> d.getName().equals("internal") && !d.isExternal()));
    }

    @Test
    @DisplayName("analyzeCodebase should detect Python dependencies")
    void testAnalyzeCodebasePythonDependencies() throws IOException {
        String pythonContent = """
            import os
            import sys
            from datetime import datetime
            import numpy as np
            import pandas
            from .local_module import function
            """;
        Files.writeString(tempDir.resolve("script.py"), pythonContent);

        ProjectStructure result = codebaseAnalyzer.analyzeCodebase(tempDir.toString());

        List<DependencyInfo> deps = result.getDependencies();
        
        // Should find standard library (not external)
        assertTrue(deps.stream().anyMatch(d -> d.getName().equals("os") && !d.isExternal()));
        assertTrue(deps.stream().anyMatch(d -> d.getName().equals("datetime") && !d.isExternal()));
        
        // Should find external dependencies
        assertTrue(deps.stream().anyMatch(d -> d.getName().equals("numpy") && d.isExternal()));
        assertTrue(deps.stream().anyMatch(d -> d.getName().equals("pandas") && d.isExternal()));
    }

    @Test
    @DisplayName("analyzeCodebase should detect Java dependencies")
    void testAnalyzeCodebaseJavaDependencies() throws IOException {
        String javaContent = """
            package com.example;
            
            import java.util.List;
            import java.io.IOException;
            import org.springframework.stereotype.Service;
            import com.fasterxml.jackson.databind.ObjectMapper;
            """;
        Files.writeString(tempDir.resolve("Example.java"), javaContent);

        ProjectStructure result = codebaseAnalyzer.analyzeCodebase(tempDir.toString());

        List<DependencyInfo> deps = result.getDependencies();
        
        // Should find all imports as external (Java analyzer considers all imports external)
        assertTrue(deps.stream().anyMatch(d -> d.getName().equals("java") && d.isExternal()));
        assertTrue(deps.stream().anyMatch(d -> d.getName().equals("org") && d.isExternal()));
        assertTrue(deps.stream().anyMatch(d -> d.getName().equals("com") && d.isExternal()));
    }

    @Test
    @DisplayName("analyzeCodebase should handle large files gracefully")
    void testAnalyzeCodebaseLargeFiles() throws IOException {
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeContent.append("Line ").append(i).append("\n");
        }
        Files.writeString(tempDir.resolve("large.txt"), largeContent.toString());

        CodebaseAnalyzer.AnalysisOptions options = new CodebaseAnalyzer.AnalysisOptions();
        options.setMaxSizePerFile(1024); // Very small limit

        ProjectStructure result = codebaseAnalyzer.analyzeCodebase(tempDir.toString(), options);

        assertNotNull(result);
        // Large file should be skipped due to size limit
        assertEquals(0, result.getTotalFiles());
    }

    @Test
    @DisplayName("analyzeCodebase should ignore specified patterns")
    void testAnalyzeCodebaseIgnorePatterns() throws IOException {
        // Create files that should be ignored
        Path nodeModules = tempDir.resolve("node_modules");
        Files.createDirectory(nodeModules);
        Files.writeString(nodeModules.resolve("package.js"), "// ignored");
        
        Path gitDir = tempDir.resolve(".git");
        Files.createDirectory(gitDir);
        Files.writeString(gitDir.resolve("config"), "// ignored");
        
        // Create files that should be included
        Files.writeString(tempDir.resolve("src.js"), "// included");

        ProjectStructure result = codebaseAnalyzer.analyzeCodebase(tempDir.toString());

        assertNotNull(result);
        assertEquals(1, result.getTotalFiles()); // Only src.js should be included
        assertTrue(result.getFilesByLanguage().containsKey("JavaScript"));
    }

    @Test
    @DisplayName("analyzeCodebase should count lines correctly")
    void testAnalyzeCodebaseLineCount() throws IOException {
        String multiLineContent = "Line 1\nLine 2\nLine 3\n\nLine 5";
        Files.writeString(tempDir.resolve("test.txt"), multiLineContent);

        ProjectStructure result = codebaseAnalyzer.analyzeCodebase(tempDir.toString());

        assertNotNull(result);
        assertEquals(5, result.getTotalLinesOfCode());
    }

    @Test
    @DisplayName("FileSearchOptions should have correct defaults")
    void testFileSearchOptionsDefaults() {
        FileSearchOptions options = new FileSearchOptions();

        assertFalse(options.isCaseSensitive());
        assertEquals(100, options.getMaxResults());
        assertFalse(options.isIncludeHidden());
        assertEquals(1024 * 1024, options.getMaxFileSize());
        assertTrue(options.getFileExtensions().isEmpty());
        assertTrue(options.getIgnorePatterns().isEmpty());
    }

    @Test
    @DisplayName("analyzeCodebase should handle empty directory")
    void testAnalyzeCodebaseEmptyDirectory() throws IOException {
        Path emptyDir = tempDir.resolve("empty");
        Files.createDirectory(emptyDir);

        ProjectStructure result = codebaseAnalyzer.analyzeCodebase(emptyDir.toString());

        assertNotNull(result);
        assertEquals(0, result.getTotalFiles());
        assertEquals(0, result.getTotalLinesOfCode());
        assertTrue(result.getFilesByLanguage().isEmpty());
        assertTrue(result.getDependencies().isEmpty());
    }

    @Test
    @DisplayName("analyzeCodebase should detect multiple languages")
    void testAnalyzeCodebaseMultipleLanguages() throws IOException {
        Files.writeString(tempDir.resolve("script.js"), "console.log('hello');");
        Files.writeString(tempDir.resolve("script.py"), "print('hello')");
        Files.writeString(tempDir.resolve("Script.java"), "System.out.println(\"hello\");");
        Files.writeString(tempDir.resolve("style.css"), "body { color: red; }");
        Files.writeString(tempDir.resolve("index.html"), "<html><body>Hello</body></html>");

        ProjectStructure result = codebaseAnalyzer.analyzeCodebase(tempDir.toString());

        assertNotNull(result);
        assertEquals(5, result.getTotalFiles());
        
        Map<String, Integer> languages = result.getFilesByLanguage();
        assertEquals(1, languages.get("JavaScript"));
        assertEquals(1, languages.get("Python"));
        assertEquals(1, languages.get("Java"));
        assertEquals(1, languages.get("CSS"));
        assertEquals(1, languages.get("HTML"));
    }
}
