package com.bank_system.bank_system.exceptions;

import com.bank_system.bank_system.payloads.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiResponse> handleAccountNotFound(AccountNotFoundException ex) {
        log.error("[EXCEPTION] AccountNotFoundException: {}", ex.getMessage());
        return new ResponseEntity<>(new ApiResponse(ex.getMessage(), false), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ApiResponse> handleInsufficientFunds(InsufficientFundsException ex) {
        log.error("[EXCEPTION] InsufficientFundsException: {}", ex.getMessage());
        return new ResponseEntity<>(new ApiResponse(ex.getMessage(), false), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        log.error("[EXCEPTION] ResourceNotFoundException: {}", ex.getMessage());
        return new ResponseEntity<>(new ApiResponse(ex.getMessage(), false), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse> handleApiException(ApiException ex) {
        log.error("[EXCEPTION] ApiException: {}", ex.getMessage());
        return new ResponseEntity<>(new ApiResponse(ex.getMessage(), false), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.error("[EXCEPTION] IllegalArgumentException: {}", ex.getMessage());
        return new ResponseEntity<>(new ApiResponse(ex.getMessage(), false), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse> handleBadCredentials(BadCredentialsException ex) {
        log.error("[EXCEPTION] BadCredentialsException: Invalid login attempt");
        return new ResponseEntity<>(new ApiResponse("Invalid email or password", false), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGeneralException(Exception ex) {
        log.error("[EXCEPTION] Unhandled exception: {} — {}", ex.getClass().getSimpleName(), ex.getMessage());
        return new ResponseEntity<>(new ApiResponse("An unexpected error occurred: " + ex.getMessage(), false), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}