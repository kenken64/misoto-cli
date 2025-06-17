// Simple test to demonstrate planning system
// Run with: java -jar target/misoto-0.0.1-SNAPSHOT.jar

public class PlanningTest {
    public static void main(String[] args) {
        System.out.println("=== Testing Planning System ===");
        System.out.println();
        System.out.println("To test the ReAct planning system:");
        System.out.println();
        System.out.println("1. Run: java -jar target/misoto-0.0.1-SNAPSHOT.jar chat");
        System.out.println("2. Type: /agent");
        System.out.println("3. Type: start");
        System.out.println("4. Type: plan create a simple Python calculator app");
        System.out.println("5. Watch the LLM responses for:");
        System.out.println("   - Task decomposition");
        System.out.println("   - Planning strategy");
        System.out.println("   - ReAct cycles (Reasoning → Acting → Observation)");
        System.out.println("6. Type: plans (to see active plans)");
        System.out.println("7. Type: exit");
        System.out.println("8. Type: /exit");
        System.out.println();
        System.out.println("Expected output will show:");
        System.out.println("- AI breaking down the goal into subtasks");
        System.out.println("- Detailed reasoning for each step");
        System.out.println("- Actual task execution and results");
        System.out.println("- Working memory updates");
    }
}