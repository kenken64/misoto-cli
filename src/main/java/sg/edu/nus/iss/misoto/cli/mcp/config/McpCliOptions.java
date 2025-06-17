package sg.edu.nus.iss.misoto.cli.mcp.config;

import lombok.Data;
import org.springframework.stereotype.Component;

/**
 * Command line arguments for MCP configuration
 */
@Component
@Data
public class McpCliOptions {
    
    private String configFile;
    private boolean createConfig = false;
    private boolean validateConfig = false;
    
    /**
     * Parse command line arguments for MCP options
     */
    public void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            
            switch (arg) {
                case "--mcp-config":
                case "-mc":
                    if (i + 1 < args.length) {
                        configFile = args[++i];
                    }
                    break;
                    
                case "--mcp-create-config":
                    createConfig = true;
                    break;
                    
                case "--mcp-validate-config":
                    validateConfig = true;
                    break;
            }
        }
    }
    
    /**
     * Get help text for MCP CLI options
     */
    public static String getHelpText() {
        return """
            MCP Configuration Options:
              --mcp-config, -mc <file>    Specify MCP configuration file path
              --mcp-create-config         Create default config file if it doesn't exist
              --mcp-validate-config       Validate the MCP configuration file
            
            Examples:
              java -jar misoto.jar --mcp-config /path/to/mcp.json
              java -jar misoto.jar --mcp-config ~/.misoto/mcp.json --mcp-create-config
            """;
    }
}
