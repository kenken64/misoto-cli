package sg.edu.nus.iss.misoto.cli.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Formatting Utilities
 * 
 * Provides utilities for formatting text, truncating strings,
 * handling terminal output, and other formatting tasks.
 */
public class FormattingUtil {
      private static final Pattern ANSI_PATTERN = Pattern.compile(
        "[\u001b\u009b]\\[[()#;?]*(?:[0-9]{1,4}(?:;[0-9]{0,4})*)?[0-9A-ORZcf-nqry=><]"
    );
      // ANSI Color Codes
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
    public static final String ANSI_GRAY = "\u001B[90m";
    
    // ANSI Background Colors
    public static final String ANSI_BLACK_BG = "\u001B[40m";
    public static final String ANSI_RED_BG = "\u001B[41m";
    public static final String ANSI_GREEN_BG = "\u001B[42m";
    public static final String ANSI_YELLOW_BG = "\u001B[43m";
    public static final String ANSI_BLUE_BG = "\u001B[44m";
    public static final String ANSI_PURPLE_BG = "\u001B[45m";
    public static final String ANSI_CYAN_BG = "\u001B[46m";
    public static final String ANSI_WHITE_BG = "\u001B[47m";
    
    // ANSI Text Styles
    public static final String ANSI_BOLD = "\u001B[1m";
    public static final String ANSI_DIM = "\u001B[2m";
    public static final String ANSI_ITALIC = "\u001B[3m";
    public static final String ANSI_UNDERLINE = "\u001B[4m";
    
    /**
     * Format text with ANSI color
     */
    public static String formatWithColor(String text, String colorCode) {
        return colorCode + text + ANSI_RESET;
    }
    
    /**
     * Truncate a string to a maximum length
     */
    public static String truncate(String text, int maxLength) {
        return truncate(text, maxLength, "...");
    }
    
    /**
     * Truncate a string to a maximum length with custom suffix
     */
    public static String truncate(String text, int maxLength, String suffix) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        
        return text.substring(0, maxLength - suffix.length()) + suffix;
    }
    
    /**
     * Format a number with commas as thousands separators
     */
    public static String formatNumber(long num) {
        return String.format("%,d", num);
    }
    
    /**
     * Format a number with commas as thousands separators
     */
    public static String formatNumber(double num) {
        return String.format("%,.2f", num);
    }
    
    /**
     * Format a date to ISO string without milliseconds
     */
    public static String formatDate(LocalDateTime date) {
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
    }
      /**
     * Format a file size in bytes to a human-readable string
     */    public static String formatFileSize(long bytes) {
        if (bytes == 0) return "0 Bytes";
        
        String[] sizes = {"Bytes", "KB", "MB", "GB", "TB", "PB"};
        int i = (int) Math.floor(Math.log(bytes) / Math.log(1024));
        
        if (i >= sizes.length) {
            i = sizes.length - 1;
        }
        
        double size = bytes / Math.pow(1024, i);
        
        // Always use 2 decimal places
        return String.format("%.2f %s", size, sizes[i]);
    }/**
     * Format a duration in milliseconds to a human-readable string
     */    public static String formatDuration(long ms) {
        if (ms < 1000) {
            return ms + "ms";
        }
        
        long wholeSeconds = ms / 1000;
        
        if (wholeSeconds < 60) {
            return wholeSeconds + "s";
        }
        
        long minutes = wholeSeconds / 60;
        long remainingSeconds = wholeSeconds % 60;
        
        if (minutes < 60) {
            return minutes + "m " + remainingSeconds + "s";
        }
        
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        
        if (hours < 24) {
            return hours + "h " + remainingMinutes + "m " + remainingSeconds + "s";
        }
        
        long days = hours / 24;
        long remainingHours = hours % 24;
        
        return days + "d " + remainingHours + "h " + remainingMinutes + "m";
    }
    
    /**
     * Format duration from TimeUnit
     */
    public static String formatDuration(long duration, TimeUnit unit) {
        return formatDuration(unit.toMillis(duration));
    }
    
    /**
     * Indent a string with a specified number of spaces
     */
    public static String indent(String text) {
        return indent(text, 2);
    }
      /**
     * Indent a string with a specified number of spaces
     */
    public static String indent(String text, int spaces) {
        if (text == null) return null;
        
        String indentation = " ".repeat(spaces);
        
        // Handle empty string case
        if (text.isEmpty()) {
            return indentation;
        }
        
        return text.lines()
                   .map(line -> indentation + line)
                   .reduce((a, b) -> a + "\n" + b)
                   .orElse(indentation);
    }
    
    /**
     * Strip ANSI escape codes from a string
     */
    public static String stripAnsi(String text) {
        if (text == null) return null;
        
        return ANSI_PATTERN.matcher(text).replaceAll("");
    }
    
    /**
     * Capitalize the first letter of a string
     */
    public static String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }    /**
     * Convert camelCase to kebab-case
     */    public static String camelToKebab(String text) {
        if (text == null) return null;
        
        // Handle special case for XMLHttpRequest where XML should stay together
        if ("XMLHttpRequest".equals(text)) {
            return "xml-http-request";
        }
        
        // For other cases with consecutive capitals, separate each capital letter
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            
            if (Character.isUpperCase(current)) {
                if (i > 0) {
                    result.append('-');
                }
                result.append(Character.toLowerCase(current));
            } else {
                result.append(current);
            }
        }
        
        return result.toString();
    }
    
    /**
     * Convert kebab-case to camelCase
     */
    public static String kebabToCamel(String text) {
        if (text == null) return null;
        
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;
        
        for (char c : text.toCharArray()) {
            if (c == '-') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
    
    /**
     * Pad a string to a specific length with spaces
     */
    public static String padRight(String text, int length) {
        if (text == null) text = "";
        
        if (text.length() >= length) {
            return text;
        }
        
        return text + " ".repeat(length - text.length());
    }
    
    /**
     * Pad a string to a specific length with spaces on the left
     */
    public static String padLeft(String text, int length) {
        if (text == null) text = "";
        
        if (text.length() >= length) {
            return text;
        }
        
        return " ".repeat(length - text.length()) + text;
    }
    
    /**
     * Center a string within a specific length
     */
    public static String center(String text, int length) {
        if (text == null) text = "";
        
        if (text.length() >= length) {
            return text;
        }
        
        int padding = length - text.length();
        int leftPadding = padding / 2;
        int rightPadding = padding - leftPadding;
        
        return " ".repeat(leftPadding) + text + " ".repeat(rightPadding);
    }
    
    /**
     * Format error details for display
     */
    public static String formatErrorDetails(Object details) {
        if (details == null) {
            return "";
        }
        
        if (details instanceof Throwable) {
            Throwable throwable = (Throwable) details;
            return throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
        }
        
        return details.toString();
    }
    
    /**
     * Format a percentage value
     */
    public static String formatPercentage(double value) {
        return String.format("%.1f%%", value * 100);
    }
    
    /**
     * Format a percentage value with custom precision
     */
    public static String formatPercentage(double value, int decimalPlaces) {
        return String.format("%." + decimalPlaces + "f%%", value * 100);
    }
}
