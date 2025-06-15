package sg.edu.nus.iss.misoto.cli.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for UtilityModule
 */
@DisplayName("UtilityModule Tests")
class UtilityModuleTest {

    @Test
    @DisplayName("Formatting.truncate should truncate text correctly")
    void testFormattingTruncate() {
        assertEquals("hello", UtilityModule.Formatting.truncate("hello", 10));
        assertEquals("hello...", UtilityModule.Formatting.truncate("hello world", 8));
        assertEquals("hello world", UtilityModule.Formatting.truncate("hello world", 11));
        assertEquals("", UtilityModule.Formatting.truncate("", 5));
    }

    @Test
    @DisplayName("Formatting.truncate with custom suffix should work correctly")
    void testFormattingTruncateWithSuffix() {
        assertEquals("hello", UtilityModule.Formatting.truncate("hello", 10, "***"));
        assertEquals("hello***", UtilityModule.Formatting.truncate("hello world", 8, "***"));
        assertEquals("h***", UtilityModule.Formatting.truncate("hello world", 4, "***"));
    }    @Test
    @DisplayName("Formatting.formatFileSize should format bytes correctly")
    void testFormattingFormatFileSize() {
        assertEquals("0 Bytes", UtilityModule.Formatting.formatFileSize(0));
        assertEquals("512.00 Bytes", UtilityModule.Formatting.formatFileSize(512));
        assertEquals("1.00 KB", UtilityModule.Formatting.formatFileSize(1024));
        assertEquals("1.50 KB", UtilityModule.Formatting.formatFileSize(1536));
        assertEquals("1.00 MB", UtilityModule.Formatting.formatFileSize(1024 * 1024));
        assertEquals("2.50 GB", UtilityModule.Formatting.formatFileSize(1024L * 1024 * 1024 * 2 + 1024L * 1024 * 1024 / 2));
    }    @Test
    @DisplayName("Formatting.formatDuration should format milliseconds correctly")
    void testFormattingFormatDuration() {
        assertEquals("0ms", UtilityModule.Formatting.formatDuration(0));
        assertEquals("500ms", UtilityModule.Formatting.formatDuration(500));
        assertEquals("1s", UtilityModule.Formatting.formatDuration(1000));
        assertEquals("1s", UtilityModule.Formatting.formatDuration(1500));
        assertEquals("1m 30s", UtilityModule.Formatting.formatDuration(90000));
        assertEquals("1h 30m 0s", UtilityModule.Formatting.formatDuration(5400000));
    }

    @Test
    @DisplayName("Formatting.formatNumber should format numbers correctly")
    void testFormattingFormatNumber() {
        assertEquals("0", UtilityModule.Formatting.formatNumber(0));
        assertEquals("1", UtilityModule.Formatting.formatNumber(1));
        assertEquals("1,000", UtilityModule.Formatting.formatNumber(1000));
        assertEquals("1,234,567", UtilityModule.Formatting.formatNumber(1234567));
        assertEquals("-1,000", UtilityModule.Formatting.formatNumber(-1000));
    }

    @Test
    @DisplayName("Formatting.indent should indent text correctly")
    void testFormattingIndent() {
        assertEquals("  hello", UtilityModule.Formatting.indent("hello", 2));
        assertEquals("    hello\n    world", UtilityModule.Formatting.indent("hello\nworld", 4));
        assertEquals("hello", UtilityModule.Formatting.indent("hello", 0));
    }

    @Test
    @DisplayName("Formatting.stripAnsi should remove ANSI codes")
    void testFormattingStripAnsi() {
        assertEquals("hello", UtilityModule.Formatting.stripAnsi("hello"));
        assertEquals("hello world", UtilityModule.Formatting.stripAnsi("\u001B[31mhello\u001B[0m world"));
        assertEquals("", UtilityModule.Formatting.stripAnsi("\u001B[31m\u001B[0m"));
    }

    @Test
    @DisplayName("Validation.isDefined should validate defined values")
    void testValidationIsDefined() {
        assertTrue(UtilityModule.Validation.isDefined("test"));
        assertTrue(UtilityModule.Validation.isDefined(123));
        assertFalse(UtilityModule.Validation.isDefined(null));
    }

    @Test
    @DisplayName("Validation.isNonEmptyString should validate non-empty strings")
    void testValidationIsNonEmptyString() {
        assertTrue(UtilityModule.Validation.isNonEmptyString("test"));
        assertFalse(UtilityModule.Validation.isNonEmptyString(""));
        assertFalse(UtilityModule.Validation.isNonEmptyString("   "));
        assertFalse(UtilityModule.Validation.isNonEmptyString(null));
        assertFalse(UtilityModule.Validation.isNonEmptyString(123));
    }

    @Test
    @DisplayName("Validation.isNumber should validate numbers")
    void testValidationIsNumber() {
        assertTrue(UtilityModule.Validation.isNumber(123));
        assertTrue(UtilityModule.Validation.isNumber(123.45));
        assertFalse(UtilityModule.Validation.isNumber("123"));
        assertFalse(UtilityModule.Validation.isNumber(null));
    }

    @Test
    @DisplayName("Validation.isEmail should validate email addresses")
    void testValidationIsEmail() {
        assertTrue(UtilityModule.Validation.isEmail("test@example.com"));
        assertFalse(UtilityModule.Validation.isEmail("invalid-email"));
        assertFalse(UtilityModule.Validation.isEmail(null));
    }

    @Test
    @DisplayName("Validation.isUrl should validate URLs")
    void testValidationIsUrl() {
        assertTrue(UtilityModule.Validation.isUrl("https://www.example.com"));
        assertFalse(UtilityModule.Validation.isUrl("not-a-url"));
        assertFalse(UtilityModule.Validation.isUrl(null));
    }

    @Test
    @DisplayName("Validation.isValidPath should validate file paths")
    void testValidationIsValidPath() {
        assertTrue(UtilityModule.Validation.isValidPath("/path/to/file"));
        assertTrue(UtilityModule.Validation.isValidPath("relative/path"));
        assertFalse(UtilityModule.Validation.isValidPath(null));
        assertFalse(UtilityModule.Validation.isValidPath(""));
    }

    @Test
    @DisplayName("Validation.requireNonNull should throw for null values")
    void testValidationRequireNonNull() {
        String result = UtilityModule.Validation.requireNonNull("test", "Cannot be null");
        assertEquals("test", result);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> UtilityModule.Validation.requireNonNull(null, "Custom message")
        );
        assertEquals("Custom message", exception.getMessage());
    }

    @Test
    @DisplayName("Validation.requireNonEmpty should throw for empty strings")
    void testValidationRequireNonEmpty() {
        String result = UtilityModule.Validation.requireNonEmpty("test", "Cannot be empty");
        assertEquals("test", result);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> UtilityModule.Validation.requireNonEmpty("", "Custom message")
        );
        assertEquals("Custom message", exception.getMessage());
    }

    @Test
    @DisplayName("Async.delay should create completed future")
    void testAsyncDelay() {
        var future = UtilityModule.Async.delay(0);
        assertNotNull(future);
        assertTrue(future.isDone());
    }    @Test
    @DisplayName("Async.createDeferred should create deferred object")
    void testAsyncCreateDeferred() {
        var deferred = UtilityModule.Async.createDeferred();
        assertNotNull(deferred);
        assertNotNull(deferred.getPromise());
        assertFalse(deferred.getPromise().isDone());
        
        deferred.resolve("test");
        assertTrue(deferred.getPromise().isDone());
    }

    @Test
    @DisplayName("Types.success should create success result")
    void testTypesSuccess() {
        var result = UtilityModule.Types.success("test");
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("test", result.getValue());
    }

    @Test
    @DisplayName("Types.failure should create failure result")
    void testTypesFailure() {
        Exception error = new RuntimeException("test error");
        var result = UtilityModule.Types.failure(error);
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals(error, result.getError());
    }

    @Test
    @DisplayName("Types.left should create left either")
    void testTypesLeft() {
        var either = UtilityModule.Types.left("left value");
        assertNotNull(either);
        assertTrue(either.isLeft());
        assertEquals("left value", either.getLeft());
    }

    @Test
    @DisplayName("Types.right should create right either")
    void testTypesRight() {
        var either = UtilityModule.Types.right("right value");
        assertNotNull(either);
        assertTrue(either.isRight());
        assertEquals("right value", either.getRight());
    }

    @Test
    @DisplayName("Types.some should create some maybe")
    void testTypesSome() {        var maybe = UtilityModule.Types.some("value");
        assertNotNull(maybe);
        assertTrue(maybe.isSome());
        assertEquals("value", maybe.get());
    }

    @Test
    @DisplayName("Types.none should create none maybe")
    void testTypesNone() {
        var maybe = UtilityModule.Types.none();
        assertNotNull(maybe);
        assertTrue(maybe.isNone());
    }

    @Test
    @DisplayName("Types.safeCast should cast safely")
    void testTypesSafeCast() {
        Object stringObj = "test";
        Object intObj = 123;
        
        var stringResult = UtilityModule.Types.safeCast(stringObj, String.class);
        assertTrue(stringResult.isPresent());
        assertEquals("test", stringResult.get());
        
        var intAsStringResult = UtilityModule.Types.safeCast(intObj, String.class);
        assertFalse(intAsStringResult.isPresent());
        
        var nullResult = UtilityModule.Types.safeCast(null, String.class);
        assertFalse(nullResult.isPresent());
    }

    @Test
    @DisplayName("Logger should provide logging methods")
    void testLogger() {
        // These methods should not throw exceptions
        assertDoesNotThrow(() -> UtilityModule.Logger.debug("Debug message"));
        assertDoesNotThrow(() -> UtilityModule.Logger.info("Info message"));
        assertDoesNotThrow(() -> UtilityModule.Logger.warn("Warning message"));
        assertDoesNotThrow(() -> UtilityModule.Logger.error("Error message"));
        assertDoesNotThrow(() -> UtilityModule.Logger.error("Error with exception", new RuntimeException()));
    }

    @Test
    @DisplayName("Logger.createLogger should create custom logger")
    void testLoggerCreateLogger() {
        var config = new LoggerUtil.LoggerConfig();        config.setLevel(LoggerUtil.LogLevel.DEBUG);
        config.setTimestamps(true);
        
        var logger = UtilityModule.Logger.createLogger(config);
        assertNotNull(logger);
    }
}
