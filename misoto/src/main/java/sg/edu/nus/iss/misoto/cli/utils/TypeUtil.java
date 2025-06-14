package sg.edu.nus.iss.misoto.cli.utils;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Type Utilities and Common Types
 * 
 * Common types and type-related utilities used throughout the application.
 * This helps centralize type definitions and avoid duplication.
 */
public class TypeUtil {
    
    /**
     * Basic callback function interface
     */
    @FunctionalInterface
    public interface Callback<T> {
        void call(Exception error, T result);
    }
    
    /**
     * Async function that returns a CompletableFuture
     */
    @FunctionalInterface
    public interface AsyncFunction<T, R> {
        CompletableFuture<R> apply(T input);
    }
    
    /**
     * Function with timeout capability
     */
    public static class TimedFunction<T, R> {
        private final Function<T, CompletableFuture<R>> function;
        private final long timeoutMs;
        
        public TimedFunction(Function<T, CompletableFuture<R>> function, long timeoutMs) {
            this.function = function;
            this.timeoutMs = timeoutMs;
        }
        
        public CompletableFuture<R> apply(T input) {
            return AsyncUtil.withTimeout(() -> function.apply(input), timeoutMs);
        }
        
        public long getTimeout() {
            return timeoutMs;
        }
    }
    
    /**
     * Result of an operation that can succeed or fail
     */
    public static class Result<T, E extends Exception> {
        private final boolean success;
        private final T value;
        private final E error;
        
        private Result(boolean success, T value, E error) {
            this.success = success;
            this.value = value;
            this.error = error;
        }
        
        public static <T, E extends Exception> Result<T, E> success(T value) {
            return new Result<>(true, value, null);
        }
        
        public static <T, E extends Exception> Result<T, E> failure(E error) {
            return new Result<>(false, null, error);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public boolean isFailure() {
            return !success;
        }
        
        public T getValue() {
            if (!success) {
                throw new IllegalStateException("Cannot get value from failed result");
            }
            return value;
        }
        
        public E getError() {
            if (success) {
                throw new IllegalStateException("Cannot get error from successful result");
            }
            return error;
        }
        
        public T getOrElse(T defaultValue) {
            return success ? value : defaultValue;
        }
        
        public T getOrThrow() throws E {
            if (success) {
                return value;
            } else {
                throw error;
            }
        }
        
        public <U> Result<U, E> map(Function<T, U> mapper) {
            if (success) {
                return Result.success(mapper.apply(value));
            } else {
                return Result.failure(error);
            }
        }
        
        public <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
            if (success) {
                return mapper.apply(value);
            } else {
                return Result.failure(error);
            }
        }
    }
    
    /**
     * Success result shorthand
     */
    public static class Success<T> extends Result<T, Exception> {
        private Success(T value) {
            super(true, value, null);
        }
        
        public static <T> Success<T> of(T value) {
            return new Success<>(value);
        }
    }
    
    /**
     * Failure result shorthand
     */
    public static class Failure<E extends Exception> extends Result<Object, E> {
        private Failure(E error) {
            super(false, null, error);
        }
        
        public static <E extends Exception> Failure<E> of(E error) {
            return new Failure<>(error);
        }
    }
    
    /**
     * Either type - represents a value that can be one of two types
     */
    public static abstract class Either<L, R> {
        
        public abstract boolean isLeft();
        public abstract boolean isRight();
        public abstract L getLeft();
        public abstract R getRight();
        
        public static <L, R> Either<L, R> left(L value) {
            return new Left<>(value);
        }
        
        public static <L, R> Either<L, R> right(R value) {
            return new Right<>(value);
        }
        
        public <T> T fold(Function<L, T> leftMapper, Function<R, T> rightMapper) {
            if (isLeft()) {
                return leftMapper.apply(getLeft());
            } else {
                return rightMapper.apply(getRight());
            }
        }
        
        public <T> Either<L, T> map(Function<R, T> mapper) {
            if (isRight()) {
                return Either.right(mapper.apply(getRight()));
            } else {
                return Either.left(getLeft());
            }
        }
        
        public <T> Either<T, R> mapLeft(Function<L, T> mapper) {
            if (isLeft()) {
                return Either.left(mapper.apply(getLeft()));
            } else {
                return Either.right(getRight());
            }
        }
        
        private static class Left<L, R> extends Either<L, R> {
            private final L value;
            
            private Left(L value) {
                this.value = value;
            }
            
            @Override
            public boolean isLeft() {
                return true;
            }
            
            @Override
            public boolean isRight() {
                return false;
            }
            
            @Override
            public L getLeft() {
                return value;
            }
            
            @Override
            public R getRight() {
                throw new IllegalStateException("Cannot get right value from left");
            }
        }
        
        private static class Right<L, R> extends Either<L, R> {
            private final R value;
            
            private Right(R value) {
                this.value = value;
            }
            
            @Override
            public boolean isLeft() {
                return false;
            }
            
            @Override
            public boolean isRight() {
                return true;
            }
            
            @Override
            public L getLeft() {
                throw new IllegalStateException("Cannot get left value from right");
            }
            
            @Override
            public R getRight() {
                return value;
            }
        }
    }
    
    /**
     * Optional-like type for operations that may not return a value
     */
    public static class Maybe<T> {
        private final T value;
        private final boolean hasValue;
        
        private Maybe(T value, boolean hasValue) {
            this.value = value;
            this.hasValue = hasValue;
        }
        
        public static <T> Maybe<T> some(T value) {
            return new Maybe<>(value, true);
        }
        
        public static <T> Maybe<T> none() {
            return new Maybe<>(null, false);
        }
        
        public static <T> Maybe<T> fromNullable(T value) {
            return value != null ? some(value) : none();
        }
        
        public boolean isSome() {
            return hasValue;
        }
        
        public boolean isNone() {
            return !hasValue;
        }
        
        public T get() {
            if (!hasValue) {
                throw new IllegalStateException("Cannot get value from empty Maybe");
            }
            return value;
        }
        
        public T getOrElse(T defaultValue) {
            return hasValue ? value : defaultValue;
        }
        
        public <U> Maybe<U> map(Function<T, U> mapper) {
            if (hasValue) {
                return Maybe.some(mapper.apply(value));
            } else {
                return Maybe.none();
            }
        }
        
        public <U> Maybe<U> flatMap(Function<T, Maybe<U>> mapper) {
            if (hasValue) {
                return mapper.apply(value);
            } else {
                return Maybe.none();
            }
        }
        
        public Maybe<T> filter(java.util.function.Predicate<T> predicate) {
            if (hasValue && predicate.test(value)) {
                return this;
            } else {
                return Maybe.none();
            }
        }
    }
    
    /**
     * Utility methods for working with types
     */
    public static class Types {
        
        /**
         * Safe cast to a specific type
         */
        @SuppressWarnings("unchecked")
        public static <T> java.util.Optional<T> safeCast(Object obj, Class<T> type) {
            if (type.isInstance(obj)) {
                return java.util.Optional.of((T) obj);
            }
            return java.util.Optional.empty();
        }
        
        /**
         * Check if an object is of a specific type
         */
        public static boolean isInstanceOf(Object obj, Class<?> type) {
            return obj != null && type.isInstance(obj);
        }
        
        /**
         * Get the simple class name
         */
        public static String getSimpleClassName(Object obj) {
            return obj != null ? obj.getClass().getSimpleName() : "null";
        }
        
        /**
         * Get the full class name
         */
        public static String getClassName(Object obj) {
            return obj != null ? obj.getClass().getName() : "null";
        }
    }
}
