# Misoto Agent Mode

## Overview

Misoto Agent Mode enables autonomous operation, allowing the system to monitor triggers, make decisions, and execute tasks without manual intervention. The agent system is built with modular components that work together to provide intelligent automation.

## Architecture

The agent system consists of several key components:

### Core Components

1. **AgentService** - Main orchestrator that manages the agent lifecycle
2. **AgentStateManager** - Handles persistent state and session management
3. **TaskQueueService** - Manages task queuing, execution, and cleanup
4. **TaskExecutorService** - Executes different types of tasks
5. **DecisionEngine** - Makes intelligent decisions based on context
6. **MonitoringService** - Watches for triggers and schedules tasks
7. **AgentConfiguration** - Centralized configuration management

### Task Types

- **SHELL** - Execute shell commands
- **FILE** - File operations (read, write, copy, etc.)
- **AI** - AI-powered tasks using Claude
- **MCP** - Model Context Protocol operations
- **SYSTEM** - System maintenance and monitoring
- **COMPOSITE** - Multi-step tasks
- **CUSTOM** - Extensible custom task types

### Priority Levels

- **CRITICAL** - Immediate execution
- **HIGH** - High priority, executed before normal tasks
- **NORMAL** - Standard priority (default)
- **LOW** - Low priority, executed when resources available

## Configuration

### Environment Variables

```bash
# Enable/disable agent mode
export MISOTO_AGENT_MODE=true

# Maximum concurrent tasks
export MISOTO_AGENT_MAX_TASKS=3

# Execution interval in milliseconds
export MISOTO_AGENT_INTERVAL=5000

# Enable auto-save of agent state
export MISOTO_AGENT_AUTO_SAVE=true

# Agent state file path
export MISOTO_AGENT_STATE_FILE=agent-state.json

# Backup retention period in days
export MISOTO_AGENT_BACKUP_DAYS=7
```

### Application Properties

```properties
# Agent Mode Configuration
misoto.agent.mode.enabled=true
misoto.agent.max-concurrent-tasks=3
misoto.agent.execution-interval-ms=5000
misoto.agent.auto-save.enabled=true
misoto.agent.state.file-path=agent-state.json
misoto.agent.backup.retention-days=7
```

### Monitoring Triggers

Configure monitoring triggers in the AgentConfiguration:

```java
AgentConfiguration config = AgentConfiguration.builder()
    .agentModeEnabled(true)
    .monitoringTrigger(MonitoringTrigger.builder()
        .name("file-watcher")
        .type("file_change")
        .path("/path/to/watch")
        .pattern("*.log")
        .action("shell")
        .command("process-log.sh")
        .build())
    .monitoringTrigger(MonitoringTrigger.builder()
        .name("scheduled-backup")
        .type("scheduled")
        .schedule("1h")
        .action("system")
        .build())
    .build();
```

## Usage

### CLI Commands

#### Basic Agent Control

```bash
# Enable agent mode
agent-config --enable

# Start the agent
agent-start

# Stop the agent
agent-stop

# Show agent status
agent-status
```

#### Task Management

```bash
# Submit a shell command task
agent-task --type SHELL --command "ls -la" --priority HIGH

# Submit an AI task
agent-task --type AI --description "analyze recent logs"

# List recent tasks
agent-tasks --limit 20

# Clear completed tasks
agent-clear
```

#### Configuration

```bash
# Set maximum concurrent tasks
agent-config --max-tasks 5

# Set execution interval
agent-config --interval 3000

# Enable auto-save
agent-config --auto-save
```

### Programmatic Usage

#### Starting the Agent

```java
@Autowired
private AgentService agentService;

// Start the agent
agentService.startAgent();

// Check if running
boolean isRunning = agentService.isRunning();

// Get status
AgentService.AgentStatus status = agentService.getStatus();
```

#### Submitting Tasks

```java
@Autowired
private AgentService agentService;

// Create a task
AgentTask task = AgentTask.builder()
    .type(AgentTask.TaskType.SHELL)
    .priority(AgentTask.Priority.HIGH)
    .description("Execute system check")
    .context(Map.of("command", "system-check.sh"))
    .createdAt(LocalDateTime.now())
    .build();

// Submit the task
agentService.submitTask(task);
```

## Monitoring and Triggers

The monitoring service supports various trigger types:

### File System Monitoring

```java
MonitoringTrigger.builder()
    .name("log-monitor")
    .type("file_change")
    .path("/var/log/app")
    .pattern("error.*\\.log")
    .action("ai")
    .command("analyze-error-logs")
    .build()
```

### Scheduled Tasks

```java
MonitoringTrigger.builder()
    .name("daily-backup")
    .type("scheduled")
    .schedule("24h")
    .action("shell")
    .command("backup.sh")
    .build()
```

### System Metrics

```java
MonitoringTrigger.builder()
    .name("memory-alert")
    .type("system_metric")
    .threshold("80")
    .action("system")
    .build()
```

## State Management

The agent maintains persistent state across sessions:

- **Task History** - Records of all executed tasks
- **Statistics** - Performance metrics and counters
- **Configuration** - Runtime configuration changes
- **Backups** - Automatic state backups with retention

### State File Structure

```json
{
  "agentId": "agent-uuid",
  "startTime": "2024-01-15T10:30:00",
  "lastActivity": "2024-01-15T14:25:30",
  "totalTasksExecuted": 127,
  "successfulTasks": 119,
  "failedTasks": 8,
  "uptime": 14100,
  "taskHistory": [...],
  "statistics": {...},
  "configuration": {...}
}
```

## Decision Engine

The decision engine provides AI-powered decision making:

### Context Analysis

The engine analyzes:
- Current system state
- Task queue statistics
- Historical performance
- Resource availability
- Error patterns

### Decision Types

- **Task Prioritization** - Intelligent task ordering
- **Resource Management** - Optimal resource allocation
- **Error Handling** - Adaptive error recovery
- **Strategy Selection** - Dynamic strategy adaptation

## Integration with Existing Systems

### MCP Integration

The agent integrates with Model Context Protocol servers:

```java
AgentTask mcpTask = AgentTask.builder()
    .type(AgentTask.TaskType.MCP)
    .context(Map.of(
        "server", "filesystem-server",
        "tool", "read_file",
        "args", Map.of("path", "/path/to/file")
    ))
    .build();
```

### AI Integration

AI tasks leverage Claude for intelligent processing:

```java
AgentTask aiTask = AgentTask.builder()
    .type(AgentTask.TaskType.AI)
    .context(Map.of(
        "prompt", "Analyze the following logs and provide insights",
        "data", logData
    ))
    .build();
```

## Testing

Run the integration tests:

```bash
# Run all agent tests
mvn test -Dtest=*AgentTest*

# Run integration tests
mvn test -Dtest=AgentSystemIntegrationTest
```

## Troubleshooting

### Common Issues

1. **Agent won't start**
   - Check that agent mode is enabled in configuration
   - Verify required dependencies are available
   - Check logs for startup errors

2. **Tasks not executing**
   - Verify task queue is accepting tasks
   - Check task executor configuration
   - Monitor resource availability

3. **High memory usage**
   - Adjust max concurrent tasks
   - Enable task queue cleanup
   - Check for memory leaks in custom tasks

### Debug Mode

Enable debug logging:

```properties
logging.level.sg.edu.nus.iss.misoto.cli.agent=DEBUG
```

### Health Checks

The monitoring service performs automatic health checks:
- Task queue responsiveness
- Memory usage monitoring
- Stuck task detection
- Resource availability

## Future Enhancements

Planned improvements:

1. **Enhanced Triggers** - Cron expressions, webhook triggers
2. **Distributed Mode** - Multi-node agent coordination
3. **Plugin System** - Custom task type plugins
4. **Web Dashboard** - Real-time monitoring interface
5. **Machine Learning** - Predictive task scheduling
6. **Event Streaming** - Real-time event processing

## Security Considerations

- Task validation and sandboxing
- Command injection prevention
- Resource usage limits
- Access control for sensitive operations
- Audit logging for all agent actions

## Performance

Typical performance characteristics:
- Task throughput: 100+ tasks/minute
- Memory usage: 50-200MB base
- CPU usage: 1-5% idle, 10-30% active
- Storage: 1-10MB for state files

## Support

For issues and questions:
- Check the troubleshooting section
- Review debug logs
- Submit issues on the project repository
- Consult the API documentation
