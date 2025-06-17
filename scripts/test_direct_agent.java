import java.util.Scanner;

// Simple test to verify agent is working when started
public class AgentTest {
    public static void main(String[] args) {
        System.out.println("Starting agent test...");
        
        // Test task: create a simple search script
        String[] testCommands = {
            "java -jar target/misoto-0.0.1-SNAPSHOT.jar",
            // We'll test if we can get into agent mode manually
        };
        
        System.out.println("Please run the following commands manually:");
        System.out.println("1. java -jar target/misoto-0.0.1-SNAPSHOT.jar chat");
        System.out.println("2. Type: /agent");
        System.out.println("3. Type: start");
        System.out.println("4. Type: task create a python script that finds element 5 in list [1,3,5,7,9]");
        System.out.println("5. Type: exit");
        System.out.println("6. Type: /exit");
        System.out.println("\nThis should show the task execution output with the actual script result.");
    }
}