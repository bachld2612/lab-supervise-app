package com.bachld.backend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> data = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            data.put(fieldName, errorMessage);
        });
        Map<String, Object> errors = new HashMap<>();
        errors.put("statusCode", HttpStatus.BAD_REQUEST.value());
        errors.put("data", data);
        return ResponseEntity.badRequest().body(errors);
    }

//    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
//    @ExceptionHandler(BadCredentialsException.class)
//    public ResponseEntity<?> handleBadCredentialsException(BadCredentialsException ex) {
//        Map<String, Object> errors = new HashMap<>();
//        errors.put("statusCode", HttpStatus.UNPROCESSABLE_ENTITY.value());
//        errors.put("message", ex.getMessage().equals("Bad credentials") ? "Tên tài khoản hoặc mật khẩu không khớp" : ex.getMessage());
//        return new ResponseEntity<>(errors, HttpStatus.UNPROCESSABLE_ENTITY);
//    }

//    @ResponseStatus(HttpStatus.UNAUTHORIZED)
//    @ExceptionHandler(ExpiredJwtException.class)
//    public ResponseEntity<?> handleExpiredJwtException(ExpiredJwtException ex) {
//        Map<String, Object> errors = new HashMap<>();
//        errors.put("statusCode", HttpStatus.UNAUTHORIZED.value());
//        errors.put("message", "Unauthorized");
//        return new ResponseEntity<>(errors, HttpStatus.UNAUTHORIZED);
//    }

    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<?> handleDateTimeParseException(DateTimeParseException ex) {
        Map<String, Object> errors = new HashMap<>();
        errors.put("statusCode", HttpStatus.UNPROCESSABLE_ENTITY.value());
        errors.put("message", ex.getMessage());
        return new ResponseEntity<>(errors, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, Object> errors = new HashMap<>();
        errors.put("statusCode", HttpStatus.UNPROCESSABLE_ENTITY.value());
        errors.put("message", ex.getMessage());
        return new ResponseEntity<>(errors, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        Map<String, Object> errors = new HashMap<>();
        errors.put("statusCode", HttpStatus.UNPROCESSABLE_ENTITY.value());
        errors.put("message", "Invalid parameters");
        return new ResponseEntity<>(errors, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleForbiddenException(ResponseStatusException ex) {
        Map<String, Object> errors = new HashMap<>();
        errors.put("statusCode", HttpStatus.FORBIDDEN.value());
        errors.put("message", "Forbidden");
        return new ResponseEntity<>(errors, HttpStatus.FORBIDDEN);
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGlobalExceptions(Exception ex) {
        LOGGER.error("INTERNAL_SERVER_ERROR", ex);

        Map<String, Object> errors = new HashMap<>();
        errors.put("statusCode", HttpStatus.INTERNAL_SERVER_ERROR.value());
        errors.put("message", "Internal server error");
        return new ResponseEntity<>(errors, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}