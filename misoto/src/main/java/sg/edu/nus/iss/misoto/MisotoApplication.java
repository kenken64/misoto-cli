package sg.edu.nus.iss.misoto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import sg.edu.nus.iss.misoto.cli.mcp.config.McpCliOptions;
import sg.edu.nus.iss.misoto.cli.config.DotenvLoader;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Slf4j
public class MisotoApplication {

	public static void main(String[] args) {
		// Initialize dotenv early in the application startup
		DotenvLoader.initialize();
		
		// Log startup information
		log.debug("Starting Misoto application with {} arguments", args.length);
		log.debug("Environment check - ANTHROPIC_API_KEY: {}", 
			DotenvLoader.hasEnv("ANTHROPIC_API_KEY") ? "Present" : "Not found");
		
		// Process MCP CLI options first
		McpCliOptions mcpCliOptions = new McpCliOptions();
		mcpCliOptions.parseArgs(args);
		
		// Handle MCP CLI options that should run before Spring context
		if (mcpCliOptions.isCreateConfig()) {
			handleCreateConfig(mcpCliOptions);
			return;
		}
		
		if (mcpCliOptions.isValidateConfig()) {
			handleValidateConfig(mcpCliOptions);
			return;
		}
		
		// Set MCP config file as system property if specified
		if (mcpCliOptions.getConfigFile() != null) {
			System.setProperty("mcp.config.file", mcpCliOptions.getConfigFile());
		}
		
		// Check if running in CLI mode (has command line arguments)
		if (args.length > 0) {
			// Run in CLI mode - this will execute the ClaudeCli CommandLineRunner
			System.setProperty("spring.main.web-application-type", "none");
			SpringApplication.run(MisotoApplication.class, args);
		} else {
			// Run in web mode (default Spring Boot behavior)
			SpringApplication.run(MisotoApplication.class, args);
		}
	}
	
	private static void handleCreateConfig(McpCliOptions cliOptions) {
		try {
			String configPath = cliOptions.getConfigFile();
			if (configPath == null) {
				configPath = System.getProperty("user.home") + "/.misoto/mcp.json";
			}
			
			System.out.printf("Creating default MCP configuration at: %s%n", configPath);
			
			// Create the directory if it doesn't exist
			java.nio.file.Path path = java.nio.file.Paths.get(configPath);
			java.nio.file.Files.createDirectories(path.getParent());
			
			// Create default configuration
			String defaultConfig = createDefaultConfigJson();
			java.nio.file.Files.writeString(path, defaultConfig);
			
			System.out.println("✓ Default MCP configuration created successfully");
			System.out.printf("Edit the file at %s to customize your MCP settings%n", configPath);
		} catch (Exception e) {
			System.err.printf("✗ Failed to create configuration: %s%n", e.getMessage());
			System.exit(1);
		}
	}
	
	private static void handleValidateConfig(McpCliOptions cliOptions) {
		try {
			String configPath = cliOptions.getConfigFile();
			if (configPath == null) {
				configPath = System.getProperty("user.home") + "/.misoto/mcp.json";
			}
			
			System.out.printf("Validating MCP configuration file: %s%n", configPath);
			
			if (!java.nio.file.Files.exists(java.nio.file.Paths.get(configPath))) {
				System.err.println("✗ Configuration file does not exist: " + configPath);
				System.exit(1);
			}
			
			// Basic JSON validation
			String content = java.nio.file.Files.readString(java.nio.file.Paths.get(configPath));
			com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
			mapper.readTree(content); // This will throw if invalid JSON
			
			System.out.println("✓ Configuration file is valid JSON");
		} catch (Exception e) {
			System.err.printf("✗ Configuration validation failed: %s%n", e.getMessage());
			System.exit(1);
		}
	}
	
	private static String createDefaultConfigJson() {
		return """
			{
			  "client": {
			    "name": "misoto-cli",
			    "version": "1.0.0",
			    "connectionTimeout": 30000,
			    "readTimeout": 60000,
			    "writeTimeout": 30000
			  },
			  "servers": {
			    "default": {
			      "url": "http://localhost:8080",
			      "name": "Local MCP Server",
			      "description": "Local development MCP server",
			      "enabled": true,
			      "priority": 1,
			      "headers": {}
			    },
			    "remote": {
			      "url": "http://localhost:8081",
			      "name": "Remote MCP Server", 
			      "description": "Remote production MCP server",
			      "enabled": false,
			      "priority": 2,
			      "headers": {}
			    },
			    "tools": {
			      "url": "http://localhost:8082",
			      "name": "Tools MCP Server",
			      "description": "Specialized tools MCP server", 
			      "enabled": false,
			      "priority": 3,
			      "headers": {}
			    }
			  }
			}""";
	}

}
