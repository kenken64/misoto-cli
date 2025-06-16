package sg.edu.nus.iss.misoto.cli.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Configuration to handle Reactor context propagation and prevent shutdown errors
 */
@Configuration
@ConditionalOnClass(name = "reactor.core.publisher.Mono")
public class ReactorConfiguration {

    @PostConstruct
    public void init() {
        try {
            // Disable context propagation and metrics to prevent shutdown errors
            System.setProperty("reactor.core.scheduler.enable-metrics", "false");
            System.setProperty("reactor.core.context.propagation", "false");
            System.setProperty("io.micrometer.context.propagation", "false");
            
            // Disable automatic context propagation to prevent ServiceConfigurationError
            // during shutdown when JAR files are not accessible
            Hooks.disableAutomaticContextPropagation();
        } catch (Exception e) {
            // Ignore any context propagation setup errors
            // This prevents the ServiceConfigurationError during startup/shutdown
        }
    }

    @PreDestroy
    public void cleanup() {
        try {
            // Clean shutdown of Reactor hooks
            Hooks.resetOnEachOperator();
            Hooks.resetOnErrorDropped();
            Hooks.resetOnNextDropped();
        } catch (Exception e) {
            // Ignore errors during cleanup to prevent shutdown issues
        }
    }
}
