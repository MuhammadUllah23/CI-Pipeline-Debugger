package com.muhammadullah.ci_debugger.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<Map<String, Object>> handleServiceException(ServiceException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", ex.getErrorCode().name());
        response.put("message", ex.getMessage());
        response.put("status", ex.getErrorCode().getStatus().value());

        if (!ex.getDetails().isEmpty()) {
            response.put("details", ex.getDetails());
        }

        return ResponseEntity.status(ex.getErrorCode().getStatus()).body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", 400);
        body.put("error", "Bad Request");
        body.put("message", ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage());
        body.put("path", request.getServletPath());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex, HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", 500);
        body.put("error", "Internal Server Error");
        body.put("message", ex.getMessage());
        body.put("path", request.getServletPath());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<Map<String, Object>> handleJsonProcessingException(
            JsonProcessingException ex, HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();

        //**  Using generic message to avoid leaking details
        body.put("status", 400);
        body.put("error", "Bad Request");
        body.put("message", "Malformed JSON payload");
        body.put("path", request.getServletPath());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
