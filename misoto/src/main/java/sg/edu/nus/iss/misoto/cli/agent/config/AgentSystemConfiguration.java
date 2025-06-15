package sg.edu.nus.iss.misoto.cli.agent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Duration;

/**
 * Spring configuration for the agent mode system.
 * This configuration class enables and configures the agent system components.
 */
@Configuration
@EnableAsync
@EnableScheduling
@ConditionalOnProperty(
    name = "misoto.agent.mode.enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class AgentSystemConfiguration {    /**
     * Agent configuration bean
     */
    @Bean
    public AgentConfiguration agentConfiguration(
            @Value("${misoto.agent.max-concurrent-tasks:3}") int maxConcurrentTasks,
            @Value("${misoto.agent.execution-interval-ms:5000}") long executionIntervalMs,
            @Value("${misoto.agent.shutdown.timeout-seconds:5}") long shutdownTimeoutSeconds,
            @Value("${misoto.agent.monitoring.shutdown.timeout-seconds:3}") long monitoringShutdownTimeoutSeconds) {
        return AgentConfiguration.builder()
            .enabled(true)
            .maxConcurrentTasks(maxConcurrentTasks)
            .monitoringInterval(Duration.ofMillis(executionIntervalMs))
            .shutdownTimeout(Duration.ofSeconds(shutdownTimeoutSeconds))
            .monitoringShutdownTimeout(Duration.ofSeconds(monitoringShutdownTimeoutSeconds))
            .statePersistence(new AgentConfiguration.StatePersistence())
            .build();
    }
}
