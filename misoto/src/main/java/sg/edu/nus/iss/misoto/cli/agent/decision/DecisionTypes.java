package sg.edu.nus.iss.misoto.cli.agent.decision;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Decision-related data structures
 */
public class DecisionTypes {
    
    @Data
    @Builder
    public static class DecisionRequest {
        private String context;
        private String question;
        private List<String> options;
        private Instant timestamp;
        private Map<String, Object> agentState;
    }
    
    @Data
    @Builder
    public static class DecisionHistory {
        private DecisionRequest request;
        private String decision;
        private Instant timestamp;
        private double confidence;
    }
    
    @Data
    @Builder
    public static class AgentStrategy {
        private StrategyType type;
        private String reasoning;
        private Priority priority;
        private Instant timestamp;
        
        public enum StrategyType {
            AGGRESSIVE,
            CONSERVATIVE,
            BALANCED,
            REACTIVE,
            PROACTIVE,
            EXPLORATORY,
            MAINTENANCE
        }
        
        public enum Priority {
            HIGH,
            MEDIUM,
            LOW
        }
    }
      @Data
    @Builder
    public static class ActionDecision {
        private boolean shouldProceed;
        private String reasoning;
        private RiskLevel riskLevel;
        private Instant timestamp;
        
        public boolean shouldProceed() {
            return shouldProceed;
        }
        
        public enum RiskLevel {
            LOW,
            MEDIUM,
            HIGH
        }
    }
      @Data
    @Builder
    public static class ErrorHandlingDecision {
        private ErrorAction action;
        private String reason;
        private int retryDelayMs;
        private Instant timestamp;
        
        public boolean shouldRetry() {
            return action == ErrorAction.RETRY;
        }
        
        public boolean shouldStop() {
            return action == ErrorAction.STOP || action == ErrorAction.ABORT;
        }
        
        public enum ErrorAction {
            RETRY,
            SKIP,
            STOP,
            ABORT,
            ESCALATE,
            MODIFY
        }
    }
}
