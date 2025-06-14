package sg.edu.nus.iss.misoto.cli.terminal;

import sg.edu.nus.iss.misoto.cli.config.TerminalTheme;

/**
 * Terminal configuration settings
 */
public class TerminalConfig {
    
    private TerminalTheme theme = TerminalTheme.SYSTEM;
    private boolean useColors = true;
    private boolean showProgressIndicators = true;
    private boolean codeHighlighting = true;
    private Integer maxHeight;
    private Integer maxWidth;
    
    // Constructors
    public TerminalConfig() {}
    
    public TerminalConfig(TerminalTheme theme, boolean useColors, boolean showProgressIndicators, 
                         boolean codeHighlighting, Integer maxHeight, Integer maxWidth) {
        this.theme = theme;
        this.useColors = useColors;
        this.showProgressIndicators = showProgressIndicators;
        this.codeHighlighting = codeHighlighting;
        this.maxHeight = maxHeight;
        this.maxWidth = maxWidth;
    }
    
    // Getters and setters
    public TerminalTheme getTheme() {
        return theme;
    }
    
    public void setTheme(TerminalTheme theme) {
        this.theme = theme;
    }
    
    public boolean isUseColors() {
        return useColors;
    }
    
    public void setUseColors(boolean useColors) {
        this.useColors = useColors;
    }
    
    public boolean isShowProgressIndicators() {
        return showProgressIndicators;
    }
    
    public void setShowProgressIndicators(boolean showProgressIndicators) {
        this.showProgressIndicators = showProgressIndicators;
    }
    
    public boolean isCodeHighlighting() {
        return codeHighlighting;
    }
    
    public void setCodeHighlighting(boolean codeHighlighting) {
        this.codeHighlighting = codeHighlighting;
    }
    
    public Integer getMaxHeight() {
        return maxHeight;
    }
    
    public void setMaxHeight(Integer maxHeight) {
        this.maxHeight = maxHeight;
    }
    
    public Integer getMaxWidth() {
        return maxWidth;
    }
    
    public void setMaxWidth(Integer maxWidth) {
        this.maxWidth = maxWidth;
    }
    
    @Override
    public String toString() {
        return "TerminalConfig{" +
                "theme=" + theme +
                ", useColors=" + useColors +
                ", showProgressIndicators=" + showProgressIndicators +
                ", codeHighlighting=" + codeHighlighting +
                ", maxHeight=" + maxHeight +
                ", maxWidth=" + maxWidth +
                '}';
    }
}
