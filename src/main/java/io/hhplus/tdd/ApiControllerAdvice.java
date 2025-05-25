package io.hhplus.tdd;

import io.hhplus.tdd.exception.DataNotFoundException;
import io.hhplus.tdd.exception.PointPolicyViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class ApiControllerAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler(DataNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDataNotFoundException(DataNotFoundException e) {
        return ResponseEntity.status(400).body(new ErrorResponse("400", e.getMessage()));
    }

    @ExceptionHandler(PointPolicyViolationException.class)
    public ResponseEntity<ErrorResponse> handlePointPolicyViolationException(PointPolicyViolationException e) {
        return ResponseEntity.status(400).body(new ErrorResponse("400", e.getMessage()));
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        return ResponseEntity.status(500).body(new ErrorResponse("500", "에러가 발생했습니다."));
    }
}
