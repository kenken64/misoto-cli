# ReAct Planning System

## Overview

The Misoto CLI now includes a sophisticated ReAct (Reasoning + Acting) planning system that can break down complex goals into manageable subtasks and execute them autonomously. This system implements advanced AI planning architectures including task decomposition, dynamic planning, and memory management.

## Architecture

### ReAct (Reasoning + Acting) Cycles

The planning system follows the ReAct methodology:

1. **Reasoning Phase**: Analyze current state and decide next action
2. **Acting Phase**: Execute the decided action 
3. **Observation Phase**: Analyze results of the action
4. **Self-reflection**: Evaluate success and decide if replanning is needed

### Planning-First Architecture

The system breaks tasks into phases:

1. **Task Decomposition**: Break high-level goals into smaller subtasks
2. **Plan Generation**: Create a sequence of actions to accomplish each subtask  
3. **Execution**: Carry out the plan step by step
4. **Monitoring**: Check progress and replan if needed

### Memory Architecture

- **Working Memory**: Tracks current task state and context
- **Long-term Memory**: Stores learned patterns and previous task solutions
- **Episodic Memory**: Remembers specific interactions and outcomes

## Usage

### Starting a Plan

In agent mode, use the `/plan` command:

```
/agent
plan create a complete web application with frontend and backend
```

### Available Commands

- `plan <goal>` - Create and execute a ReAct-based plan
- `plans` - List active plans and their status

### Example Goals

```bash
# Web development
plan create a REST API with authentication and database

# File management
plan organize project files and create documentation

# Code analysis
plan analyze codebase for security vulnerabilities and performance issues

# Data processing
plan process CSV data and generate visualization reports
```

## Plan Structure

### Subtasks

Each plan is decomposed into subtasks with:
- **Description**: Clear, specific description
- **Expected Outcome**: What should result
- **Priority**: CRITICAL, HIGH, MEDIUM, LOW
- **Complexity**: SIMPLE, MODERATE, COMPLEX
- **Dependencies**: Prerequisites from other subtasks

### Execution Strategy

Plans include:
- **Execution Order**: Optimal sequence of subtasks
- **Parallel Opportunities**: Tasks that can run simultaneously
- **Risk Mitigation**: Strategies for handling failures
- **Tool Selection**: Appropriate tools for each subtask
- **Monitoring Points**: Key milestones to track

## ReAct Cycle Details

### 1. Reasoning Phase

The agent analyzes:
- Current information available
- Information still needed
- Most appropriate tools to use
- Potential risks or dependencies
- How the task fits into the overall plan

### 2. Acting Phase

The agent:
- Determines specific action type (FILE_OPERATIONS, SHELL_COMMANDS, CODE_GENERATION, etc.)
- Creates detailed action parameters
- Executes the action via agent tasks
- Monitors execution results

### 3. Observation Phase

The agent evaluates:
- Whether the action achieved its goal
- New information discovered
- Unexpected results or errors
- Implications for next steps
- Need for plan adaptation

## Tool Integration

The planning system can use all available agent tools:

- **FILE_OPERATIONS**: Read, write, copy, delete files
- **SHELL_COMMANDS**: Execute system commands
- **CODE_GENERATION**: Generate and execute code
- **AI_ANALYSIS**: Analyze data and make decisions
- **MCP_TOOLS**: Use Model Context Protocol tools

## Example Output

```
🧠 Creating ReAct plan for goal: create a complete web application

📋 Phase 1: Task Decomposition and Planning...
✅ Plan created with 6 subtasks (ID: plan-1703123456789)

📊 Plan Details:
──────────────────────────────────────────────────
Goal: create a complete web application with frontend and backend
Plan ID: plan-1703123456789
Status: CREATED

🔍 Subtasks:
  1. Set up project structure and dependencies (HIGH)
     Expected: Project initialized with proper folder structure
  2. Create backend API with database (HIGH)
     Expected: RESTful API with CRUD operations
  3. Implement authentication system (CRITICAL)
     Expected: Secure user login and registration
  4. Build frontend user interface (MEDIUM)
     Expected: Responsive web interface
  5. Integrate frontend with backend (HIGH)
     Expected: Fully connected application
  6. Deploy and test application (MEDIUM)
     Expected: Working deployed application

⚡ Phase 2: ReAct Execution (Reasoning + Acting cycles)...

🔄 ReAct Cycles (Reasoning → Acting → Observation):

Cycle 1: subtask-1
  🤔 Reasoning:
     I need to set up a project structure. First, I should create the main directory...
  ⚡ Action:
     Create project directories and initialize package.json
  👁️ Observation:
     Successfully created project structure with frontend and backend folders...
  Status: COMPLETED

[Additional cycles...]

🎯 Plan Execution Results:
────────────────────────────────────────────────────────────
Execution Status: COMPLETED
Total Execution Time: 45000 ms
Steps Completed: 6

🧠 Final Working Memory:
  • last_action: Deploy application to production server
  • project_structure: Complete with frontend, backend, database
  • deployment_url: https://myapp.example.com
```

## Advanced Features

### Dynamic Replanning

If a subtask fails, the system can:
- Analyze the failure cause
- Generate alternative approaches
- Modify remaining subtasks
- Continue execution with updated plan

### Parallel Execution

The system identifies subtasks that can run simultaneously:
- Independent tasks with no dependencies
- Resource-compatible operations
- Time-efficient execution ordering

### Context Awareness

Plans consider:
- Current conversation context
- Previous successful patterns
- Available system resources
- User preferences and constraints

## Configuration

The planning system respects agent configuration:
- Maximum concurrent tasks
- Execution timeouts
- Memory limits
- Tool availability

## Error Handling

Robust error handling includes:
- Graceful failure recovery
- Automatic retry with exponential backoff
- Alternative strategy selection
- User notification of critical failures

## Performance

- Efficient task decomposition using AI reasoning
- Optimized execution ordering
- Memory-conscious operation
- Scalable to complex, multi-step goals

## Future Enhancements

Planned improvements:
- Learning from successful plans
- User preference adaptation
- Integration with external planning tools
- Advanced dependency resolution
- Collaborative multi-agent planning