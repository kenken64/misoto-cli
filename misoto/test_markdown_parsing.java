import java.util.HashMap;
import java.util.Map;

public class test_markdown_parsing {
    /**
     * Parse AI analysis response into structured data - simplified test version
     */
    private static Map<String, String> parseAnalysisResponse(String response) {
        Map<String, String> result = new HashMap<>();
        String[] lines = response.split("\n");
        StringBuilder codeBuilder = new StringBuilder();
        boolean inCodeSection = false;
        boolean inMarkdownCodeBlock = false;
        String detectedLanguage = null;
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // Handle structured format (CODE: ... END_CODE)
            if (trimmedLine.startsWith("LANGUAGE:")) {
                result.put("LANGUAGE", trimmedLine.substring("LANGUAGE:".length()).trim().toLowerCase());
            } else if (trimmedLine.startsWith("FILENAME:")) {
                result.put("FILENAME", trimmedLine.substring("FILENAME:".length()).trim());
            } else if (trimmedLine.startsWith("DIRECTORIES:")) {
                result.put("DIRECTORIES", trimmedLine.substring("DIRECTORIES:".length()).trim());
            } else if (trimmedLine.equals("CODE:")) {
                inCodeSection = true;
            } else if (trimmedLine.equals("END_CODE")) {
                inCodeSection = false;
            } 
            // Handle markdown code blocks (```language ... ```)
            else if (trimmedLine.startsWith("```")) {
                if (!inMarkdownCodeBlock) {
                    // Starting a code block
                    inMarkdownCodeBlock = true;
                    // Extract language from the opening tag
                    String languageTag = trimmedLine.substring(3).trim().toLowerCase();
                    if (!languageTag.isEmpty()) {
                        detectedLanguage = languageTag;
                        result.put("LANGUAGE", languageTag);
                    }
                } else {
                    // Ending a code block
                    inMarkdownCodeBlock = false;
                }
            } else if (inCodeSection || inMarkdownCodeBlock) {
                // Preserve original line formatting for code content
                codeBuilder.append(line).append("\n");
            }
        }
        
        // If no explicit language was set but we detected one from markdown, use it
        if (!result.containsKey("LANGUAGE") && detectedLanguage != null) {
            result.put("LANGUAGE", detectedLanguage);
        }
        
        result.put("CODE", codeBuilder.toString().trim());
        return result;
    }
    
    public static void main(String[] args) {
        // Test 1: Structured format
        String structuredResponse = """
            LANGUAGE: python
            FILENAME: hello.py
            DIRECTORIES: none
            CODE:
            print("Hello, World!")
            print("This is a test")
            END_CODE
            """;
        
        System.out.println("=== Test 1: Structured Format ===");
        Map<String, String> result1 = parseAnalysisResponse(structuredResponse);
        System.out.println("Language: " + result1.get("LANGUAGE"));
        System.out.println("Filename: " + result1.get("FILENAME"));
        System.out.println("Code:\n" + result1.get("CODE"));
        
        // Test 2: Markdown code block format
        String markdownResponse = """
            FILENAME: hello.py
            DIRECTORIES: none
            ```python
            print("Hello, World!")
            print("This is a test with markdown")
            ```
            """;
        
        System.out.println("\n=== Test 2: Markdown Format ===");
        Map<String, String> result2 = parseAnalysisResponse(markdownResponse);
        System.out.println("Language: " + result2.get("LANGUAGE"));
        System.out.println("Filename: " + result2.get("FILENAME"));
        System.out.println("Code:\n" + result2.get("CODE"));
        
        // Test 3: Mixed format with multiple code blocks
        String mixedResponse = """
            LANGUAGE: javascript
            FILENAME: test.js
            DIRECTORIES: src/utils
            ```javascript
            function hello() {
                console.log("Hello from JavaScript!");
                return true;
            }
            ```
            """;
        
        System.out.println("\n=== Test 3: Mixed Format ===");
        Map<String, String> result3 = parseAnalysisResponse(mixedResponse);
        System.out.println("Language: " + result3.get("LANGUAGE"));
        System.out.println("Filename: " + result3.get("FILENAME"));
        System.out.println("Directories: " + result3.get("DIRECTORIES"));
        System.out.println("Code:\n" + result3.get("CODE"));
    }
}
