package sg.edu.nus.iss.misoto.cli.utils;

/**
 * Utility Module
 * 
 * Exports various utility functions used throughout the application.
 * This serves as a central point for accessing all utility classes.
 */
public class UtilityModule {
    
    /**
     * Formatting utilities
     */
    public static class Formatting {
        public static String truncate(String text, int maxLength) {
            return FormattingUtil.truncate(text, maxLength);
        }
        
        public static String truncate(String text, int maxLength, String suffix) {
            return FormattingUtil.truncate(text, maxLength, suffix);
        }
        
        public static String formatFileSize(long bytes) {
            return FormattingUtil.formatFileSize(bytes);
        }
        
        public static String formatDuration(long ms) {
            return FormattingUtil.formatDuration(ms);
        }
        
        public static String formatNumber(long num) {
            return FormattingUtil.formatNumber(num);
        }
        
        public static String indent(String text, int spaces) {
            return FormattingUtil.indent(text, spaces);
        }
        
        public static String stripAnsi(String text) {
            return FormattingUtil.stripAnsi(text);
        }
    }
    
    /**
     * Validation utilities
     */
    public static class Validation {
        public static boolean isDefined(Object value) {
            return ValidationUtil.isDefined(value);
        }
        
        public static boolean isNonEmptyString(Object value) {
            return ValidationUtil.isNonEmptyString(value);
        }
        
        public static boolean isNumber(Object value) {
            return ValidationUtil.isNumber(value);
        }
        
        public static boolean isEmail(String value) {
            return ValidationUtil.isEmail(value);
        }
        
        public static boolean isUrl(String value) {
            return ValidationUtil.isUrl(value);
        }
        
        public static boolean isValidPath(String value) {
            return ValidationUtil.isValidPath(value);
        }
        
        public static <T> T requireNonNull(T value, String message) {
            return ValidationUtil.requireNonNull(value, message);
        }
        
        public static String requireNonEmpty(String value, String message) {
            return ValidationUtil.requireNonEmpty(value, message);
        }
    }
    
    /**
     * Async utilities
     */
    public static class Async {
        public static java.util.concurrent.CompletableFuture<Void> delay(long ms) {
            return AsyncUtil.delay(ms);
        }
        
        public static <T> java.util.concurrent.CompletableFuture<T> withTimeout(
                java.util.function.Supplier<java.util.concurrent.CompletableFuture<T>> supplier, 
                long timeoutMs) {
            return AsyncUtil.withTimeout(supplier, timeoutMs);
        }
        
        public static <T> java.util.concurrent.CompletableFuture<T> withRetry(
                java.util.function.Supplier<java.util.concurrent.CompletableFuture<T>> supplier) {
            return AsyncUtil.withRetry(supplier);
        }
        
        public static <T> AsyncUtil.Deferred<T> createDeferred() {
            return AsyncUtil.createDeferred();
        }
    }
    
    /**
     * Type utilities
     */
    public static class Types {
        public static <T, E extends Exception> TypeUtil.Result<T, E> success(T value) {
            return TypeUtil.Result.success(value);
        }
        
        public static <T, E extends Exception> TypeUtil.Result<T, E> failure(E error) {
            return TypeUtil.Result.failure(error);
        }
        
        public static <L, R> TypeUtil.Either<L, R> left(L value) {
            return TypeUtil.Either.left(value);
        }
        
        public static <L, R> TypeUtil.Either<L, R> right(R value) {
            return TypeUtil.Either.right(value);
        }
        
        public static <T> TypeUtil.Maybe<T> some(T value) {
            return TypeUtil.Maybe.some(value);
        }
        
        public static <T> TypeUtil.Maybe<T> none() {
            return TypeUtil.Maybe.none();
        }
        
        public static <T> java.util.Optional<T> safeCast(Object obj, Class<T> type) {
            return TypeUtil.Types.safeCast(obj, type);
        }
    }
    
    /**
     * Logger utilities
     */
    public static class Logger {
        private static final LoggerUtil defaultLogger = new LoggerUtil();
        
        public static void debug(String message) {
            defaultLogger.debug(message);
        }
        
        public static void info(String message) {
            defaultLogger.info(message);
        }
        
        public static void warn(String message) {
            defaultLogger.warn(message);
        }
        
        public static void error(String message) {
            defaultLogger.error(message);
        }
        
        public static void error(String message, Throwable throwable) {
            defaultLogger.error(message, throwable);
        }
        
        public static LoggerUtil createLogger(LoggerUtil.LoggerConfig config) {
            return new LoggerUtil(config);
        }
    }
}
