#!/bin/bash
# Shell script to start Misoto chat with agent mode enabled

echo "🤖 Starting Misoto Chat with Agent Mode Enabled..."

# Set environment variable to enable agent mode
export MISOTO_AGENT_MODE="true"

# Change to the root directory (one level up from scripts)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$ROOT_DIR"

# Check if the JAR file exists
JAR_FILE="target/misoto-0.0.1-SNAPSHOT.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ JAR file not found: $JAR_FILE"
    echo "   Please build the project first with: mvn clean package -DskipTests"
    exit 1
fi

echo "✅ Agent mode enabled (MISOTO_AGENT_MODE=true)"
echo "🚀 Starting interactive chat..."
echo ""

# Start the application
java -jar "$JAR_FILE" chat
