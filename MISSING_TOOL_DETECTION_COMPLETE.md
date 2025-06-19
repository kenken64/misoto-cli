# Missing Tool Detection and Installation Suggestions - Implementation Complete

## Overview

The AI planning system has been enhanced with intelligent missing tool detection and OS-appropriate installation suggestions. When the agent plans tasks, it now automatically checks for required tools/commands and prompts the LLM to suggest installation commands specific to the user's operating system.

## Key Features Implemented

### 1. Automatic Tool Detection
**File**: `src/main/java/sg/edu/nus/iss/misoto/cli/agent/planning/PlanningService.java:1393-1673`

#### **checkAndSuggestMissingTools()** - Main Detection Method
- **Command Analysis**: Extracts tools from planned shell commands
- **Language Tool Check**: Verifies availability of programming language interpreters
- **Real-time Detection**: Runs before each subtask execution
- **Cross-platform Support**: Works on Windows, macOS, and Linux

#### **Tool Extraction Logic**:
```java
// Example command analysis:
"python3 app.py" -> extracts "python3"
"sudo apt install nodejs" -> extracts "apt" 
"docker run hello-world" -> extracts "docker"
"/usr/local/bin/custom-tool --flag" -> extracts "custom-tool"
```

### 2. Operating System Detection
**Method**: `getOperatingSystemInfo()`

#### **Comprehensive OS Analysis**:
- **Base OS Info**: Name, version, architecture
- **Package Manager Detection**: Homebrew, APT, YUM, DNF, Chocolatey, WinGet
- **Platform-specific Logic**: macOS, Linux distributions, Windows variants

#### **Example OS Detection Output**:
```
Mac OS X 15.4.1 (aarch64) - macOS with Homebrew
Ubuntu 22.04 (x86_64) - Linux with APT (Debian/Ubuntu)  
Windows 11 (amd64) - Windows with Chocolatey
```

### 3. AI-Powered Installation Suggestions
**Method**: `suggestToolInstallations()`

#### **Intelligent Suggestions**:
- **OS-Appropriate Commands**: Package manager specific to detected OS
- **Multiple Installation Methods**: Primary and alternative approaches
- **Verification Commands**: How to confirm successful installation
- **Safety Focus**: Only suggests well-known, safe installation methods

#### **Example AI Prompt**:
```
You are an expert system administrator helping to install missing tools/commands.

MISSING TOOLS: python3, docker
OPERATING SYSTEM: Mac OS X 15.4.1 (aarch64) - macOS with Homebrew

For each missing tool, provide the appropriate installation command for this operating system.
Consider multiple installation methods when available (package manager, official installer, etc.).
```

#### **Example AI Response**:
```
🔧 **python3** - Python programming language interpreter

**Installation Options:**
1. **Recommended**: brew install python3  (macOS with Homebrew)
2. **Alternative**: Download from https://python.org/downloads/

**Verify Installation**: python3 --version

🔧 **docker** - Containerization platform

**Installation Options:**
1. **Recommended**: brew install --cask docker
2. **Alternative**: Download Docker Desktop from docker.com

**Verify Installation**: docker --version
```

### 4. System Tool Awareness
**Method**: `checkAvailableSystemTools()`

#### **Development Tool Monitoring**:
- **Common Tools**: git, docker, python3, node, npm, java, maven, etc.
- **Availability Status**: Available vs Missing tools
- **AI Context**: Tools status included in reasoning prompts

#### **Example Tool Status**:
```
SYSTEM TOOL AVAILABILITY:
Available: git, java, javac, mvn, python3, node, npm
Missing: docker, go, rust, php, ruby
```

## Implementation Flow

### 1. Pre-Execution Check
```java
// Before each subtask execution:
checkAndSuggestMissingTools(subTask, execution);
```

### 2. Tool Detection Process
1. **Extract Commands**: Parse shell commands from subtask
2. **Check Availability**: Use `which`/`where` to verify tool presence
3. **Language Tools**: Check programming language interpreters
4. **Collect Missing**: Build list of unavailable tools

### 3. AI Suggestion Generation
1. **OS Detection**: Identify operating system and package managers
2. **Prompt Building**: Create detailed installation request for AI
3. **AI Query**: Get OS-appropriate installation suggestions
4. **User Display**: Show formatted installation instructions

### 4. Integration with Planning
- **Reasoning Phase**: AI receives tool availability status
- **Working Memory**: Missing tools stored for reference
- **Adaptive Planning**: AI can adjust plans based on available tools

## Real-World Examples

### Example 1: Python Development on macOS
```
Goal: "Create a Python web scraper"
```

**Detection**: `python3` missing
**AI Suggestion**:
```
🔧 **python3** - Python programming language interpreter

**Installation Options:**
1. **Recommended**: brew install python3
2. **Alternative**: Download from https://python.org/downloads/

**Verify Installation**: python3 --version
```

### Example 2: Docker Development on Ubuntu
```
Goal: "Containerize the application with Docker"
```

**Detection**: `docker` missing
**AI Suggestion**:
```
🔧 **docker** - Containerization platform

**Installation Options:**
1. **Recommended**: sudo apt update && sudo apt install docker.io
2. **Alternative**: curl -fsSL https://get.docker.com | sh

**Verify Installation**: docker --version
```

### Example 3: Node.js Project on Windows
```
Goal: "Create a React application"
```

**Detection**: `node`, `npm` missing
**AI Suggestion**:
```
🔧 **node** - JavaScript runtime environment

**Installation Options:**
1. **Recommended**: winget install OpenJS.NodeJS
2. **Alternative**: choco install nodejs (if Chocolatey installed)

**Verify Installation**: node --version && npm --version
```

## Benefits

### 1. **Proactive Problem Prevention**
- Detects missing tools before execution failures
- Prevents "command not found" errors
- Reduces debugging time and frustration

### 2. **OS-Appropriate Guidance**
- Tailored installation commands for each operating system
- Leverages existing package managers when available
- Provides multiple installation options

### 3. **Educational Value**
- Users learn proper tool installation methods
- Introduces best practices for development environment setup
- Builds understanding of system dependencies

### 4. **Seamless Integration**
- Automatic detection requires no user intervention
- Works with existing planning and execution flow
- Stores suggestions in working memory for reference

## User Experience Flow

### 1. **Normal Operation** (All tools available)
```
🎯 Working on: Set up Python development environment [HIGH]
🚀 Executing step: Create virtual environment [SHELL_COMMAND]
✅ Step completed successfully
```

### 2. **Missing Tool Detected**
```
🎯 Working on: Set up Python development environment [HIGH]
⚠️  Missing tools detected for subtask: Set up Python development environment

🔧 AI Installation Suggestions:
🔧 **python3** - Python programming language interpreter

**Installation Options:**
1. **Recommended**: brew install python3
2. **Alternative**: Download from https://python.org/downloads/

**Verify Installation**: python3 --version

💡 Would you like to install these tools automatically? 
(This will be handled in the next planning cycle)
```

### 3. **Informed Planning**
- AI receives tool availability status in reasoning prompts
- Plans are adapted based on available vs missing tools
- Alternative approaches suggested when tools are unavailable

## Implementation Status

✅ **Tool detection implemented** - Automatic command and language tool checking  
✅ **OS detection implemented** - Comprehensive operating system and package manager identification  
✅ **AI integration implemented** - LLM generates OS-appropriate installation suggestions  
✅ **Cross-platform support** - Windows, macOS, and Linux compatibility  
✅ **Command extraction** - Smart parsing of shell commands to identify required tools  
✅ **System tool awareness** - AI receives tool availability context  
✅ **Working memory integration** - Missing tools stored for planning reference  
✅ **User-friendly output** - Clear, formatted installation instructions  
✅ **Build verification completed** - No compilation errors  

## User Request Fulfilled

> *"in the agent mode when plan if there is a missing command or tools that is not available is it possible to prompt the llm model to suggest for installation command that is for the operating system shell."*

✅ **Missing tool detection** - Automatically identifies unavailable commands/tools  
✅ **LLM integration** - AI suggests installation commands specific to detected OS  
✅ **Shell command specificity** - Installation suggestions tailored to user's shell/OS  
✅ **Proactive planning** - Detection happens before execution failures  
✅ **Multi-platform support** - Works across Windows, macOS, and Linux  

The agent now intelligently detects missing tools and leverages AI to provide platform-specific installation guidance, making the development experience smoother and more educational.