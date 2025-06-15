package sg.edu.nus.iss.misoto.cli.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for FormattingUtil
 */
@DisplayName("FormattingUtil Tests")
class FormattingUtilTest {

    @Test
    @DisplayName("formatWithColor should apply ANSI color codes")
    void testFormatWithColor() {
        String text = "Hello World";
        String expected = FormattingUtil.ANSI_RED + text + FormattingUtil.ANSI_RESET;
        
        String result = FormattingUtil.formatWithColor(text, FormattingUtil.ANSI_RED);
        
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("formatWithColor should handle null text")
    void testFormatWithColorNullText() {
        String result = FormattingUtil.formatWithColor(null, FormattingUtil.ANSI_BLUE);
        
        assertEquals(FormattingUtil.ANSI_BLUE + "null" + FormattingUtil.ANSI_RESET, result);
    }

    @Test
    @DisplayName("truncate should truncate long strings")
    void testTruncate() {
        String text = "This is a very long string that needs to be truncated";
        int maxLength = 20;
        
        String result = FormattingUtil.truncate(text, maxLength);
        
        assertEquals(20, result.length());
        assertTrue(result.endsWith("..."));
        assertEquals("This is a very lo...", result);
    }

    @Test
    @DisplayName("truncate should not truncate short strings")
    void testTruncateShortString() {
        String text = "Short";
        int maxLength = 20;
        
        String result = FormattingUtil.truncate(text, maxLength);
        
        assertEquals(text, result);
    }

    @Test
    @DisplayName("truncate should handle null text")
    void testTruncateNullText() {
        String result = FormattingUtil.truncate(null, 10);
        
        assertNull(result);
    }

    @Test
    @DisplayName("truncate with custom suffix")
    void testTruncateCustomSuffix() {
        String text = "This is a long string";
        String suffix = " [more]";
        int maxLength = 15;
        
        String result = FormattingUtil.truncate(text, maxLength, suffix);
        
        assertEquals(15, result.length());
        assertTrue(result.endsWith(suffix));
    }

    @Test
    @DisplayName("formatNumber should format long with commas")
    void testFormatNumberLong() {
        long number = 1234567;
        
        String result = FormattingUtil.formatNumber(number);
        
        assertEquals("1,234,567", result);
    }

    @Test
    @DisplayName("formatNumber should format double with commas")
    void testFormatNumberDouble() {
        double number = 1234567.89;
        
        String result = FormattingUtil.formatNumber(number);
        
        assertEquals("1,234,567.89", result);
    }

    @Test
    @DisplayName("formatNumber should handle zero")
    void testFormatNumberZero() {
        String result = FormattingUtil.formatNumber(0L);
        
        assertEquals("0", result);
    }

    @Test
    @DisplayName("formatDate should format LocalDateTime")
    void testFormatDate() {
        LocalDateTime date = LocalDateTime.of(2023, 12, 25, 15, 30, 45);
        
        String result = FormattingUtil.formatDate(date);
        
        assertEquals("2023-12-25T15:30:45Z", result);
    }

    @Test
    @DisplayName("formatFileSize should format bytes correctly")
    void testFormatFileSize() {
        assertEquals("0 Bytes", FormattingUtil.formatFileSize(0));
        assertEquals("1.00 Bytes", FormattingUtil.formatFileSize(1));
        assertEquals("1.50 KB", FormattingUtil.formatFileSize(1536));
        assertEquals("1.00 MB", FormattingUtil.formatFileSize(1024 * 1024));
        assertEquals("1.50 GB", FormattingUtil.formatFileSize(1024L * 1024 * 1024 + 512 * 1024 * 1024));
    }

    @Test
    @DisplayName("formatFileSize should handle large numbers")
    void testFormatFileSizeLarge() {
        long terabyte = 1024L * 1024 * 1024 * 1024;
        
        String result = FormattingUtil.formatFileSize(terabyte);
        
        assertEquals("1.00 TB", result);
    }

    @Test
    @DisplayName("formatDuration should format milliseconds")
    void testFormatDurationMilliseconds() {
        assertEquals("500ms", FormattingUtil.formatDuration(500));
        assertEquals("999ms", FormattingUtil.formatDuration(999));
    }

    @Test
    @DisplayName("formatDuration should format seconds")
    void testFormatDurationSeconds() {
        assertEquals("1s", FormattingUtil.formatDuration(1000));
        assertEquals("30s", FormattingUtil.formatDuration(30000));
        assertEquals("59s", FormattingUtil.formatDuration(59000));
    }

    @Test
    @DisplayName("formatDuration should format minutes and seconds")
    void testFormatDurationMinutes() {
        assertEquals("1m 0s", FormattingUtil.formatDuration(60000));
        assertEquals("2m 30s", FormattingUtil.formatDuration(150000));
        assertEquals("59m 59s", FormattingUtil.formatDuration(3599000));
    }

    @Test
    @DisplayName("formatDuration should format hours, minutes and seconds")
    void testFormatDurationHours() {
        assertEquals("1h 0m 0s", FormattingUtil.formatDuration(3600000));
        assertEquals("2h 30m 45s", FormattingUtil.formatDuration(9045000));
    }

    @Test
    @DisplayName("formatDuration should format days, hours and minutes")
    void testFormatDurationDays() {
        long oneDayMs = 24 * 60 * 60 * 1000;
        
        String result = FormattingUtil.formatDuration(oneDayMs + 3600000 + 1800000); // 1d 1h 30m
        
        assertEquals("1d 1h 30m", result);
    }

    @Test
    @DisplayName("formatDuration with TimeUnit should work")
    void testFormatDurationTimeUnit() {
        String result = FormattingUtil.formatDuration(5, TimeUnit.MINUTES);
        
        assertEquals("5m 0s", result);
    }

    @Test
    @DisplayName("indent should add 2 spaces by default")
    void testIndentDefault() {
        String text = "Line 1\nLine 2\nLine 3";
        
        String result = FormattingUtil.indent(text);
        
        assertEquals("  Line 1\n  Line 2\n  Line 3", result);
    }

    @Test
    @DisplayName("indent should add custom number of spaces")
    void testIndentCustom() {
        String text = "Line 1\nLine 2";
        
        String result = FormattingUtil.indent(text, 4);
        
        assertEquals("    Line 1\n    Line 2", result);
    }

    @Test
    @DisplayName("indent should handle null text")
    void testIndentNullText() {
        String result = FormattingUtil.indent(null, 2);
        
        assertNull(result);
    }

    @Test
    @DisplayName("indent should handle empty text")
    void testIndentEmptyText() {
        String result = FormattingUtil.indent("", 2);
        
        assertEquals("  ", result);
    }

    @Test
    @DisplayName("stripAnsi should remove ANSI codes")
    void testStripAnsi() {
        String textWithAnsi = FormattingUtil.ANSI_RED + "Hello" + FormattingUtil.ANSI_BOLD + " World" + FormattingUtil.ANSI_RESET;
        
        String result = FormattingUtil.stripAnsi(textWithAnsi);
        
        assertEquals("Hello World", result);
    }

    @Test
    @DisplayName("stripAnsi should handle text without ANSI codes")
    void testStripAnsiPlainText() {
        String plainText = "Hello World";
        
        String result = FormattingUtil.stripAnsi(plainText);
        
        assertEquals(plainText, result);
    }

    @Test
    @DisplayName("stripAnsi should handle null text")
    void testStripAnsiNullText() {
        String result = FormattingUtil.stripAnsi(null);
        
        assertNull(result);
    }

    @Test
    @DisplayName("capitalize should capitalize first letter")
    void testCapitalize() {
        assertEquals("Hello", FormattingUtil.capitalize("hello"));
        assertEquals("World", FormattingUtil.capitalize("WORLD"));
        assertEquals("Test", FormattingUtil.capitalize("tEST"));
    }

    @Test
    @DisplayName("capitalize should handle null and empty strings")
    void testCapitalizeEdgeCases() {
        assertNull(FormattingUtil.capitalize(null));
        assertEquals("", FormattingUtil.capitalize(""));
        assertEquals("A", FormattingUtil.capitalize("a"));
    }

    @Test
    @DisplayName("camelToKebab should convert camelCase to kebab-case")
    void testCamelToKebab() {
        assertEquals("hello-world", FormattingUtil.camelToKebab("helloWorld"));
        assertEquals("camel-case-string", FormattingUtil.camelToKebab("camelCaseString"));
        assertEquals("simple", FormattingUtil.camelToKebab("simple"));
        assertEquals("xml-http-request", FormattingUtil.camelToKebab("XMLHttpRequest"));
    }

    @Test
    @DisplayName("camelToKebab should handle null")
    void testCamelToKebabNull() {
        assertNull(FormattingUtil.camelToKebab(null));
    }

    @Test
    @DisplayName("kebabToCamel should convert kebab-case to camelCase")
    void testKebabToCamel() {
        assertEquals("helloWorld", FormattingUtil.kebabToCamel("hello-world"));
        assertEquals("camelCaseString", FormattingUtil.kebabToCamel("camel-case-string"));
        assertEquals("simple", FormattingUtil.kebabToCamel("simple"));
    }

    @Test
    @DisplayName("kebabToCamel should handle null")
    void testKebabToCamelNull() {
        assertNull(FormattingUtil.kebabToCamel(null));
    }

    @Test
    @DisplayName("padRight should pad string with spaces")
    void testPadRight() {
        assertEquals("Hello     ", FormattingUtil.padRight("Hello", 10));
        assertEquals("Test", FormattingUtil.padRight("Test", 3)); // No padding needed
        assertEquals("", FormattingUtil.padRight("", 0));
    }

    @Test
    @DisplayName("padRight should handle null text")
    void testPadRightNull() {
        assertEquals("     ", FormattingUtil.padRight(null, 5));
    }

    @Test
    @DisplayName("padLeft should pad string with spaces on left")
    void testPadLeft() {
        assertEquals("     Hello", FormattingUtil.padLeft("Hello", 10));
        assertEquals("Test", FormattingUtil.padLeft("Test", 3)); // No padding needed
        assertEquals("", FormattingUtil.padLeft("", 0));
    }

    @Test
    @DisplayName("padLeft should handle null text")
    void testPadLeftNull() {
        assertEquals("     ", FormattingUtil.padLeft(null, 5));
    }

    @Test
    @DisplayName("center should center string")
    void testCenter() {
        assertEquals("  Hello   ", FormattingUtil.center("Hello", 10));
        assertEquals(" Hi  ", FormattingUtil.center("Hi", 5));
        assertEquals("Test", FormattingUtil.center("Test", 3)); // No padding needed
    }

    @Test
    @DisplayName("center should handle null text")
    void testCenterNull() {
        assertEquals("     ", FormattingUtil.center(null, 5));
    }

    @Test
    @DisplayName("formatErrorDetails should format Throwable")
    void testFormatErrorDetailsThrowable() {
        RuntimeException ex = new RuntimeException("Test error");
        
        String result = FormattingUtil.formatErrorDetails(ex);
        
        assertEquals("RuntimeException: Test error", result);
    }

    @Test
    @DisplayName("formatErrorDetails should format regular objects")
    void testFormatErrorDetailsObject() {
        String error = "Simple error message";
        
        String result = FormattingUtil.formatErrorDetails(error);
        
        assertEquals("Simple error message", result);
    }

    @Test
    @DisplayName("formatErrorDetails should handle null")
    void testFormatErrorDetailsNull() {
        String result = FormattingUtil.formatErrorDetails(null);
        
        assertEquals("", result);
    }

    @Test
    @DisplayName("formatPercentage should format percentage with default precision")
    void testFormatPercentage() {
        assertEquals("50.0%", FormattingUtil.formatPercentage(0.5));
        assertEquals("75.5%", FormattingUtil.formatPercentage(0.755));
        assertEquals("100.0%", FormattingUtil.formatPercentage(1.0));
        assertEquals("0.0%", FormattingUtil.formatPercentage(0.0));
    }

    @Test
    @DisplayName("formatPercentage should format with custom precision")
    void testFormatPercentageCustomPrecision() {
        assertEquals("50%", FormattingUtil.formatPercentage(0.5, 0));
        assertEquals("75.55%", FormattingUtil.formatPercentage(0.7555, 2));
        assertEquals("33.333%", FormattingUtil.formatPercentage(0.33333, 3));
    }

    @Test
    @DisplayName("ANSI constants should be defined correctly")
    void testAnsiConstants() {
        // Test that ANSI constants are not null or empty
        assertNotNull(FormattingUtil.ANSI_RESET);
        assertNotNull(FormattingUtil.ANSI_RED);
        assertNotNull(FormattingUtil.ANSI_GREEN);
        assertNotNull(FormattingUtil.ANSI_BLUE);
        assertNotNull(FormattingUtil.ANSI_BOLD);
        assertNotNull(FormattingUtil.ANSI_UNDERLINE);

        assertFalse(FormattingUtil.ANSI_RESET.isEmpty());
        assertFalse(FormattingUtil.ANSI_RED.isEmpty());
        assertFalse(FormattingUtil.ANSI_BOLD.isEmpty());
    }

    @Test
    @DisplayName("stripAnsi should remove complex ANSI sequences")
    void testStripAnsiComplex() {
        String complex = "\u001B[31;1mRed Bold\u001B[0m \u001B[32mGreen\u001B[0m";
        
        String result = FormattingUtil.stripAnsi(complex);
        
        assertEquals("Red Bold Green", result);
    }

    @Test
    @DisplayName("formatFileSize should handle edge cases")
    void testFormatFileSizeEdgeCases() {
        // Test very large numbers
        long petabyte = 1024L * 1024 * 1024 * 1024 * 1024;
        String result = FormattingUtil.formatFileSize(petabyte);
        assertEquals("1.00 PB", result);
        
        // Test numbers larger than PB
        long larger = petabyte * 10;
        String largerResult = FormattingUtil.formatFileSize(larger);
        assertEquals("10.00 PB", largerResult);
    }

    @Test
    @DisplayName("formatDuration should handle zero")
    void testFormatDurationZero() {
        assertEquals("0ms", FormattingUtil.formatDuration(0));
    }

    @Test
    @DisplayName("all padding methods should handle exact length")
    void testPaddingExactLength() {
        String text = "Hello";
        
        assertEquals("Hello", FormattingUtil.padRight(text, 5));
        assertEquals("Hello", FormattingUtil.padLeft(text, 5));
        assertEquals("Hello", FormattingUtil.center(text, 5));
    }

    @Test
    @DisplayName("kebabToCamel should handle consecutive dashes")
    void testKebabToCamelConsecutiveDashes() {
        assertEquals("helloWorld", FormattingUtil.kebabToCamel("hello--world"));
        assertEquals("testCase", FormattingUtil.kebabToCamel("test---case"));
    }    @Test
    @DisplayName("camelToKebab should handle consecutive capitals")
    void testCamelToKebabConsecutiveCapitals() {
        assertEquals("x-m-l-parser", FormattingUtil.camelToKebab("XMLParser"));
        assertEquals("h-t-t-p-client", FormattingUtil.camelToKebab("HTTPClient"));
    }

    @Test
    @DisplayName("indent should handle single line")
    void testIndentSingleLine() {
        String text = "Single line";
        
        String result = FormattingUtil.indent(text, 3);
        
        assertEquals("   Single line", result);
    }

    @Test
    @DisplayName("formatPercentage should handle negative values")
    void testFormatPercentageNegative() {
        assertEquals("-25.0%", FormattingUtil.formatPercentage(-0.25));
        assertEquals("-100.0%", FormattingUtil.formatPercentage(-1.0));
    }

    @Test
    @DisplayName("formatPercentage should handle values greater than 1")
    void testFormatPercentageGreaterThanOne() {
        assertEquals("150.0%", FormattingUtil.formatPercentage(1.5));
        assertEquals("200.0%", FormattingUtil.formatPercentage(2.0));
    }
}
