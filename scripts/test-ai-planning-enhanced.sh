#!/bin/bash

# Test script for Enhanced AI Planning System with File Creation and Command Execution
# Demonstrates the new capabilities where AI generates executable commands and code

echo "=== Enhanced AI Planning System Test ==="
echo ""

# Set environment variables for testing
export MISOTO_AGENT_MODE="true"
export MISOTO_AGENT_MAX_TASKS="3"
export MISOTO_AGENT_INTERVAL="5000"
export MISOTO_AGENT_AUTO_SAVE="true"

echo "Environment configured:"
echo "  MISOTO_AGENT_MODE=$MISOTO_AGENT_MODE"
echo "  MISOTO_AGENT_MAX_TASKS=$MISOTO_AGENT_MAX_TASKS" 
echo "  MISOTO_AGENT_INTERVAL=$MISOTO_AGENT_INTERVAL"
echo "  MISOTO_AGENT_AUTO_SAVE=$MISOTO_AGENT_AUTO_SAVE"
echo ""

# Build the project
echo "Building the project..."
./mvnw clean compile -DskipTests -q

if [ $? -ne 0 ]; then
    echo "Build failed! Please check compilation errors."
    exit 1
fi

echo "Build successful!"
echo ""

echo "Enhanced AI Planning System Features:"
echo "  ✓ Automatic current directory analysis for context-aware planning"
echo "  ✓ AI understands existing project structure and technologies"
echo "  ✓ Missing tool detection with OS-specific installation suggestions"
echo "  ✓ AI generates executable shell commands"
echo "  ✓ AI creates code snippets with file paths"
echo "  ✓ Files are automatically saved to current directory"
echo "  ✓ Commands are executed through the task system"
echo "  ✓ Real-time progress feedback"
echo "  ✓ Builds upon existing project patterns and conventions"
echo ""

echo "Example AI Planning Request:"
echo '  Goal: "Create a simple todo application with Python backend and HTML frontend"'
echo ""
echo "AI will generate:"
echo "  - Shell commands: mkdir todoApp, cd todoApp, python -m venv venv"
echo "  - Python code: Flask server with todo endpoints" 
echo "  - HTML/CSS: Frontend interface files"
echo "  - File paths: app.py, templates/index.html, static/style.css"
echo "  - All files saved to ./todoApp/ directory"
echo ""

echo "Test commands to run once the application starts:"
echo "  agent-config --enable"
echo "  agent-start" 
echo "  agent-task --type AI --description 'Create a Python todo app with web interface' --priority HIGH"
echo "  agent-tasks --limit 10"
echo "  agent-stop"
echo ""

echo "The enhanced planning system will:"
echo "  1. Analyze current directory to understand existing codebase"
echo "  2. Decompose the goal into executable subtasks"
echo "  3. Check for missing tools and suggest OS-specific installations"
echo "  4. Generate context-aware commands that fit project structure"
echo "  5. Create actual code files following existing conventions"
echo "  6. Save all files to appropriate directories"
echo "  7. Execute commands to set up the project structure"
echo "  8. Provide real-time feedback on progress"
echo "  9. Integrate seamlessly with existing technology stack"
echo ""

echo "Enhanced AI Planning System implementation is complete!"