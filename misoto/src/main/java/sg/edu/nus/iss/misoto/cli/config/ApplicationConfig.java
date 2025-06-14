package sg.edu.nus.iss.misoto.cli.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import sg.edu.nus.iss.misoto.cli.terminal.TerminalConfig;

/**
 * Application Configuration
 * 
 * Holds all configuration settings for the Claude Code CLI application.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplicationConfig {
    
    // API Configuration
    @JsonProperty("api_key")
    private String apiKey;
    
    @JsonProperty("api_base_url")
    private String apiBaseUrl = "https://api.anthropic.com";
    
    @JsonProperty("api_version")
    private String apiVersion = "2023-06-01";
    
    @JsonProperty("api_timeout")
    private Integer apiTimeout = 60000; // 60 seconds
    
    // AI Configuration
    @JsonProperty("ai_model")
    private String aiModel = "claude-3-haiku-20240307";
    
    @JsonProperty("ai_temperature")
    private Double aiTemperature = 0.5;
    
    @JsonProperty("ai_max_tokens")
    private Integer aiMaxTokens = 4096;
    
    @JsonProperty("ai_max_history_length")
    private Integer aiMaxHistoryLength = 20;
    
    // Authentication Configuration
    @JsonProperty("auth_auto_refresh")
    private Boolean authAutoRefresh = true;
    
    @JsonProperty("auth_token_refresh_threshold")
    private Integer authTokenRefreshThreshold = 300; // 5 minutes
    
    @JsonProperty("auth_max_retry_attempts")
    private Integer authMaxRetryAttempts = 3;
    
    // Terminal Configuration
    @JsonProperty("terminal_theme")
    private TerminalTheme terminalTheme = TerminalTheme.SYSTEM;
    
    @JsonProperty("terminal_use_colors")
    private Boolean terminalUseColors = true;
    
    @JsonProperty("terminal_show_progress")
    private Boolean terminalShowProgress = true;
    
    @JsonProperty("terminal_code_highlighting")
    private Boolean terminalCodeHighlighting = true;
    
    @JsonProperty("terminal_max_height")
    private Integer terminalMaxHeight;
    
    @JsonProperty("terminal_max_width")
    private Integer terminalMaxWidth;
    
    // Telemetry Configuration
    @JsonProperty("telemetry_enabled")
    private Boolean telemetryEnabled = true;
    
    @JsonProperty("telemetry_anonymize")
    private Boolean telemetryAnonymize = true;
    
    @JsonProperty("telemetry_submission_interval")
    private Integer telemetrySubmissionInterval = 1800000; // 30 minutes
    
    @JsonProperty("telemetry_max_queue_size")
    private Integer telemetryMaxQueueSize = 100;
      @JsonProperty("telemetry_auto_submit")
    private Boolean telemetryAutoSubmit = true;
    
    @JsonProperty("telemetry_endpoint")
    private String telemetryEndpoint = "https://telemetry.example.com/api/events";
    
    // File Operations Configuration
    @JsonProperty("file_ops_max_read_size")
    private Long fileOpsMaxReadSize = 10485760L; // 10MB
    
    // Execution Configuration
    @JsonProperty("execution_shell")
    private String executionShell = System.getProperty("os.name").toLowerCase().contains("windows") ? "cmd" : "bash";
    
    // Logger Configuration
    @JsonProperty("log_level")
    private LogLevel logLevel = LogLevel.INFO;
    
    @JsonProperty("log_timestamps")
    private Boolean logTimestamps = true;
    
    @JsonProperty("log_colors")
    private Boolean logColors = true;
    
    // Code Analysis Configuration
    @JsonProperty("codebase_index_depth")
    private Integer codebaseIndexDepth = 3;
    
    @JsonProperty("codebase_max_file_size")
    private Long codebaseMaxFileSize = 1048576L; // 1MB
    
    @JsonProperty("codebase_scan_timeout")
    private Integer codebaseScanTimeout = 30000; // 30 seconds
    
    // Git Configuration
    @JsonProperty("git_preferred_remote")
    private String gitPreferredRemote = "origin";
    
    @JsonProperty("git_preferred_branch")
    private String gitPreferredBranch;
    
    @JsonProperty("git_use_ssh")
    private Boolean gitUseSsh = false;
    
    @JsonProperty("git_use_gpg")
    private Boolean gitUseGpg = false;
    
    @JsonProperty("git_sign_commits")
    private Boolean gitSignCommits = false;
    
    // Editor Configuration
    @JsonProperty("editor_preferred_launcher")
    private String editorPreferredLauncher;
    
    @JsonProperty("editor_tab_width")
    private Integer editorTabWidth = 2;
    
    @JsonProperty("editor_insert_spaces")
    private Boolean editorInsertSpaces = true;
    
    @JsonProperty("editor_format_on_save")
    private Boolean editorFormatOnSave = true;
    
    // Application Information
    @JsonProperty("version")
    private String version = "1.0.0";
    
    /**
     * Get API endpoint URL
     */
    public String getApiEndpoint() {
        return apiBaseUrl + "/" + apiVersion;
    }
    
    /**
     * Check if terminal colors should be used
     */
    public boolean shouldUseColors() {
        return Boolean.TRUE.equals(terminalUseColors) && 
               (terminalTheme != TerminalTheme.LIGHT || System.getProperty("java.awt.headless", "false").equals("false"));
    }
      /**
     * Check if telemetry is enabled
     */
    public boolean isTelemetryEnabled() {
        return Boolean.TRUE.equals(telemetryEnabled);
    }
    
    /**
     * Get telemetry endpoint URL
     */
    public String getTelemetryEndpoint() {
        return telemetryEndpoint;
    }
    
    /**
     * Check if authentication auto-refresh is enabled
     */
    public boolean isAuthAutoRefreshEnabled() {
        return Boolean.TRUE.equals(authAutoRefresh);
    }
    
    /**
     * Get terminal configuration
     */
    public TerminalConfig getTerminal() {
        return new TerminalConfig(
            terminalTheme != null ? terminalTheme : TerminalTheme.SYSTEM,
            Boolean.TRUE.equals(terminalUseColors),
            Boolean.TRUE.equals(terminalShowProgress),
            Boolean.TRUE.equals(terminalCodeHighlighting),
            terminalMaxHeight,
            terminalMaxWidth
        );
    }
}
