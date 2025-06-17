#!/bin/bash

# Quick test script for ReAct Planning System with verbose LLM responses
# Run this to see the enhanced logging in action

echo "=== ReAct Planning System Test ==="
echo "Testing verbose LLM response logging..."
echo ""

# Check if ANTHROPIC_API_KEY is set
if [ -z "$ANTHROPIC_API_KEY" ]; then
    echo "❌ ANTHROPIC_API_KEY not set. Please set it first:"
    echo "export ANTHROPIC_API_KEY=sk-ant-api03-..."
    exit 1
fi

echo "✅ ANTHROPIC_API_KEY is configured"
echo ""

# Test plan creation with verbose output
echo "Starting test plan creation..."
echo "This will demonstrate:"
echo "  🤖 Task decomposition with AI responses"
echo "  🧠 Strategy generation with AI planning"  
echo "  🔄 ReAct cycles (Reasoning → Acting → Observation)"
echo "  📊 Self-reflection and success evaluation"
echo ""

# Create test input file
cat > plan_test_input.txt << 'EOF'
/agent
start
plan create a simple Python calculator with basic arithmetic operations
exit
/exit
EOF

echo "Executing plan test..."
echo "Watch for detailed AI model responses in the output!"
echo ""

# Run the test
java -jar target/misoto-0.0.1-SNAPSHOT.jar chat < plan_test_input.txt

echo ""
echo "=== Test Complete ==="
echo "You should have seen detailed logging including:"
echo "  • AI task decomposition responses"
echo "  • AI strategy generation"
echo "  • ReAct reasoning chains"
echo "  • Action decisions and observations"
echo "  • Success evaluations"

# Cleanup
rm -f plan_test_input.txt

echo ""
echo "For more comprehensive testing, see:"
echo "  📖 test_planning_comprehensive.md"
echo "  📋 PLANNING_SYSTEM_README.md"