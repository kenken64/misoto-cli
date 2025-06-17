# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Java-based CLI application called Misoto that ports TypeScript Claude Code functionality to Java using Spring Boot. The project features:

- **Multi-module architecture**: Contains 3 main modules (misoto/, mcp-server/, claude-code/)
- **Agent system**: Autonomous task execution with different operational modes
- **MCP integration**: Model Context Protocol support with multi-server capabilities
- **AI integration**: Claude AI client using Spring AI Anthropic provider

## Build and Development Commands

### Maven Build Commands
```bash
# Build the main application
cd misoto && ./mvnw clean package -DskipTests

# Build with tests
cd misoto && ./mvnw clean package

# Run tests only
cd misoto && ./mvnw test

# Run specific test class
cd misoto && ./mvnw test -Dtest=AgentSystemIntegrationTest

# Run in development mode (auto-restart)
cd misoto && ./mvnw spring-boot:run
```

### MCP Server Commands
```bash
# Build MCP server (Gradle)
cd mcp-server && ./gradlew build

# Run MCP server
cd mcp-server && ./gradlew bootRun

# Test MCP server endpoints
cd mcp-server && ./test-endpoints.ps1
```

### Application Execution
```bash
# Run CLI with Java directly
cd misoto && java -jar target/misoto-0.0.1-SNAPSHOT.jar <command>

# Use PowerShell wrapper (auto-builds)
cd misoto && ./claude-code.ps1 <command>

# Start agent chat mode
cd misoto && ./scripts/start-agent-chat.ps1
```

## Architecture

### Core CLI Structure
- **MisotoApplication**: Main Spring Boot entry point with MCP CLI preprocessing
- **ClaudeCli**: CommandLineRunner that handles argument parsing and command execution
- **Command pattern**: All commands implement `Command` interface, registered via `CommandRegistry`
- **Configuration**: Dual config system - Spring properties + JSON-based MCP config

### Agent System (Key Feature)
Located in `misoto/src/main/java/sg/edu/nus/iss/misoto/cli/agent/`:
- **AgentService**: Main orchestrator managing agent lifecycle
- **TaskQueueService**: Handles task queuing with priority levels (CRITICAL, HIGH, NORMAL, LOW)
- **TaskExecutorService**: Executes different task types (SHELL, FILE, AI, MCP, SYSTEM, COMPOSITE)
- **DecisionEngine**: Makes autonomous decisions based on context
- **AgentStateManager**: Persistent state and session management

Agent modes: INTERACTIVE, AUTONOMOUS, SUPERVISED, MANUAL

### MCP (Model Context Protocol) System
Located in `misoto/src/main/java/sg/edu/nus/iss/misoto/cli/mcp/`:
- **Multi-server support**: Can connect to multiple MCP servers with priority ordering
- **JSON configuration**: Standalone `mcp.json` config file (not in Spring properties)
- **CLI management**: `mcp` command with subcommands for config management
- **WebSocket + HTTP**: Supports both connection types

### AI Integration
- Uses Spring AI with Anthropic provider
- Configured via `ANTHROPIC_API_KEY` environment variable
- Multiple AI providers supported: Anthropic (primary), Ollama (secondary)

## Configuration Files

### Environment Variables
```bash
# AI Provider Configuration
ANTHROPIC_API_KEY=sk-ant-api03-...              # Claude AI API key (required for Anthropic)
MISOTO_AI_DEFAULT_PROVIDER=anthropic            # Default AI provider (anthropic|ollama)

# Ollama Configuration (if using Ollama)
OLLAMA_HOST=http://localhost:11434              # Standard Ollama host URL
MISOTO_AI_OLLAMA_URL=http://localhost:11434     # Custom Ollama URL (overrides OLLAMA_HOST)
MISOTO_AI_OLLAMA_MODEL=qwen2.5:0.5b             # Default Ollama model

# Agent system
MISOTO_AGENT_MODE=true
MISOTO_AGENT_MAX_TASKS=3
MISOTO_AGENT_INTERVAL=5000

# Optional
MISOTO_CONFIG_DIR=~/.misoto
```

### Key Config Files
- `misoto/src/main/resources/application.properties`: Spring Boot configuration
- `misoto/src/main/resources/mcp.json`: MCP server configurations
- `~/.misoto/mcp.json`: User-specific MCP configuration
- `.env`: Environment variables (loaded by DotenvLoader)

## Testing

### Test Structure
- **Unit tests**: Standard JUnit 5 tests in `src/test/java/`
- **Integration tests**: Agent system integration tests
- **MCP tests**: PowerShell scripts in `mcp-server/` and `misoto/scripts/`

### Run Tests
```bash
# Run all tests
cd misoto && ./mvnw test

# Run specific test categories
cd misoto && ./mvnw test -Dtest="*Test"
cd misoto && ./mvnw test -Dtest="*IntegrationTest"

# Agent system integration test
cd misoto && ./scripts/test-agent-mode.ps1

# MCP integration tests
cd misoto && ./scripts/test-mcp-integration.ps1
```

## Key Development Patterns

### Command Implementation
All commands follow this pattern:
1. Implement `Command` interface
2. Use `@Component` annotation for Spring registration
3. Register in `CommandRegistrationService.registerAllCommands()`
4. Categories: "AI Assistance", "Authentication", "Configuration", "Development"

### Error Handling
- `UserError`: User-facing errors (exit code 1)
- `ErrorFormatter`: Formats errors for display
- Telemetry: All errors tracked via `TelemetryService`

### Async Operations
- Agent tasks are executed asynchronously
- Uses Spring's `@Async` with custom thread pools
- `CompletableFuture` for async operations

## Scripts and Automation

### Build Scripts
- `misoto/scripts/build-and-run.ps1`: Build and run in one command
- `misoto/scripts/run.ps1`: Run pre-built JAR

### Agent Scripts  
- `misoto/scripts/start-agent-chat.ps1`: Start interactive agent mode
- `misoto/scripts/test-agent-mode.ps1`: Test agent functionality

### MCP Scripts
- `misoto/scripts/test-mcp-*.ps1`: Various MCP integration tests
- `mcp-server/test-*.ps1`: MCP server testing scripts

## Important Notes

- **Java 17+**: Required for compilation and runtime
- **Spring Boot 3.5**: Uses latest Spring Boot with Spring AI integration
- **Maven wrapper**: Always use `./mvnw` instead of global Maven
- **Multi-threaded**: Agent system runs background tasks while CLI operates
- **State persistence**: Agent state is persisted across sessions
- **Graceful shutdown**: Application includes proper shutdown handling (5s timeout)

## Development Workflow

1. **Environment setup**: Set `ANTHROPIC_API_KEY` environment variable
2. **Build**: Run `./mvnw clean package -DskipTests` to build quickly
3. **Test**: Use `./mvnw test` to run tests when making changes
4. **Run**: Use `./claude-code.ps1` for development, `java -jar` for production
5. **Agent mode**: Use `./scripts/start-agent-chat.ps1` for agent development
6. **MCP testing**: Use scripts in `scripts/` directory for MCP functionality