# Misoto CLI - Project Structure Documentation

## Updated Project Structure (Current as of June 17, 2025)

```
misoto-cli/                              # Root project directory
├── README.md                            # Main project documentation with comprehensive tables
├── CLAUDE.md                            # Development guidance for Claude Code
├── CLI-README.md                        # Basic CLI documentation
├── PORTING_DOCUMENTATION.md             # TypeScript to Java migration documentation
├── AGENT_MODE_README.md                 # Agent system documentation
├── AGENT_IMPLEMENTATION_COMPLETE.md     # Agent implementation completion status
├── MCP_CONFIG_MIGRATION_COMPLETED.md    # MCP configuration migration status
├── MCP_MULTI_SERVER_COMPLETION.md       # Multi-server MCP support completion
├── PARAMETER_VALIDATION_ENHANCEMENT_COMPLETE.md # Parameter validation enhancements
├── PLANNING_SYSTEM_README.md            # ReAct planning system documentation
├── PROJECT_STRUCTURE_UPDATED.md         # This file - project structure documentation
├── MARKDOWN_FILES_INVENTORY.md          # Complete listing of all markdown files
├── package.md                           # Package configuration details
├── package-lock.md                      # Dependency lock information
├── backup-config.json                   # Configuration backup
├── pom.xml                              # Root Maven configuration
├── mvnw                                 # Maven wrapper (Unix)
├── mvnw.cmd                             # Maven wrapper (Windows)
│
├── mcp-server/                          # Model Context Protocol Server (Gradle)
│   ├── README.md                        # MCP server documentation
│   ├── TESTING_RESULTS.md               # MCP testing results and validation
│   ├── build.gradle                     # Gradle build configuration
│   ├── settings.gradle                  # Gradle settings
│   ├── pom.xml                          # Maven configuration (dual build support)
│   ├── gradlew                          # Gradle wrapper (Unix)
│   ├── gradlew.bat                      # Gradle wrapper (Windows)
│   ├── simple-sse-test.ps1              # Simple SSE testing script
│   ├── simple-test.ps1                  # Simple functionality test
│   ├── test-endpoints.ps1               # Endpoint testing script
│   ├── test-mcp-server.ps1              # Comprehensive MCP server test
│   ├── test-sse.ps1                     # SSE functionality test
│   ├── gradle/
│   │   └── wrapper/
│   │       ├── gradle-wrapper.jar
│   │       └── gradle-wrapper.properties
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/sg/edu/nus/iss/mcp/server/
│   │   │   │   ├── McpServerApplication.java     # Main application
│   │   │   │   ├── cli/
│   │   │   │   │   └── McpServerCli.java
│   │   │   │   ├── config/
│   │   │   │   │   ├── McpWebSocketHandler.java
│   │   │   │   │   └── WebSocketConfig.java
│   │   │   │   ├── controller/
│   │   │   │   │   └── McpController.java
│   │   │   │   ├── example/
│   │   │   │   │   └── SseClientExample.java
│   │   │   │   ├── model/
│   │   │   │   │   ├── McpResource.java
│   │   │   │   │   ├── McpTool.java
│   │   │   │   │   └── McpToolResult.java
│   │   │   │   ├── protocol/
│   │   │   │   │   ├── McpError.java
│   │   │   │   │   ├── McpMessage.java
│   │   │   │   │   ├── McpNotification.java
│   │   │   │   │   ├── McpRequest.java
│   │   │   │   │   └── McpResponse.java
│   │   │   │   └── service/
│   │   │   │       ├── SseService.java
│   │   │   │       └── ToolService.java
│   │   │   └── resources/
│   │   │       └── application.properties
│   │   └── test/
│   │       └── java/sg/edu/nus/iss/mcp/server/
│   │           └── McpServerApplicationTests.java
│   └── target/                          # Build output directory
│
├── scripts/                             # Build and execution scripts
│   ├── README.md                        # Scripts documentation
│   ├── build-and-run.bat               # Windows build and run script
│   ├── build-and-run.ps1               # PowerShell build and run script
│   ├── claude-code.bat                 # Windows CLI wrapper
│   ├── claude-code.ps1                 # PowerShell CLI wrapper
│   ├── run.bat                         # Windows run script
│   ├── run.ps1                         # PowerShell run script
│   ├── start-agent-chat.bat            # Windows agent mode startup
│   ├── start-agent-chat.ps1            # PowerShell agent mode startup
│   ├── start-agent-chat.sh             # Unix agent mode startup
│   ├── quick_plan_test.sh              # Planning system test
│   ├── test-agent-mode.ps1             # Agent mode testing
│   ├── test-agent-mode.sh              # Unix agent mode testing
│   ├── test-mcp-config-migration.ps1   # MCP config migration test
│   ├── test-mcp-integration.ps1        # MCP integration testing
│   ├── test-mcp-multi-server.ps1       # Multi-server MCP testing
│   ├── test-mcp-multi-server-fixed.ps1 # Fixed multi-server testing
│   ├── test-mcp-multi-server-simple.ps1# Simple multi-server testing
│   ├── test-task-execution.py          # Task execution testing
│   ├── test-*.txt                      # Agent conversation test files
│   ├── test_*.java                     # Java test files
│   ├── test_*.md                       # Test documentation
│   └── unknown                         # Unknown file type
│
├── src/                                 # Main Java Spring Boot Implementation
│   ├── main/
│   │   ├── java/sg/edu/nus/iss/misoto/
│   │   │   ├── MisotoApplication.java           # Main Spring Boot application with MCP preprocessing
│   │   │   └── cli/
│   │   │       ├── ClaudeCli.java               # Main CLI entry point and command parser
│   │       ├── agent/                           # Agent System (Autonomous operation)
│   │   │       │   ├── AgentService.java            # Main agent orchestrator
│   │   │       │   ├── commands/
│   │   │       │   │   └── AgentCommands.java       # CLI commands for agent control
│   │   │       │   ├── config/
│   │   │       │   │   ├── AgentConfiguration.java  # Agent configuration
│   │   │       │   │   └── AgentSystemConfiguration.java # Spring configuration
│   │   │       │   ├── decision/
│   │   │       │   │   ├── DecisionEngine.java      # AI-powered decision making
│   │   │       │   │   └── DecisionTypes.java       # Decision type definitions
│   │   │       │   ├── monitoring/
│   │   │       │   │   └── MonitoringService.java   # Resource and file monitoring
│   │   │       │   ├── planning/                    # ReAct Planning System
│   │   │       │   │   ├── ActionResult.java        # Planning action results
│   │   │       │   │   ├── ActionSpec.java          # Action specifications
│   │   │       │   │   ├── ExecutionPlan.java       # Execution plan management
│   │   │       │   │   ├── ExecutionStep.java       # Individual execution steps
│   │   │       │   │   ├── PlanExecution.java       # Plan execution logic
│   │   │       │   │   ├── PlanningService.java     # Main planning service
│   │   │       │   │   ├── PlanningStrategy.java    # Planning strategies
│   │   │       │   │   ├── ReActCycleResult.java    # ReAct cycle results
│   │   │       │   │   └── SubTask.java             # Subtask definitions
│   │   │       │   ├── state/
│   │   │       │   │   ├── AgentStateManager.java   # Persistent state management
│   │   │       │   │   └── AgentStateSnapshot.java  # State snapshots
│   │   │       │   └── task/
│   │   │       │       ├── AgentTask.java           # Task definitions
│   │   │       │       ├── TaskExecutorService.java # Task execution
│   │   │       │       ├── TaskQueueService.java    # Task queue management
│   │   │       │       └── TaskQueueStats.java      # Queue statistics
│   │   │       ├── ai/                             # AI Integration
│   │   │       │   ├── AiClient.java               # Claude AI client
│   │   │       │   └── provider/                   # AI Provider System
│   │   │       │       ├── AiProvider.java         # Provider interface
│   │   │       │       ├── AiProviderManager.java  # Provider management
│   │   │       │       ├── AiResponse.java         # Response wrapper
│   │   │       │       ├── AiUsage.java            # Usage tracking
│   │   │       │       ├── ChatMessage.java        # Message structure
│   │   │       │       ├── ProviderCapabilities.java # Provider capabilities
│   │   │       │       ├── ProviderStatus.java     # Provider status
│   │   │       │       └── impl/
│   │   │       │           ├── AnthropicProvider.java # Anthropic Claude integration
│   │   │       │           └── OllamaProvider.java   # Ollama local AI integration
│   │   │       ├── auth/                           # Authentication System
│   │   │       │   └── AuthManager.java            # Token-based authentication
│   │   │       ├── codebase/                       # Code Analysis
│   │   │       │   ├── CodebaseAnalyzer.java       # Main analyzer
│   │   │       │   ├── DependencyInfo.java         # Dependency information
│   │   │       │   ├── FileInfo.java               # File metadata
│   │   │       │   ├── FileSearchOptions.java      # Search configuration
│   │   │       │   ├── FileSearchResult.java       # Search results
│   │   │       │   └── ProjectStructure.java       # Project structure analysis
│   │   │       ├── commands/                       # CLI Command System
│   │   │       │   ├── Command.java                # Command interface
│   │   │       │   ├── CommandExecutor.java        # Command execution service
│   │   │       │   ├── CommandRegistrationService.java # Command registration
│   │   │       │   ├── CommandRegistry.java        # Command registry
│   │   │       │   └── impl/                       # Command implementations
│   │   │       │       ├── AnalyzeCommand.java     # Code analysis command
│   │   │       │       ├── AskCommand.java         # AI question command
│   │   │       │       ├── ChatCommand.java        # Interactive chat with agent support
│   │   │       │       ├── ExplainCommand.java     # Code explanation command
│   │   │       │       ├── InfoCommand.java        # System information with agent status
│   │   │       │       ├── LoginCommand.java       # Authentication login
│   │   │       │       ├── LogoutCommand.java      # Authentication logout
│   │   │       │       ├── McpCommand.java         # MCP management with fixed parsing
│   │   │       │       ├── ProviderCommand.java    # AI provider management
│   │   │       │       └── StatusCommand.java      # System status
│   │   │       ├── config/                         # Configuration Management
│   │   │       │   ├── ApplicationConfig.java      # Main application configuration
│   │   │       │   ├── ConfigManager.java          # Configuration management
│   │   │       │   ├── DotenvConfiguration.java    # Environment variable configuration
│   │   │       │   ├── DotenvLoader.java           # .env file loader
│   │   │       │   ├── ExecutionConfiguration.java # Execution settings
│   │   │       │   ├── LogLevel.java               # Logging levels
│   │   │       │   ├── ReactorConfiguration.java   # Reactor configuration
│   │   │       │   └── TerminalTheme.java          # Terminal theming
│   │   │       ├── errors/                         # Error Handling
│   │   │       │   ├── ErrorFormatter.java         # Error formatting for display
│   │   │       │   └── UserError.java              # User-facing error type
│   │   │       ├── execution/                      # Code Execution
│   │   │       │   └── ExecutionEnvironment.java   # Execution environment management
│   │   │       ├── fileops/                        # File Operations
│   │   │       │   ├── CopyOptions.java            # Copy operation configuration
│   │   │       │   ├── FileOperations.java         # File operation utilities
│   │   │       │   ├── FindOptions.java            # Find operation configuration
│   │   │       │   └── WriteOptions.java           # Write operation configuration
│   │   │       ├── mcp/                            # Model Context Protocol
│   │   │       │   ├── client/
│   │   │       │   │   ├── McpClient.java          # Enhanced MCP client with SSE/WebSocket
│   │   │       │   │   └── McpClientNew.java       # New MCP client implementation
│   │   │       │   ├── config/
│   │   │       │   │   ├── McpCliOptions.java      # CLI options for MCP
│   │   │       │   │   ├── McpConfiguration.java   # MCP configuration
│   │   │       │   │   ├── McpConfigurationLoader.java # Configuration loader
│   │   │       │   │   └── McpConfigurationService.java # Configuration service
│   │   │       │   ├── manager/
│   │   │       │   │   └── McpServerManager.java   # Enhanced multi-server support
│   │   │       │   ├── model/
│   │   │       │   │   ├── McpResource.java        # MCP resource definitions
│   │   │       │   │   ├── McpTool.java            # MCP tool definitions
│   │   │       │   │   ├── McpToolCall.java        # Tool call structures
│   │   │       │   │   └── McpToolResult.java      # Tool result structures
│   │   │       │   └── protocol/
│   │   │       │       ├── McpError.java           # MCP error handling
│   │   │       │       ├── McpMessage.java         # Message structures
│   │   │       │       ├── McpNotification.java    # Notification handling
│   │   │       │       ├── McpRequest.java         # Request structures
│   │   │       │       └── McpResponse.java        # Response structures
│   │   │       ├── telemetry/                      # Usage Analytics
│   │   │       │   ├── TelemetryEventType.java     # Event type definitions
│   │   │       │   └── TelemetryService.java       # Telemetry collection service
│   │   │       ├── terminal/                       # Terminal Interface
│   │   │       │   ├── Terminal.java               # Terminal utilities
│   │   │       │   ├── TerminalConfig.java         # Terminal configuration
│   │   │       │   └── TerminalInterface.java      # Terminal interface
│   │   │       └── utils/                          # Utility Classes
│   │   │           ├── AsyncUtil.java              # Async operation utilities
│   │   │           ├── FormattingUtil.java         # Enhanced chat formatting
│   │   │           ├── LoggerUtil.java             # Logging utilities
│   │   │           ├── TypeUtil.java               # Type utilities
│   │   │           ├── UtilityModule.java          # Utility module
│   │   │           └── ValidationUtil.java         # Validation utilities
│   │   └── resources/
│   │       ├── META-INF/
│   │       │   └── spring.factories                # Spring factory configuration
│   │       ├── application.properties              # Updated with agent and MCP configs
│   │       └── mcp.json                            # MCP server configurations
│   └── test/
│       ├── java/sg/edu/nus/iss/misoto/
│       │   ├── MisotoApplicationTests.java          # Main application tests
│       │   └── cli/
│       │       ├── agent/                          # Agent System Tests
│       │       │   ├── AgentSystemIntegrationTest.java # Agent integration tests
│       │       │   ├── AgentSystemIntegrationTest_FIXED.java # Fixed integration tests
│       │       │   └── test/
│       │       │       └── TaskExecutionFixTest.java # Task execution fix tests
│       │       ├── auth/                           # Authentication Tests
│       │       │   └── AuthManagerTest.java        # Authentication manager tests
│       │       ├── codebase/                       # Codebase Analysis Tests
│       │       │   └── CodebaseAnalyzerTest.java   # Codebase analyzer tests
│       │       ├── config/                         # Configuration Tests
│       │       │   ├── ApplicationConfigTest.java  # Application config tests
│       │       │   └── ConfigManagerTest.java      # Config manager tests
│       │       ├── fileops/                        # File Operations Tests
│       │       │   └── FileOperationsTest.java     # File operations tests
│       │       ├── telemetry/                      # Telemetry Tests
│       │       │   └── TelemetryServiceTest.java   # Telemetry service tests
│       │       └── utils/                          # Utility Tests
│       │           ├── FormattingUtilTest.java     # Formatting utility tests
│       │           ├── UtilityModuleTest.java      # Utility module tests
│       │           └── ValidationUtilTest.java     # Validation utility tests
│       └── resources/
│           └── application-test.properties         # Test configuration properties
│
└── target/                              # Build Output Directory
    ├── classes/                         # Compiled classes
    ├── generated-sources/               # Generated source files
    ├── maven-archiver/                  # Maven archiver files
    ├── test-classes/                    # Compiled test classes
    └── task-execution-test-summary.json # Test execution summary
```

## Major System Components (Current as of June 17, 2025)

### 🤖 Agent System
Complete autonomous operation system with:
- **AgentService.java**: Main orchestrator with optimized shutdown (5s timeout)
- **Planning System**: ReAct-based planning with task decomposition
- **Task Management**: Priority-based queue with CRITICAL, HIGH, NORMAL, LOW levels
- **Decision Engine**: AI-powered autonomous decision making
- **State Management**: Persistent state across sessions
- **Multiple Modes**: INTERACTIVE, AUTONOMOUS, SUPERVISED, MANUAL

### 🔗 MCP (Model Context Protocol) Integration
Multi-server support with enhanced capabilities:
- **Multi-server Configuration**: Priority-ordered server connections
- **WebSocket + SSE Support**: Dual connection protocols
- **JSON Configuration**: Standalone mcp.json config system
- **Enhanced Tool Support**: Fixed argument parsing for proper tool execution
- **CLI Management**: Complete mcp command with subcommands

### 🧠 AI Provider System
Flexible AI integration supporting multiple providers:
- **Anthropic Provider**: Primary Claude AI integration via Spring AI
- **Ollama Provider**: Local AI model support
- **Provider Management**: Automatic failover and load balancing
- **Usage Tracking**: Token usage and cost monitoring
- **Capability Detection**: Dynamic feature detection per provider

### 🔧 Configuration Management
Comprehensive configuration system:
- **Multi-source Configuration**: Environment variables, .env files, JSON configs
- **Agent Configuration**: Timeout settings, execution intervals, task limits
- **MCP Configuration**: Server definitions, connection settings, tool mappings
- **Runtime Configuration**: Dynamic configuration updates

## Key Documentation Files

| Category | File | Status | Description |
|----------|------|---------|-------------|
| **Core** | README.md | ✅ Updated | Main documentation with comprehensive tables |
| **Development** | CLAUDE.md | ✅ Current | Build commands, architecture, development guide |
| **Features** | AGENT_MODE_README.md | ✅ Complete | Agent system architecture and usage |
| **Features** | PLANNING_SYSTEM_README.md | ✅ Complete | ReAct planning system documentation |
| **Migration** | PORTING_DOCUMENTATION.md | ✅ Complete | TypeScript to Java port documentation |
| **Status** | AGENT_IMPLEMENTATION_COMPLETE.md | ✅ Complete | Agent implementation completion |
| **Status** | MCP_CONFIG_MIGRATION_COMPLETED.md | ✅ Complete | MCP configuration migration |
| **Status** | MCP_MULTI_SERVER_COMPLETION.md | ✅ Complete | Multi-server MCP support |
| **Status** | PARAMETER_VALIDATION_ENHANCEMENT_COMPLETE.md | ✅ Complete | Parameter validation enhancements |
| **Tracking** | PROJECT_STRUCTURE_UPDATED.md | ✅ Current | This file - complete project structure |
| **Inventory** | MARKDOWN_FILES_INVENTORY.md | ✅ Current | Complete listing of all markdown files |

## Current Configuration Properties

```properties
# Agent System Configuration
misoto.agent.mode.enabled=${MISOTO_AGENT_MODE:false}
misoto.agent.max-concurrent-tasks=${MISOTO_AGENT_MAX_TASKS:3}
misoto.agent.execution-interval-ms=${MISOTO_AGENT_INTERVAL:5000}
misoto.agent.shutdown.timeout-seconds=${MISOTO_AGENT_SHUTDOWN_TIMEOUT:5}
misoto.agent.monitoring.shutdown.timeout-seconds=${MISOTO_AGENT_MONITORING_SHUTDOWN_TIMEOUT:3}

# AI Provider Configuration
spring.ai.anthropic.api-key=${ANTHROPIC_API_KEY}
spring.ai.anthropic.chat.model=claude-3-sonnet-20240229
spring.ai.anthropic.chat.temperature=0.7
spring.ai.anthropic.chat.max-tokens=4000

# MCP Configuration (via mcp.json)
# Configuration directory: ~/.misoto/ or project root

# Performance Optimized Logging
logging.level.sg.edu.nus.iss.misoto.cli=WARN
logging.level.sg.edu.nus.iss.misoto.cli.mcp=WARN
logging.level.root=WARN
```

## System Status

### ✅ Completed Features
- **Agent System**: Full autonomous operation with planning
- **MCP Integration**: Multi-server support with WebSocket/SSE
- **AI Providers**: Anthropic Claude + Ollama local AI
- **Configuration**: Comprehensive multi-source config system
- **CLI Commands**: Complete command set with help system
- **Testing**: Unit and integration test suites
- **Documentation**: Comprehensive documentation system
- **Build System**: Maven-based with PowerShell/Batch scripts

### 🎯 Current Capabilities
- Interactive chat with agent integration
- Autonomous task execution and planning
- Multi-server MCP tool execution
- Code analysis and explanation
- File operations and project analysis
- Token usage tracking and cost monitoring
- Cross-platform script support (Windows/Unix)

---

*Last updated: June 17, 2025*  
*Total Files: 100+ Java classes, 15+ markdown documentation files*  
*Architecture: Spring Boot 3.5 + Spring AI + Custom Agent System*
