package sg.edu.nus.iss.misoto.cli.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.HashMap;

/**
 * Configuration to load .env file variables into Spring Environment
 * This ensures dotenv variables are available for Spring property resolution
 */
@Configuration
@Slf4j
public class DotenvConfiguration implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        
        // Initialize dotenv loader
        DotenvLoader.initialize();
        
        // Create a map of dotenv properties
        Map<String, Object> dotenvProps = new HashMap<>();
        
        // Add important environment variables if they exist
        addIfPresent(dotenvProps, "ANTHROPIC_API_KEY");
        addIfPresent(dotenvProps, "CLAUDE_API_URL");
        addIfPresent(dotenvProps, "CLAUDE_LOG_LEVEL");
        addIfPresent(dotenvProps, "CLAUDE_TELEMETRY");
        
        if (!dotenvProps.isEmpty()) {
            // Add dotenv properties to Spring environment with high precedence
            environment.getPropertySources().addFirst(
                new MapPropertySource("dotenv", dotenvProps)
            );
            
            log.debug("Added {} dotenv properties to Spring environment", dotenvProps.size());
        }
    }
    
    private void addIfPresent(Map<String, Object> props, String key) {
        String value = DotenvLoader.getEnv(key);
        if (value != null) {
            props.put(key, value);
            log.debug("Added dotenv property: {}", key);
        }
    }
}
