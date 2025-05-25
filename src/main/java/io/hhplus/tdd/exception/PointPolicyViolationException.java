package io.hhplus.tdd.exception;

public class PointPolicyViolationException extends RuntimeException {
    public PointPolicyViolationException(String message) {
        super(message);
    }

    public PointPolicyViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
