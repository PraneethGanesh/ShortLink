package com.praneeth.identityservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors =
                exception.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .collect(Collectors.toMap(
                                FieldError::getField,
                                error -> error.getDefaultMessage() == null
                                        ? "Invalid value"
                                        : error.getDefaultMessage(),
                                (first, second) -> first
                        ));

        ApiError error = new ApiError(
                400,
                "Bad Request",
                "Validation failed",
                request.getRequestURI(),
                fieldErrors,
                Instant.now()
        );

        return ResponseEntity.badRequest().body(error);
    }
}