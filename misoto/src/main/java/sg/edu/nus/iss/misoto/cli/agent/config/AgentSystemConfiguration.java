package sg.edu.nus.iss.misoto.cli.agent.config;

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
    public AgentConfiguration agentConfiguration() {        return AgentConfiguration.builder()
            .enabled(true)
            .maxConcurrentTasks(3)
            .monitoringInterval(Duration.ofMillis(5000L))
            .statePersistence(new AgentConfiguration.StatePersistence())
            .build();
    }
}
