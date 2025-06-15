# Claude Code Markdown - Project Structure Documentation

## Updated Project Structure (Current as of June 15, 2025)

```
claude-code-markdown/
├── README.md
├── PORTING_DOCUMENTATION.md  
├── package.md
├── package-lock.md
│
├── claude-code/                          # TypeScript CLI implementation
│   ├── LICENSE.md
│   ├── package.md
│   ├── tsconfig.md
│   ├── scripts/
│   │   └── preinstall.md
│   └── src/
│       ├── cli.md
│       ├── cli.ts
│       ├── index.md
│       ├── index.ts
│       ├── ai/                           # AI client implementations
│       │   ├── client.md
│       │   ├── client.ts
│       │   ├── index.md
│       │   ├── index.ts
│       │   ├── prompts.md
│       │   ├── prompts.ts
│       │   ├── types.md
│       │   └── types.ts
│       ├── auth/                         # Authentication system
│       │   ├── index.md
│       │   ├── index.ts
│       │   ├── manager.md
│       │   ├── manager.ts
│       │   ├── oauth.md
│       │   ├── oauth.ts
│       │   ├── tokens.md
│       │   ├── tokens.ts
│       │   ├── types.md
│       │   └── types.ts
│       ├── codebase/                     # Code analysis tools
│       │   ├── analyzer.md
│       │   ├── analyzer.ts
│       │   └── index.md
│       ├── commands/                     # CLI commands
│       │   ├── index.md
│       │   └── register.md
│       ├── config/                       # Configuration management
│       │   ├── defaults.md
│       │   ├── index.md
│       │   └── schema.md
│       ├── errors/                       # Error handling
│       │   ├── console.md
│       │   ├── formatter.md
│       │   ├── index.md
│       │   ├── sentry.md
│       │   └── types.md
│       ├── execution/                    # Code execution
│       │   └── index.md
│       ├── fileops/                      # File operations
│       │   └── index.md
│       ├── fs/                          # File system utilities
│       │   └── operations.md
│       ├── telemetry/                   # Usage analytics
│       │   └── index.md
│       ├── terminal/                    # Terminal interface
│       │   ├── formatting.md
│       │   ├── index.md
│       │   ├── prompt.md
│       │   └── types.md
│       └── utils/                       # Utility functions
│           ├── async.md
│           ├── formatting.md
│           ├── index.md
│           ├── logger.md
│           ├── types.md
│           └── validation.md
│
├── mcp-server/                          # Model Context Protocol Server
│   ├── build.gradle
│   ├── gradlew
│   ├── gradlew.bat
│   ├── pom.xml
│   ├── settings.gradle
│   ├── README.md
│   ├── TESTING_RESULTS.md
│   ├── test-*.ps1                       # PowerShell test scripts
│   ├── simple-*.ps1                    # Simple test scripts
│   ├── .github/
│   │   └── copilot-instructions.md
│   ├── build/
│   │   └── reports/
│   ├── gradle/
│   │   └── wrapper/
│   ├── src/
│   │   ├── main/
│   │   └── test/
│   └── target/
│       └── classes/
│
├── misoto/                              # Java Spring Boot Implementation
│   ├── pom.xml
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── README.md
│   ├── CLI-README.md
│   ├── AGENT_MODE_README.md
│   ├── AGENT_IMPLEMENTATION_COMPLETE.md
│   ├── MCP_CONFIG_MIGRATION_COMPLETED.md
│   ├── MCP_MULTI_SERVER_COMPLETION.md
│   ├── misoto.log
│   ├── backup-config.json
│   ├── claude-code.bat
│   ├── claude-code.ps1
│   ├── test-*.txt                       # Test conversation files
│   │
│   ├── scripts/                         # Startup and utility scripts
│   │   ├── README.md
│   │   ├── build-and-run.bat
│   │   ├── build-and-run.ps1
│   │   ├── run.bat
│   │   ├── run.ps1
│   │   ├── start-agent-chat.bat         # Agent mode startup scripts
│   │   ├── start-agent-chat.ps1
│   │   ├── start-agent-chat.sh
│   │   └── test-*.ps1                   # Various test scripts
│   │
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/sg/edu/nus/iss/misoto/
│   │   │   │   ├── MisotoApplication.java        # Main Spring Boot application
│   │   │   │   └── cli/
│   │   │   │       ├── agent/                    # **NEW: Agent system**
│   │   │   │       │   ├── AgentService.java     # Main agent orchestrator
│   │   │   │       │   ├── commands/
│   │   │   │       │   │   └── AgentCommands.java
│   │   │   │       │   ├── config/
│   │   │   │       │   │   ├── AgentConfiguration.java
│   │   │   │       │   │   └── AgentSystemConfiguration.java
│   │   │   │       │   ├── decision/
│   │   │   │       │   │   └── DecisionEngine.java
│   │   │   │       │   ├── monitoring/
│   │   │   │       │   │   └── MonitoringService.java
│   │   │   │       │   ├── state/
│   │   │   │       │   │   └── AgentStateManager.java
│   │   │   │       │   └── task/
│   │   │   │       │       ├── AgentTask.java
│   │   │   │       │       ├── TaskExecutorService.java
│   │   │   │       │       └── TaskQueueService.java
│   │   │   │       ├── ai/                       # AI client integration
│   │   │   │       │   └── AiClient.java
│   │   │   │       ├── auth/                     # Authentication
│   │   │   │       │   ├── AuthManager.java
│   │   │   │       │   └── TokenManager.java
│   │   │   │       ├── codebase/                 # Code analysis
│   │   │   │       │   └── CodebaseAnalyzer.java
│   │   │   │       ├── commands/                 # CLI command system
│   │   │   │       │   ├── Command.java
│   │   │   │       │   ├── CommandContext.java
│   │   │   │       │   ├── CommandRegistrationService.java
│   │   │   │       │   └── impl/
│   │   │   │       │       ├── ChatCommand.java      # **Enhanced with agent mode**
│   │   │   │       │       ├── InfoCommand.java      # **Enhanced with agent status**
│   │   │   │       │       ├── McpCommand.java       # **Fixed argument parsing**
│   │   │   │       │       ├── AuthCommand.java
│   │   │   │       │       ├── CodebaseCommand.java
│   │   │   │       │       ├── ExecuteCommand.java
│   │   │   │       │       ├── HelpCommand.java
│   │   │   │       │       └── SearchCommand.java
│   │   │   │       ├── config/                   # Configuration management
│   │   │   │       │   ├── ApplicationConfig.java
│   │   │   │       │   ├── ConfigManager.java
│   │   │   │       │   ├── DotenvConfiguration.java
│   │   │   │       │   ├── ExecutionConfiguration.java
│   │   │   │       │   ├── LogLevel.java
│   │   │   │       │   └── TerminalTheme.java
│   │   │   │       ├── errors/                   # Error handling
│   │   │   │       │   ├── ErrorFormatter.java
│   │   │   │       │   └── UserError.java
│   │   │   │       ├── execution/                # Code execution
│   │   │   │       │   └── ExecutionEnvironment.java
│   │   │   │       ├── fileops/                  # File operations
│   │   │   │       │   ├── CopyOptions.java
│   │   │   │       │   ├── FileOperations.java
│   │   │   │       │   ├── FindOptions.java
│   │   │   │       │   └── WriteOptions.java
│   │   │   │       ├── fs/                       # File system utilities
│   │   │   │       │   └── PathUtil.java
│   │   │   │       ├── mcp/                      # **Model Context Protocol**
│   │   │   │       │   ├── client/
│   │   │   │       │   │   ├── McpClient.java         # **Enhanced with SSE/WebSocket**
│   │   │   │       │   │   └── McpClientNew.java
│   │   │   │       │   ├── config/
│   │   │   │       │   │   ├── McpCliOptions.java
│   │   │   │       │   │   ├── McpConfiguration.java
│   │   │   │       │   │   ├── McpConfigurationLoader.java
│   │   │   │       │   │   └── McpConfigurationService.java
│   │   │   │       │   ├── manager/
│   │   │   │       │   │   └── McpServerManager.java  # **Enhanced multi-server support**
│   │   │   │       │   ├── model/
│   │   │   │       │   │   ├── McpResource.java
│   │   │   │       │   │   ├── McpTool.java
│   │   │   │       │   │   ├── McpToolCall.java
│   │   │   │       │   │   └── McpToolResult.java
│   │   │   │       │   └── protocol/
│   │   │   │       │       ├── McpError.java
│   │   │   │       │       ├── McpMessage.java
│   │   │   │       │       ├── McpNotification.java
│   │   │   │       │       ├── McpRequest.java
│   │   │   │       │       └── McpResponse.java
│   │   │   │       ├── telemetry/                # Usage analytics
│   │   │   │       │   ├── TelemetryEventType.java
│   │   │   │       │   └── TelemetryService.java
│   │   │   │       ├── terminal/                 # Terminal interface
│   │   │   │       │   ├── Terminal.java
│   │   │   │       │   ├── TerminalConfig.java
│   │   │   │       │   └── TerminalInterface.java
│   │   │   │       └── utils/                    # Utility classes
│   │   │   │           ├── AsyncUtil.java
│   │   │   │           ├── FormattingUtil.java    # **Enhanced chat formatting**
│   │   │   │           ├── LoggerUtil.java
│   │   │   │           ├── TypeUtil.java
│   │   │   │           ├── UtilityModule.java
│   │   │   │           └── ValidationUtil.java
│   │   │   └── resources/
│   │   │       ├── application.properties         # **Updated with agent configs**
│   │   │       └── application-test.properties    # **Updated logging config**
│   │   └── test/
│   │       └── java/sg/edu/nus/iss/misoto/
│   │           ├── MisotoApplicationTests.java
│   │           └── cli/
│   │               ├── agent/
│   │               │   ├── AgentSystemIntegrationTest.java
│   │               │   └── AgentSystemIntegrationTest_FIXED.java
│   │               ├── auth/
│   │               │   └── AuthManagerTest.java
│   │               ├── codebase/
│   │               │   └── CodebaseAnalyzerTest.java
│   │               ├── config/
│   │               │   ├── ApplicationConfigTest.java
│   │               │   └── ConfigManagerTest.java
│   │               ├── fileops/
│   │               │   └── FileOperationsTest.java
│   │               ├── telemetry/
│   │               │   └── TelemetryServiceTest.java
│   │               └── utils/
│   │                   ├── FormattingUtilTest.java
│   │                   ├── UtilityModuleTest.java
│   │                   └── ValidationUtilTest.java
│   └── target/
│       ├── misoto-0.0.1-SNAPSHOT.jar
│       ├── classes/
│       ├── generated-sources/
│       ├── maven-archiver/
│       └── test-classes/
│
└── specs/                               # Documentation and specifications
    ├── architecture.md
    ├── command_reference.md
    ├── development.md
    ├── error_handling.md
    ├── features.md
    ├── index.md
    ├── installation.md
    ├── integration.md
    ├── LICENSE.md
    ├── overview.md
    └── performance.md
```

## Recent Major Changes and Additions

### 🤖 Agent System (NEW)
- **AgentService.java**: Main orchestrator for autonomous behavior with configurable shutdown timeouts
- **AgentConfiguration.java**: Configuration with performance optimizations (5s shutdown, 3s monitoring timeout)
- **AgentSystemConfiguration.java**: Spring configuration with property injection
- **MonitoringService.java**: File system and resource monitoring with optimized shutdown
- **TaskQueueService.java**: Asynchronous task management
- **DecisionEngine.java**: AI-powered decision making
- **AgentCommands.java**: CLI integration for agent control

### 💬 Enhanced Chat System
- **ChatCommand.java**: 
  - Added `/agent` command for interactive agent control
  - Token usage tracking with `/usage` and `/cost` commands
  - Improved UI with session summaries
  - Agent mode integration with proper exit handling

### 🔧 MCP (Model Context Protocol) Improvements
- **McpCommand.java**: Fixed argument parsing for tool calls (echo now works correctly)
- **McpServerManager.java**: Enhanced multi-server support
- **McpClient.java**: Added SSE and WebSocket connectivity

### 📊 Performance Optimizations
- **Reduced shutdown timeouts**: Agent (5s) and Monitoring (3s) for faster CLI exit
- **Configurable timeouts**: Via application.properties
- **Improved logging**: Less verbose output for better user experience

### 🚀 Startup Scripts
- **start-agent-chat.ps1/.sh/.bat**: Quick agent mode startup scripts
- **Enhanced build scripts**: Streamlined development workflow

---

## Configuration Properties (Updated)

```properties
# Agent Mode Configuration (NEW/UPDATED)
misoto.agent.mode.enabled=${MISOTO_AGENT_MODE:false}
misoto.agent.max-concurrent-tasks=${MISOTO_AGENT_MAX_TASKS:3}
misoto.agent.execution-interval-ms=${MISOTO_AGENT_INTERVAL:5000}
misoto.agent.shutdown.timeout-seconds=${MISOTO_AGENT_SHUTDOWN_TIMEOUT:5}
misoto.agent.monitoring.shutdown.timeout-seconds=${MISOTO_AGENT_MONITORING_SHUTDOWN_TIMEOUT:3}

# Performance Optimized Logging
logging.level.sg.edu.nus.iss.misoto.cli=WARN
logging.level.sg.edu.nus.iss.misoto.cli.mcp=WARN
logging.level.root=WARN
```

## Key Features Status

✅ **Completed:**
- Agent mode with chat integration
- MCP tool argument parsing fix
- Performance optimizations (fast exit)
- Token usage tracking
- Multi-MCP server support
- Agent state management
- Interactive agent commands
- Startup scripts for agent mode

🔄 **In Progress:**
- MCP tool call verification testing
- Advanced agent decision making
- Resource monitoring enhancements

---

*Last updated: June 15, 2025*
