package sg.edu.nus.iss.mcp.server.service;

import org.springframework.stereotype.Service;
import sg.edu.nus.iss.mcp.server.model.McpTool;
import sg.edu.nus.iss.mcp.server.model.McpToolResult;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for managing MCP tools
 * Handles tool registration, discovery, and execution
 */
@Service
@Slf4j
public class ToolService {
    
    private final Map<String, McpTool> registeredTools = new ConcurrentHashMap<>();
    private final Map<String, ToolExecutor> toolExecutors = new ConcurrentHashMap<>();
    
    /**
     * Interface for tool execution
     */
    @FunctionalInterface
    public interface ToolExecutor {
        McpToolResult execute(Map<String, Object> arguments);
    }
    
    /**
     * Initialize with default tools
     */
    public ToolService() {
        registerDefaultTools();
    }
    
    /**
     * Register default tools
     */
    private void registerDefaultTools() {
        // Echo tool - returns the input text
        registerTool(
            McpTool.createWithParams(
                "echo",
                "Echo back the provided text",
                Map.of("text", Map.of("type", "string", "description", "Text to echo back")),
                new String[]{"text"}
            ),
            args -> {
                String text = (String) args.get("text");
                return McpToolResult.success("Echo: " + text);
            }
        );
        
        // Current time tool
        registerTool(
            McpTool.createSimple("current_time", "Get the current date and time"),
            args -> {
                String currentTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                return McpToolResult.success("Current time: " + currentTime);
            }
        );
        
        // Math calculator tool
        registerTool(
            McpTool.createWithParams(
                "calculate",
                "Perform basic mathematical calculations",
                Map.of(
                    "expression", Map.of("type", "string", "description", "Mathematical expression to evaluate"),
                    "operation", Map.of("type", "string", "description", "Operation type: add, subtract, multiply, divide")
                ),
                new String[]{"expression"}
            ),            args -> {
                try {
                    String expression = (String) args.get("expression");
                    String operation = (String) args.getOrDefault("operation", "evaluate");
                    
                    // Enhanced calculation logic with order of operations
                    double result = evaluateExpression(expression.trim());
                    return McpToolResult.success("Result: " + result);
                } catch (Exception e) {
                    return McpToolResult.error("Calculation error: " + e.getMessage());
                }
            }
        );
        
        // System info tool
        registerTool(
            McpTool.createSimple("system_info", "Get basic system information"),
            args -> {
                Map<String, Object> systemInfo = Map.of(
                    "os", System.getProperty("os.name"),
                    "javaVersion", System.getProperty("java.version"),
                    "availableProcessors", Runtime.getRuntime().availableProcessors(),
                    "freeMemory", Runtime.getRuntime().freeMemory(),
                    "totalMemory", Runtime.getRuntime().totalMemory()
                );
                return McpToolResult.success(List.of(
                    McpToolResult.ContentItem.json(systemInfo.toString())
                ));
            }
        );
        
        log.info("Registered {} default tools", registeredTools.size());
    }
    
    /**
     * Register a new tool
     */
    public void registerTool(McpTool tool, ToolExecutor executor) {
        registeredTools.put(tool.getName(), tool);
        toolExecutors.put(tool.getName(), executor);
        log.info("Registered tool: {}", tool.getName());
    }
    
    /**
     * Get all registered tools
     */
    public List<McpTool> getAllTools() {
        return List.copyOf(registeredTools.values());
    }
    
    /**
     * Get a specific tool by name
     */
    public McpTool getTool(String name) {
        return registeredTools.get(name);
    }
    
    /**
     * Execute a tool
     */
    public McpToolResult executeTool(String toolName, Map<String, Object> arguments) {
        log.info("Executing tool: {} with arguments: {}", toolName, arguments);
        
        ToolExecutor executor = toolExecutors.get(toolName);
        if (executor == null) {
            log.error("Tool not found: {}", toolName);
            return McpToolResult.error("Tool not found: " + toolName);
        }
        
        try {
            McpToolResult result = executor.execute(arguments != null ? arguments : Map.of());
            log.info("Tool {} executed successfully", toolName);
            return result;
        } catch (Exception e) {
            log.error("Error executing tool {}: {}", toolName, e.getMessage(), e);
            return McpToolResult.error("Tool execution failed: " + e.getMessage());
        }
    }
    
    /**
     * Check if a tool exists
     */
    public boolean toolExists(String toolName) {
        return registeredTools.containsKey(toolName);
    }
    
    /**
     * Get the number of registered tools
     */
    public int getToolCount() {
        return registeredTools.size();
    }
    
    /**
     * Enhanced expression evaluator with proper order of operations
     * Supports basic arithmetic: +, -, *, /
     */
    private double evaluateExpression(String expression) {
        // Remove spaces
        expression = expression.replaceAll("\\s+", "");
        
        // Handle multiplication and division first (left to right)
        while (expression.contains("*") || expression.contains("/")) {
            int multIndex = expression.indexOf("*");
            int divIndex = expression.indexOf("/");
            
            int opIndex;
            char operator;
            
            if (multIndex == -1) {
                opIndex = divIndex;
                operator = '/';
            } else if (divIndex == -1) {
                opIndex = multIndex;
                operator = '*';
            } else {
                if (multIndex < divIndex) {
                    opIndex = multIndex;
                    operator = '*';
                } else {
                    opIndex = divIndex;
                    operator = '/';
                }
            }
            
            // Find the operands
            String leftPart = expression.substring(0, opIndex);
            String rightPart = expression.substring(opIndex + 1);
            
            // Extract numbers around the operator
            double leftNum = extractRightmostNumber(leftPart);
            double rightNum = extractLeftmostNumber(rightPart);
            
            // Calculate result
            double result;
            if (operator == '*') {
                result = leftNum * rightNum;
            } else {
                if (rightNum == 0) {
                    throw new ArithmeticException("Division by zero");
                }
                result = leftNum / rightNum;
            }
            
            // Replace the expression part with the result
            String leftRemainder = removeRightmostNumber(leftPart);
            String rightRemainder = removeLeftmostNumber(rightPart);
            expression = leftRemainder + result + rightRemainder;
        }
        
        // Handle addition and subtraction (left to right)
        while (expression.contains("+") || (expression.contains("-") && expression.lastIndexOf("-") > 0)) {
            int addIndex = expression.indexOf("+");
            int subIndex = expression.indexOf("-", 1); // Skip first character (could be negative sign)
            
            int opIndex;
            char operator;
            
            if (addIndex == -1) {
                opIndex = subIndex;
                operator = '-';
            } else if (subIndex == -1) {
                opIndex = addIndex;
                operator = '+';
            } else {
                if (addIndex < subIndex) {
                    opIndex = addIndex;
                    operator = '+';
                } else {
                    opIndex = subIndex;
                    operator = '-';
                }
            }
            
            // Find the operands
            String leftPart = expression.substring(0, opIndex);
            String rightPart = expression.substring(opIndex + 1);
            
            // Extract numbers around the operator
            double leftNum = extractRightmostNumber(leftPart);
            double rightNum = extractLeftmostNumber(rightPart);
            
            // Calculate result
            double result = (operator == '+') ? leftNum + rightNum : leftNum - rightNum;
            
            // Replace the expression part with the result
            String leftRemainder = removeRightmostNumber(leftPart);
            String rightRemainder = removeLeftmostNumber(rightPart);
            expression = leftRemainder + result + rightRemainder;
        }
        
        // Final result should be a single number
        return Double.parseDouble(expression);
    }
    
    private double extractRightmostNumber(String str) {
        if (str.isEmpty()) return 0;
        
        // Find the last number in the string
        int i = str.length() - 1;
        while (i >= 0 && (Character.isDigit(str.charAt(i)) || str.charAt(i) == '.')) {
            i--;
        }
        
        if (i >= 0 && str.charAt(i) == '-') {
            i--; // Include negative sign
        }
        
        return Double.parseDouble(str.substring(i + 1));
    }
    
    private double extractLeftmostNumber(String str) {
        if (str.isEmpty()) return 0;
        
        // Find the first number in the string
        int i = 0;
        if (str.charAt(0) == '-') {
            i = 1; // Skip negative sign
        }
        
        while (i < str.length() && (Character.isDigit(str.charAt(i)) || str.charAt(i) == '.')) {
            i++;
        }
        
        return Double.parseDouble(str.substring(0, i));
    }
    
    private String removeRightmostNumber(String str) {
        if (str.isEmpty()) return "";
        
        int i = str.length() - 1;
        while (i >= 0 && (Character.isDigit(str.charAt(i)) || str.charAt(i) == '.')) {
            i--;
        }
        
        if (i >= 0 && str.charAt(i) == '-') {
            i--; // Include negative sign
        }
        
        return str.substring(0, i + 1);
    }
    
    private String removeLeftmostNumber(String str) {
        if (str.isEmpty()) return "";
        
        int i = 0;
        if (str.charAt(0) == '-') {
            i = 1; // Skip negative sign
        }
        
        while (i < str.length() && (Character.isDigit(str.charAt(i)) || str.charAt(i) == '.')) {
            i++;
        }
        
        return str.substring(i);
    }
}
