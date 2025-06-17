package sg.edu.nus.iss.misoto.cli.commands;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.misoto.cli.commands.impl.*;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * Service for registering all available CLI commands
 */
@Service
@Slf4j
public class CommandRegistrationService {
    
    @Autowired
    private CommandRegistry commandRegistry;
    
    @Autowired
    private AskCommand askCommand;
    
    @Autowired
    private LoginCommand loginCommand;
    
    @Autowired
    private LogoutCommand logoutCommand;
      @Autowired
    private ExplainCommand explainCommand;
      @Autowired
    private McpCommand mcpCommand;
      @Autowired
    private InfoCommand infoCommand;
    
    @Autowired
    private ChatCommand chatCommand;
    
    @Autowired
    private ProviderCommand providerCommand;
      /**
     * Register all commands after bean initialization
     */    @PostConstruct
    public void registerAllCommands() {        List<Command> commands = List.of(
            askCommand,
            loginCommand,
            logoutCommand,
            explainCommand,
            mcpCommand,
            infoCommand,
            chatCommand,
            providerCommand
        );
        
        for (Command command : commands) {
            commandRegistry.register(command);
        }
        
        log.info("Registered {} CLI commands", commands.size());
    }
}
