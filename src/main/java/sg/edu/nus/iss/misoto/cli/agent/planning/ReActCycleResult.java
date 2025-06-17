package sg.edu.nus.iss.misoto.cli.agent.planning;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Result of a single ReAct (Reasoning + Acting) cycle
 */
@Data
@Builder
public class ReActCycleResult {
    private String reasoning;
    private String action;
    private String observation;
    private boolean success;
    private boolean shouldReplan;
    private Map<String, Object> memoryUpdates;
    private String nextStepSuggestion;
}