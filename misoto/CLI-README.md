# Claude Code CLI - Java Version

This project contains a Java conversion of the TypeScript Claude Code CLI, integrated into a Spring Boot application.

## Overview

The CLI provides the following functionality:
- **Authentication**: Login/logout with Claude AI API
- **AI Assistance**: Ask questions to Claude AI
- **Code Analysis**: Explain code files using Claude AI
- **Help System**: Built-in help and command documentation

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
