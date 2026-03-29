package com.example.nextjsbackendadmin.api;

import com.example.nextjsbackendadmin.supabase.SupabaseException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralized exception-to-response mapping.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SupabaseException.class)
    public ResponseEntity<ApiError> handleSupabase(SupabaseException ex) {
        HttpStatus status = HttpStatus.resolve(ex.statusCode());
        if (status == null) status = HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(ApiError.of(ex.getMessage(), "SUPABASE_ERROR"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleInvalid(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body(ApiError.of("Validation failed", "VALIDATION_ERROR"));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleViolation(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(ApiError.of("Validation failed", "VALIDATION_ERROR"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegal(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ApiError.of(ex.getMessage(), "BAD_REQUEST"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of("Unexpected server error", "INTERNAL_ERROR"));
    }
}
