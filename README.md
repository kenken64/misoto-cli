# Claude Code CLI - Java Version

This project contains a Java conversion of the TypeScript Claude Code CLI, integrated into a Spring Boot application.

## Overview

The CLI provides the following functionality:
- **Authentication**: Login/logout with Claude AI API
- **AI Assistance**: Ask questions to Claude AI
- **Code Analysis**: Explain code files using Claude AI
- **Help System**: Built-in help and command documentation

## 📚 Documentation Index

### Quick Navigation Table

| Category | Document | Description | Status |
|----------|----------|-------------|--------|
| **📋 Project Documentation** |
| Root | [README.md](README.md) | Main project overview and setup guide | ✅ Current |
| Root | [PORTING_DOCUMENTATION.md](PORTING_DOCUMENTATION.md) | TypeScript to Java porting guide | ✅ Current |
| Root | [PROJECT_STRUCTURE_UPDATED.md](PROJECT_STRUCTURE_UPDATED.md) | Complete current project structure | ✅ Current |
| Root | [MARKDOWN_FILES_INVENTORY.md](MARKDOWN_FILES_INVENTORY.md) | Complete inventory of all MD files | ✅ Current |
| **🏗️ Architecture & Design** |
| Specs | [specs/architecture.md](specs/architecture.md) | System architecture documentation | ✅ Active |
| Specs | [specs/overview.md](specs/overview.md) | Project overview and goals | ✅ Active |
| Specs | [specs/features.md](specs/features.md) | Feature specifications and roadmap | ✅ Active |
| Specs | [specs/performance.md](specs/performance.md) | Performance requirements and optimizations | ✅ Active |
| **📖 User Guides** |
| Specs | [specs/installation.md](specs/installation.md) | Installation instructions | ✅ Active |
| Specs | [specs/command_reference.md](specs/command_reference.md) | CLI command documentation | ✅ Active |
| Misoto | [misoto/CLI-README.md](misoto/CLI-README.md) | CLI usage guide | ✅ Active |
| **🤖 Agent System** |
| Misoto | [misoto/AGENT_MODE_README.md](misoto/AGENT_MODE_README.md) | **Agent mode documentation** | ✅ Active |
| Misoto | [misoto/AGENT_IMPLEMENTATION_COMPLETE.md](misoto/AGENT_IMPLEMENTATION_COMPLETE.md) | **Agent implementation status** | ✅ Active |
| **🔧 Development** |
| Specs | [specs/development.md](specs/development.md) | Development setup and guidelines | ✅ Active |
| Specs | [specs/integration.md](specs/integration.md) | Integration documentation | ✅ Active |
| Specs | [specs/error_handling.md](specs/error_handling.md) | Error handling strategies | ✅ Active |
| Misoto | [misoto/scripts/README.md](misoto/scripts/README.md) | **Script usage documentation** | ✅ Active |
| **🔌 MCP Integration** |
| Misoto | [misoto/MCP_CONFIG_MIGRATION_COMPLETED.md](misoto/MCP_CONFIG_MIGRATION_COMPLETED.md) | **MCP configuration migration** | ✅ Active |
| Misoto | [misoto/MCP_MULTI_SERVER_COMPLETION.md](misoto/MCP_MULTI_SERVER_COMPLETION.md) | **Multi-server MCP implementation** | ✅ Active |
| MCP Server | [mcp-server/README.md](mcp-server/README.md) | MCP server implementation guide | ✅ Active |
| MCP Server | [mcp-server/TESTING_RESULTS.md](mcp-server/TESTING_RESULTS.md) | **MCP server test results** | ✅ Active |
| **⚙️ Implementation Details** |
| Misoto | [misoto/README.md](misoto/README.md) | Java implementation overview | ✅ Active |
| Specs | [specs/index.md](specs/index.md) | Specs directory index | ✅ Active |
| **📄 Legal & Meta** |
| Specs | [specs/LICENSE.md](specs/LICENSE.md) | License information | ✅ Active |
| MCP Server | [mcp-server/.github/copilot-instructions.md](mcp-server/.github/copilot-instructions.md) | GitHub Copilot instructions | ✅ Active |
| **📚 Legacy Reference (TypeScript)** |
| Claude Code | [claude-code/LICENSE.md](claude-code/LICENSE.md) | TypeScript implementation license | 📝 Reference |
| Claude Code | [claude-code/src/ai/client.md](claude-code/src/ai/client.md) | AI client implementation docs | 📝 Reference |
| Claude Code | [claude-code/src/auth/manager.md](claude-code/src/auth/manager.md) | Authentication manager docs | 📝 Reference |
| Claude Code | [claude-code/src/commands/index.md](claude-code/src/commands/index.md) | Commands module docs | 📝 Reference |
| Claude Code | [claude-code/src/config/index.md](claude-code/src/config/index.md) | Configuration docs | 📝 Reference |
| Claude Code | [claude-code/src/errors/formatter.md](claude-code/src/errors/formatter.md) | Error formatting docs | 📝 Reference |
| ... | [See MARKDOWN_FILES_INVENTORY.md](MARKDOWN_FILES_INVENTORY.md) | **Complete list of all 66 MD files** | 📝 Full List |

### Legend
- ✅ **Active**: Currently maintained and up-to-date
- 📝 **Reference**: Legacy documentation for reference
- 🔄 **In Progress**: Being updated or modified

### Recent Additions (2025)
- **Agent System**: Complete autonomous behavior implementation
- **MCP Integration**: Model Context Protocol with multi-server support  
- **Performance Optimizations**: Fast shutdown (5s vs 30s) and improved chat experience
- **Enhanced Documentation**: Comprehensive project structure and inventory

*For the complete list of all 66 markdown files, see [MARKDOWN_FILES_INVENTORY.md](MARKDOWN_FILES_INVENTORY.md)*

## Project Structure

```
src/main/java/sg/edu/nus/iss/misoto/cli/
├── ClaudeCli.java                     # Main CLI entry point and command parser
├── ai/
│   └── AiClient.java                  # Claude AI integration using Spring AI
├── auth/
│   └── AuthManager.java               # Authentication token management
├── commands/
│   ├── Command.java                   # Command interface
│   ├── CommandExecutor.java           # Command execution service
│   ├── CommandRegistry.java           # Command registry and help generation
│   ├── CommandRegistrationService.java # Command registration
│   └── impl/
│       ├── AskCommand.java            # Ask questions to Claude AI
│       ├── ExplainCommand.java        # Explain code files
│       ├── LoginCommand.java          # Authentication login
│       └── LogoutCommand.java         # Authentication logout
└── errors/
    ├── ErrorFormatter.java            # Error formatting for display
    └── UserError.java                 # User-facing error type
```

## Key Features Converted from TypeScript

1. **Command Line Parsing**: Converts TypeScript argument parsing to Java
2. **Command Registry**: Maintains a registry of available commands with categories
3. **Help System**: Generates formatted help output for commands
4. **Error Handling**: User-friendly error messages and proper exit codes
5. **Authentication**: Token-based authentication with local storage
6. **AI Integration**: Uses Spring AI Anthropic integration for Claude API calls

## Installation and Setup

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher
- Claude API key from Anthropic

### Building the Project

```powershell
# Build the project
.\mvnw.cmd clean package -DskipTests

# Or use the provided script
.\claude-code.ps1 --version
```

### Configuration

Set your Claude API key either:

1. **Environment Variable** (Recommended):
   ```powershell
   $env:ANTHROPIC_API_KEY = "sk-ant-api03-..."
   ```

2. **CLI Login** (Alternative):
   ```powershell
   java -jar target\misoto-0.0.1-SNAPSHOT.jar login
   ```

## Usage

### Running the CLI

```powershell
# Using Java directly
java -jar target\misoto-0.0.1-SNAPSHOT.jar <command> [arguments]

# Using PowerShell script (auto-builds if needed)
.\claude-code.ps1 <command> [arguments]

# Using Batch script 
.\claude-code.bat <command> [arguments]
```

### Available Commands

#### Authentication
```powershell
# Login with API token
java -jar target\misoto-0.0.1-SNAPSHOT.jar login --token sk-ant-api03-...

# Login interactively
java -jar target\misoto-0.0.1-SNAPSHOT.jar login

# Logout
java -jar target\misoto-0.0.1-SNAPSHOT.jar logout
```

#### AI Assistance
```powershell
# Ask a question
java -jar target\misoto-0.0.1-SNAPSHOT.jar ask "How do I implement a binary search tree in Java?"

# Explain a code file
java -jar target\misoto-0.0.1-SNAPSHOT.jar explain src\main\java\MyClass.java

# Explain with focus
java -jar target\misoto-0.0.1-SNAPSHOT.jar explain MyClass.java --focus=algorithm
```

#### Help and Information
```powershell
# Show all commands
java -jar target\misoto-0.0.1-SNAPSHOT.jar help

# Show help for specific command
java -jar target\misoto-0.0.1-SNAPSHOT.jar help ask

# Show version
java -jar target\misoto-0.0.1-SNAPSHOT.jar version
```

## Key Differences from TypeScript Version

1. **Spring Boot Integration**: Uses Spring Boot's dependency injection and configuration
2. **Spring AI**: Leverages Spring AI for Anthropic Claude integration
3. **Java Type System**: Strongly typed interfaces and classes instead of TypeScript types
4. **Maven Dependencies**: Uses Maven for dependency management instead of npm
5. **JAR Packaging**: Distributed as a self-contained JAR instead of npm package

## Development

### Adding New Commands

1. Create a new command class implementing the `Command` interface:
   ```java
   @Component
   public class MyCommand implements Command {
       // Implement required methods
   }
   ```

2. Register the command in `CommandRegistrationService`:
   ```java
   @Autowired
   private MyCommand myCommand;
   
   // Add to registerAllCommands() method
   commands.add(myCommand);
   ```

### Configuration Options

Configure the application through `application.properties`:

```properties
# Claude AI Configuration
spring.ai.anthropic.api-key=${ANTHROPIC_API_KEY:}
spring.ai.anthropic.chat.model=claude-3-sonnet-20240229
spring.ai.anthropic.chat.temperature=0.7
spring.ai.anthropic.chat.max-tokens=4000

# Logging
logging.level.sg.edu.nus.iss.misoto.cli=INFO
```

## Troubleshooting

### Common Issues

1. **No API Key**: Set the `ANTHROPIC_API_KEY` environment variable or use the login command
2. **Java Version**: Ensure you're using Java 17 or higher
3. **Build Issues**: Run `.\mvnw.cmd clean compile` to rebuild

### Authentication

The CLI stores authentication tokens in `~/.claude-code/auth.token` on your system. Use the `logout` command to clear stored credentials.

## License

This project maintains the same license as the original TypeScript implementation.
