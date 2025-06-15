# Misoto Scripts

This directory contains convenience scripts for running Misoto with different configurations.

## Agent Chat Scripts

These scripts start Misoto in interactive chat mode with the agent system enabled:

### Windows

- **`start-agent-chat.ps1`** - PowerShell script (recommended for Windows)
- **`start-agent-chat.bat`** - Batch script (alternative for Windows)

### Unix/Linux/Mac

- **`start-agent-chat.sh`** - Shell script for Unix-based systems

## Usage

### PowerShell (Windows)
```powershell
.\scripts\start-agent-chat.ps1
```

### Batch (Windows)
```cmd
scripts\start-agent-chat.bat
```

### Shell (Unix/Linux/Mac)
```bash
./scripts/start-agent-chat.sh
```

## What These Scripts Do

1. **Enable Agent Mode**: Set `MISOTO_AGENT_MODE=true`
2. **Navigate to Root**: Change to the project root directory
3. **Validate JAR**: Check if the built JAR file exists
4. **Start Chat**: Launch Misoto in interactive chat mode with agent enabled

## Prerequisites

- The project must be built first: `mvn clean package -DskipTests`
- Java must be installed and available in PATH
- `ANTHROPIC_API_KEY` must be set in your environment or `.env` file

## Agent Features

When agent mode is enabled, you can:

- Use `/agent` in chat to access agent controls
- Submit tasks to the autonomous agent
- Monitor agent status and performance
- Change agent modes (INTERACTIVE, AUTONOMOUS, SUPERVISED, MANUAL)
- Start/stop the agent as needed

The agent runs in the background while you chat, providing autonomous assistance and task processing capabilities.
