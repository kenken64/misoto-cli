# ReAct Planning System Test Demonstration

## Overview
This document demonstrates the ReAct (Reasoning + Acting) planning system with verbose LLM model responses.

## Enhanced Logging Features

The planning system now includes comprehensive logging to show all AI model interactions:

1. **Task Decomposition Phase**: Shows AI prompts and responses for breaking down goals
2. **Strategy Generation Phase**: Shows AI reasoning for planning approach  
3. **ReAct Cycles**: Shows detailed Reasoning → Acting → Observation phases
4. **Self-Reflection**: Shows AI evaluation of task completion

## Test Commands

### 1. Basic Plan Test
```bash
java -jar target/misoto-0.0.1-SNAPSHOT.jar chat
/agent
plan create a simple Python calculator script
```

### 2. Complex Plan Test  
```bash
java -jar target/misoto-0.0.1-SNAPSHOT.jar chat
/agent
plan create a complete web application with frontend and backend
```

### 3. File Organization Test
```bash
java -jar target/misoto-0.0.1-SNAPSHOT.jar chat
/agent  
plan organize project files and create comprehensive documentation
```

## Expected Verbose Output

When running a plan, you should see detailed logging like:

```
🧠 Creating ReAct plan for goal: create a simple Python calculator script

🤖 Sending task decomposition prompt to AI...
AI Response: I'll break this down into manageable subtasks:

SUBTASK_1:
Description: Set up project structure and create main Python file
Expected Outcome: calculator.py file created with basic structure
Priority: HIGH
Complexity: SIMPLE
Dependencies: NONE

SUBTASK_2:
Description: Implement basic arithmetic operations (add, subtract, multiply, divide)
Expected Outcome: Working calculator functions for basic math operations
Priority: HIGH
Complexity: MODERATE
Dependencies: SUBTASK_1

[Additional subtasks...]

🧠 Sending strategy generation prompt to AI...
AI Strategy Response: For this calculator project, I recommend:
1. Sequential execution starting with file creation
2. Test each operation as it's implemented
3. Add input validation and error handling
4. Include user interface for interaction

⚡ Phase 2: ReAct Execution (Reasoning + Acting cycles)...

🤔 ReAct Phase: REASONING for subtask subtask-1
🧠 AI Reasoning: I need to create the initial project structure. First, I should create a new Python file called calculator.py. This will serve as the foundation for our calculator application...

⚡ ReAct Phase: ACTING for subtask subtask-1  
🎯 AI Action Decision: 
ACTION_TYPE: FILE_OPERATIONS
ACTION_DESCRIPTION: Create calculator.py file with basic structure and imports
PARAMETERS: filename=calculator.py, content=basic calculator template
EXPECTED_OUTCOME: Python file created ready for implementation

👁️ ReAct Phase: OBSERVATION for action: Create calculator.py file
🔍 AI Observation: The file was successfully created. I can see that calculator.py now exists with the basic structure. The file includes the necessary imports and a main function template...

🔬 ReAct Phase: SELF-REFLECTION for subtask subtask-1
✅ AI Success Evaluation: YES - The subtask has been completed successfully. The calculator.py file has been created with the proper structure...

📊 Subtask subtask-1 evaluated as: SUCCESS

[Additional ReAct cycles for remaining subtasks...]
```

## Key Logging Features

### 1. Task Decomposition Logging
- Shows the full prompt sent to AI for task breakdown
- Displays AI's complete response with subtask details
- Logs the parsing results

### 2. Strategy Generation Logging  
- Shows strategic planning prompt
- Displays AI's comprehensive strategy response
- Logs execution order and risk mitigation

### 3. ReAct Cycle Logging
Each cycle shows:
- **🤔 Reasoning**: AI's analysis of current state and decision-making
- **⚡ Acting**: AI's specific action decisions and parameters
- **👁️ Observation**: AI's analysis of action results
- **🔬 Self-Reflection**: AI's evaluation of subtask completion

### 4. Progress Tracking
- Real-time status updates for each phase
- Success/failure indicators for each subtask
- Working memory updates and learning

## Testing Tips

1. **Set ANTHROPIC_API_KEY**: Ensure your API key is configured
2. **Enable Debug Logging**: For even more verbose output, set logging level to DEBUG
3. **Try Different Goals**: Test with various complexity levels
4. **Monitor Performance**: Watch execution times and token usage

## Example Test Session

Here's a complete test session you can run:

```bash
# Start the application
java -jar target/misoto-0.0.1-SNAPSHOT.jar chat

# Enter agent mode
/agent

# Start the agent if not running
start

# Create a plan (this will show all LLM responses)
plan create a Python script that processes CSV data and generates reports

# Check plan status
plans

# View completed tasks
tasks COMPLETED

# Exit agent mode
exit

# Exit chat
/exit
```

## Verification Points

When testing, verify you see:

✅ **Phase 1**: Task decomposition with AI breakdown
✅ **Phase 2**: Strategy generation with AI planning  
✅ **Phase 3**: ReAct cycles with full reasoning chains
✅ **Working Memory**: Updates showing learned information
✅ **Final Results**: Completed tasks and files created

## Troubleshooting

If you don't see verbose LLM responses:
1. Check that agent service is running (`start` command)
2. Verify ANTHROPIC_API_KEY is set correctly
3. Ensure logging level allows INFO messages
4. Try with a simpler goal first

## Performance Notes

- Each ReAct cycle involves multiple AI calls
- Complex plans may take several minutes to complete
- Token usage can be significant for detailed planning
- All AI interactions are logged for transparency

This comprehensive logging system provides full visibility into the AI's reasoning process during plan creation and execution.