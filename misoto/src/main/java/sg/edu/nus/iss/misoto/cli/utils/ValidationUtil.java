package sg.edu.nus.iss.misoto.cli.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Validation Utilities
 * 
 * Provides utilities for validating inputs, checking types,
 * and ensuring data conforms to expected formats.
 */
public class ValidationUtil {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );
    
    private static final Pattern IPV4_PATTERN = Pattern.compile(
        "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );
    
    /**
     * Check if a value is defined (not null)
     */
    public static <T> boolean isDefined(T value) {
        return value != null;
    }
    
    /**
     * Check if a value is a non-empty string
     */
    public static boolean isNonEmptyString(Object value) {
        return value instanceof String && !((String) value).trim().isEmpty();
    }
    
    /**
     * Check if a value is a number (and optionally within range)
     */
    public static boolean isNumber(Object value) {
        return isNumber(value, null, null);
    }
    
    /**
     * Check if a value is a number within range
     */
    public static boolean isNumber(Object value, Double min, Double max) {
        if (!(value instanceof Number)) {
            return false;
        }
        
        double numValue = ((Number) value).doubleValue();
        
        if (Double.isNaN(numValue) || Double.isInfinite(numValue)) {
            return false;
        }
        
        if (min != null && numValue < min) {
            return false;
        }
        
        if (max != null && numValue > max) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if a value is a boolean
     */
    public static boolean isBoolean(Object value) {
        return value instanceof Boolean;
    }
    
    /**
     * Check if a value is an object (Map) and not null
     */
    public static boolean isObject(Object value) {
        return value instanceof Map && !((Map<?, ?>) value).isEmpty();
    }
    
    /**
     * Check if a value is an array (Collection)
     */
    public static boolean isArray(Object value) {
        return value instanceof Collection;
    }
    
    /**
     * Check if a value is an array with item validation
     */
    public static <T> boolean isArray(Object value, Predicate<Object> itemValidator) {
        if (!(value instanceof Collection)) {
            return false;
        }
        
        Collection<?> collection = (Collection<?>) value;
        
        if (itemValidator != null) {
            return collection.stream().allMatch(itemValidator::test);
        }
        
        return true;
    }
    
    /**
     * Check if a value is a valid date
     */
    public static boolean isValidDate(Object value) {
        return value instanceof LocalDateTime || value instanceof java.util.Date;
    }
    
    /**
     * Check if a string is a valid email address
     */
    public static boolean isEmail(String value) {
        if (value == null) return false;
        return EMAIL_PATTERN.matcher(value).matches();
    }
    
    /**
     * Check if a string is a valid URL
     */
    public static boolean isUrl(String value) {
        if (value == null) return false;
        
        try {
            new URL(value);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }
    
    /**
     * Check if a string is a valid file path
     */
    public static boolean isValidPath(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        
        try {
            Paths.get(value);
            return true;
        } catch (InvalidPathException e) {
            return false;
        }
    }
    
    /**
     * Check if a string is a valid file path (alias for consistency)
     */
    public static boolean isValidFilePath(String value) {
        return isValidPath(value);
    }
    
    /**
     * Check if a string is a valid directory path
     */
    public static boolean isValidDirectoryPath(String value) {
        return isValidPath(value);
    }
    
    /**
     * Check if a string is a valid UUID
     */
    public static boolean isUuid(String value) {
        if (value == null) return false;
        return UUID_PATTERN.matcher(value).matches();
    }
    
    /**
     * Check if a string is a valid IPv4 address
     */
    public static boolean isIpv4(String value) {
        if (value == null) return false;
        return IPV4_PATTERN.matcher(value).matches();
    }
    
    /**
     * Check if a string matches a regular expression
     */
    public static boolean matches(String value, String regex) {
        if (value == null || regex == null) return false;
        return Pattern.matches(regex, value);
    }
    
    /**
     * Check if a string matches a pattern
     */
    public static boolean matches(String value, Pattern pattern) {
        if (value == null || pattern == null) return false;
        return pattern.matcher(value).matches();
    }
    
    /**
     * Check if a value is within a collection of valid values
     */
    public static <T> boolean isOneOf(T value, Collection<T> validValues) {
        if (validValues == null) return false;
        return validValues.contains(value);
    }
    
    /**
     * Check if a value is within an array of valid values
     */
    @SafeVarargs
    public static <T> boolean isOneOf(T value, T... validValues) {
        if (validValues == null) return false;
        
        for (T validValue : validValues) {
            if (java.util.Objects.equals(value, validValue)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if a string has a minimum length
     */
    public static boolean hasMinLength(String value, int minLength) {
        return value != null && value.length() >= minLength;
    }
    
    /**
     * Check if a string has a maximum length
     */
    public static boolean hasMaxLength(String value, int maxLength) {
        return value == null || value.length() <= maxLength;
    }
    
    /**
     * Check if a string length is within range
     */
    public static boolean hasLengthInRange(String value, int minLength, int maxLength) {
        return hasMinLength(value, minLength) && hasMaxLength(value, maxLength);
    }
    
    /**
     * Check if a collection has a minimum size
     */
    public static boolean hasMinSize(Collection<?> collection, int minSize) {
        return collection != null && collection.size() >= minSize;
    }
    
    /**
     * Check if a collection has a maximum size
     */
    public static boolean hasMaxSize(Collection<?> collection, int maxSize) {
        return collection == null || collection.size() <= maxSize;
    }
    
    /**
     * Check if a collection size is within range
     */
    public static boolean hasSizeInRange(Collection<?> collection, int minSize, int maxSize) {
        return hasMinSize(collection, minSize) && hasMaxSize(collection, maxSize);
    }
    
    /**
     * Check if all values in a collection are valid according to predicate
     */
    public static <T> boolean allValid(Collection<T> collection, Predicate<T> validator) {
        if (collection == null || validator == null) return false;
        return collection.stream().allMatch(validator);
    }
    
    /**
     * Check if any value in a collection is valid according to predicate
     */
    public static <T> boolean anyValid(Collection<T> collection, Predicate<T> validator) {
        if (collection == null || validator == null) return false;
        return collection.stream().anyMatch(validator);
    }
    
    /**
     * Require that a value is not null
     */
    public static <T> T requireNonNull(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message != null ? message : "Value cannot be null");
        }
        return value;
    }
    
    /**
     * Require that a string is not null or empty
     */
    public static String requireNonEmpty(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message != null ? message : "String cannot be null or empty");
        }
        return value;
    }
    
    /**
     * Require that a collection is not null or empty
     */
    public static <T extends Collection<?>> T requireNonEmpty(T collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new IllegalArgumentException(message != null ? message : "Collection cannot be null or empty");
        }
        return collection;
    }
    
    /**
     * Validate that a condition is true
     */
    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message != null ? message : "Validation failed");
        }
    }
}
