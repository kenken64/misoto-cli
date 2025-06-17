package sg.edu.nus.iss.misoto.cli.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.*;
import java.util.regex.Pattern;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for ValidationUtil
 */
@DisplayName("ValidationUtil Tests")
class ValidationUtilTest {

    @Test
    @DisplayName("isDefined should return true for non-null values")
    void testIsDefined() {
        assertTrue(ValidationUtil.isDefined("test"));
        assertTrue(ValidationUtil.isDefined(123));
        assertTrue(ValidationUtil.isDefined(new Object()));
        assertTrue(ValidationUtil.isDefined(Collections.emptyList()));
    }

    @Test
    @DisplayName("isDefined should return false for null values")
    void testIsDefinedWithNull() {
        assertFalse(ValidationUtil.isDefined(null));
    }

    @Test
    @DisplayName("isNonEmptyString should validate strings correctly")
    void testIsNonEmptyString() {
        // Valid non-empty strings
        assertTrue(ValidationUtil.isNonEmptyString("test"));
        assertTrue(ValidationUtil.isNonEmptyString("hello world"));
        assertTrue(ValidationUtil.isNonEmptyString("   valid   "));
        
        // Invalid cases
        assertFalse(ValidationUtil.isNonEmptyString(null));
        assertFalse(ValidationUtil.isNonEmptyString(""));
        assertFalse(ValidationUtil.isNonEmptyString("   "));
        assertFalse(ValidationUtil.isNonEmptyString(123));
        assertFalse(ValidationUtil.isNonEmptyString(new Object()));
    }

    @Test
    @DisplayName("isNumber should validate numbers correctly")
    void testIsNumber() {
        // Valid numbers
        assertTrue(ValidationUtil.isNumber(123));
        assertTrue(ValidationUtil.isNumber(123L));
        assertTrue(ValidationUtil.isNumber(123.45));
        assertTrue(ValidationUtil.isNumber(123.45f));
        
        // Invalid cases
        assertFalse(ValidationUtil.isNumber(null));
        assertFalse(ValidationUtil.isNumber("123"));
        assertFalse(ValidationUtil.isNumber(new Object()));
    }

    @Test
    @DisplayName("isNumber with range should validate correctly")
    void testIsNumberWithRange() {
        assertTrue(ValidationUtil.isNumber(50, 0.0, 100.0));
        assertTrue(ValidationUtil.isNumber(0, 0.0, 100.0));
        assertTrue(ValidationUtil.isNumber(100, 0.0, 100.0));
        
        assertFalse(ValidationUtil.isNumber(-1, 0.0, 100.0));
        assertFalse(ValidationUtil.isNumber(101, 0.0, 100.0));
        assertFalse(ValidationUtil.isNumber("50", 0.0, 100.0));
    }

    @Test
    @DisplayName("isBoolean should validate booleans correctly")
    void testIsBoolean() {
        assertTrue(ValidationUtil.isBoolean(true));
        assertTrue(ValidationUtil.isBoolean(false));
        assertTrue(ValidationUtil.isBoolean(Boolean.TRUE));
        assertTrue(ValidationUtil.isBoolean(Boolean.FALSE));
        
        assertFalse(ValidationUtil.isBoolean(null));
        assertFalse(ValidationUtil.isBoolean("true"));
        assertFalse(ValidationUtil.isBoolean(1));
        assertFalse(ValidationUtil.isBoolean(0));
    }

    @Test
    @DisplayName("isObject should validate objects correctly")
    void testIsObject() {
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        assertTrue(ValidationUtil.isObject(map));
        
        assertFalse(ValidationUtil.isObject(null));
        assertFalse(ValidationUtil.isObject(Collections.emptyMap()));
        assertFalse(ValidationUtil.isObject("string"));
        assertFalse(ValidationUtil.isObject(123));
    }

    @Test
    @DisplayName("isArray should validate arrays correctly")
    void testIsArray() {
        assertTrue(ValidationUtil.isArray(Arrays.asList("a", "b", "c")));
        assertTrue(ValidationUtil.isArray(Collections.emptyList()));
        assertTrue(ValidationUtil.isArray(new ArrayList<>()));
        
        assertFalse(ValidationUtil.isArray(null));
        assertFalse(ValidationUtil.isArray("string"));
        assertFalse(ValidationUtil.isArray(123));
    }

    @Test
    @DisplayName("isValidDate should validate dates correctly")
    void testIsValidDate() {
        assertTrue(ValidationUtil.isValidDate(LocalDateTime.now()));
        assertTrue(ValidationUtil.isValidDate(new Date()));
        
        assertFalse(ValidationUtil.isValidDate(null));
        assertFalse(ValidationUtil.isValidDate("2023-01-01"));
        assertFalse(ValidationUtil.isValidDate(123456789L));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "test@example.com",
        "user.name@domain.co.uk",
        "user+tag@domain.org",
        "user123@test-domain.com"
    })
    @DisplayName("isEmail should validate valid email addresses")
    void testIsEmailValid(String email) {
        assertTrue(ValidationUtil.isEmail(email));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "invalid-email",
        "@domain.com",
        "user@",
        "user@domain",
        "user space@domain.com",
        "user@domain..com"
    })
    @DisplayName("isEmail should reject invalid email addresses")
    void testIsEmailInvalid(String email) {
        assertFalse(ValidationUtil.isEmail(email));
    }

    @Test
    @DisplayName("isEmail should handle null")
    void testIsEmailNull() {
        assertFalse(ValidationUtil.isEmail(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://www.example.com",
        "http://localhost:8080",
        "https://api.domain.com/v1/endpoint",
        "ftp://files.example.com"
    })
    @DisplayName("isUrl should validate valid URLs")
    void testIsUrlValid(String url) {
        assertTrue(ValidationUtil.isUrl(url));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "not-a-url",
        "http://",
        "://example.com",
        "http://invalid url with spaces"
    })
    @DisplayName("isUrl should reject invalid URLs")
    void testIsUrlInvalid(String url) {
        assertFalse(ValidationUtil.isUrl(url));
    }

    @Test
    @DisplayName("isUrl should handle null")
    void testIsUrlNull() {
        assertFalse(ValidationUtil.isUrl(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/path/to/file",
        "C:\\Windows\\System32",
        "~/documents/file.txt",
        "relative/path/file.txt",
        "."
    })
    @DisplayName("isValidPath should validate valid paths")
    void testIsValidPathValid(String path) {
        assertTrue(ValidationUtil.isValidPath(path));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("isValidPath should reject null, empty, or whitespace paths")
    void testIsValidPathInvalid(String path) {
        assertFalse(ValidationUtil.isValidPath(path));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "123e4567-e89b-12d3-a456-426614174000",
        "550e8400-e29b-41d4-a716-446655440000",
        "6ba7b810-9dad-11d1-80b4-00c04fd430c8"
    })
    @DisplayName("isUuid should validate valid UUIDs")
    void testIsUuidValid(String uuid) {
        assertTrue(ValidationUtil.isUuid(uuid));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "not-a-uuid",
        "123e4567-e89b-12d3-a456-42661417400",  // too short
        "123e4567-e89b-12d3-a456-4266141740000", // too long
        "123e4567-e89b-12d3-a456-42661417400g"  // invalid character
    })
    @DisplayName("isUuid should reject invalid UUIDs")
    void testIsUuidInvalid(String uuid) {
        assertFalse(ValidationUtil.isUuid(uuid));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "192.168.1.1",
        "10.0.0.1",
        "172.16.254.1",
        "127.0.0.1",
        "255.255.255.255"
    })
    @DisplayName("isIpv4 should validate valid IPv4 addresses")
    void testIsIpv4Valid(String ip) {
        assertTrue(ValidationUtil.isIpv4(ip));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "256.1.1.1",        // out of range
        "192.168.1",        // incomplete
        "192.168.1.1.1",    // too many parts
        "not-an-ip",        // not numeric
        "192.168.01.1"      // leading zeros
    })
    @DisplayName("isIpv4 should reject invalid IPv4 addresses")
    void testIsIpv4Invalid(String ip) {
        assertFalse(ValidationUtil.isIpv4(ip));
    }    @Test
    @DisplayName("matches should validate regex patterns correctly")
    void testMatches() {
        assertTrue(ValidationUtil.matches("hello123", Pattern.compile("\\w+")));
        assertTrue(ValidationUtil.matches("test@example.com", Pattern.compile("\\w+@\\w+\\.\\w+")));
        assertFalse(ValidationUtil.matches("hello world", Pattern.compile("\\w+")));
        assertFalse(ValidationUtil.matches(null, Pattern.compile("\\w+")));
        assertFalse(ValidationUtil.matches("test", (Pattern) null));
    }

    @Test
    @DisplayName("hasMinLength should validate string length correctly")
    void testHasMinLength() {
        assertTrue(ValidationUtil.hasMinLength("hello", 3));
        assertTrue(ValidationUtil.hasMinLength("hello", 5));
        assertFalse(ValidationUtil.hasMinLength("hi", 3));
        assertFalse(ValidationUtil.hasMinLength(null, 1));
    }

    @Test
    @DisplayName("hasMaxLength should validate string length correctly")
    void testHasMaxLength() {
        assertTrue(ValidationUtil.hasMaxLength("hi", 5));
        assertTrue(ValidationUtil.hasMaxLength("hello", 5));
        assertFalse(ValidationUtil.hasMaxLength("hello world", 5));
        assertFalse(ValidationUtil.hasMaxLength(null, 5));
    }

    @Test
    @DisplayName("hasLengthInRange should validate string length in range")
    void testHasLengthInRange() {
        assertTrue(ValidationUtil.hasLengthInRange("hello", 3, 7));
        assertTrue(ValidationUtil.hasLengthInRange("hi", 2, 5));
        assertFalse(ValidationUtil.hasLengthInRange("a", 2, 5));
        assertFalse(ValidationUtil.hasLengthInRange("too long string", 2, 5));
        assertFalse(ValidationUtil.hasLengthInRange(null, 2, 5));
    }

    @Test
    @DisplayName("collection size validation methods should work correctly")
    void testCollectionSizeValidation() {
        List<String> list = Arrays.asList("a", "b", "c");
        
        assertTrue(ValidationUtil.hasMinSize(list, 2));
        assertTrue(ValidationUtil.hasMaxSize(list, 5));
        assertTrue(ValidationUtil.hasSizeInRange(list, 2, 5));
        
        assertFalse(ValidationUtil.hasMinSize(list, 5));
        assertFalse(ValidationUtil.hasMaxSize(list, 2));
        assertFalse(ValidationUtil.hasSizeInRange(list, 5, 10));
        
        assertFalse(ValidationUtil.hasMinSize(null, 1));
        assertFalse(ValidationUtil.hasMaxSize(null, 1));
        assertFalse(ValidationUtil.hasSizeInRange(null, 1, 5));
    }

    @Test
    @DisplayName("allValid should validate all collection items")
    void testAllValid() {
        List<String> validEmails = Arrays.asList("test@example.com", "user@domain.org");
        List<String> mixedEmails = Arrays.asList("test@example.com", "invalid-email");
        
        assertTrue(ValidationUtil.allValid(validEmails, ValidationUtil::isEmail));
        assertFalse(ValidationUtil.allValid(mixedEmails, ValidationUtil::isEmail));
        assertFalse(ValidationUtil.allValid(null, ValidationUtil::isEmail));
        assertFalse(ValidationUtil.allValid(validEmails, null));
    }

    @Test
    @DisplayName("anyValid should validate any collection item")
    void testAnyValid() {
        List<String> mixedEmails = Arrays.asList("invalid-email", "test@example.com");
        List<String> invalidEmails = Arrays.asList("invalid1", "invalid2");
        
        assertTrue(ValidationUtil.anyValid(mixedEmails, ValidationUtil::isEmail));
        assertFalse(ValidationUtil.anyValid(invalidEmails, ValidationUtil::isEmail));
        assertFalse(ValidationUtil.anyValid(null, ValidationUtil::isEmail));
        assertFalse(ValidationUtil.anyValid(mixedEmails, null));
    }

    @Test
    @DisplayName("isOneOf should validate value in collection")
    void testIsOneOf() {
        List<String> validValues = Arrays.asList("red", "green", "blue");
        
        assertTrue(ValidationUtil.isOneOf("red", validValues));
        assertFalse(ValidationUtil.isOneOf("yellow", validValues));
        assertFalse(ValidationUtil.isOneOf(null, validValues));
        assertFalse(ValidationUtil.isOneOf("red", (Collection<String>) null));
    }

    @Test
    @DisplayName("isOneOf with varargs should validate value in array")
    void testIsOneOfVarargs() {
        assertTrue(ValidationUtil.isOneOf("red", "red", "green", "blue"));
        assertFalse(ValidationUtil.isOneOf("yellow", "red", "green", "blue"));
        assertFalse(ValidationUtil.isOneOf(null, "red", "green", "blue"));
    }

    @Test
    @DisplayName("requireNonNull should throw for null values")
    void testRequireNonNull() {
        String result = ValidationUtil.requireNonNull("test", "Value cannot be null");
        assertEquals("test", result);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ValidationUtil.requireNonNull(null, "Custom message")
        );
        assertEquals("Custom message", exception.getMessage());
        
        // Test default message
        exception = assertThrows(
            IllegalArgumentException.class,
            () -> ValidationUtil.requireNonNull(null, null)
        );
        assertEquals("Value cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("requireNonEmpty should throw for null or empty strings")
    void testRequireNonEmpty() {        String result = ValidationUtil.requireNonEmpty("test", "String cannot be empty");
        assertEquals("test", result);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ValidationUtil.requireNonEmpty((String) null, "Custom message")
        );
        assertEquals("Custom message", exception.getMessage());
        
        exception = assertThrows(
            IllegalArgumentException.class,
            () -> ValidationUtil.requireNonEmpty("   ", "Custom message")
        );
        assertEquals("Custom message", exception.getMessage());
    }

    @Test
    @DisplayName("requireNonEmpty collection should throw for null or empty collections")
    void testRequireNonEmptyCollection() {
        List<String> list = Arrays.asList("a", "b");
        List<String> result = ValidationUtil.requireNonEmpty(list, "Collection cannot be empty");
        assertEquals(list, result);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ValidationUtil.requireNonEmpty((List<String>) null, "Custom message")
        );
        assertEquals("Custom message", exception.getMessage());
        
        exception = assertThrows(
            IllegalArgumentException.class,
            () -> ValidationUtil.requireNonEmpty(Collections.emptyList(), "Custom message")
        );
        assertEquals("Custom message", exception.getMessage());
    }

    @Test
    @DisplayName("require should throw for false conditions")
    void testRequire() {
        assertDoesNotThrow(() -> ValidationUtil.require(true, "Should not throw"));
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ValidationUtil.require(false, "Custom validation message")
        );
        assertEquals("Custom validation message", exception.getMessage());
        
        // Test default message
        exception = assertThrows(
            IllegalArgumentException.class,
            () -> ValidationUtil.require(false, null)
        );
        assertEquals("Validation failed", exception.getMessage());
    }
}
