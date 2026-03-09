package com.example.file_storage.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    public record ErrorMessage (String message) {};

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<@NotNull ErrorMessage> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req){

        log.error("Unhandled exception: method={}, path={}, ip={}",
                req.getMethod(), req.getRequestURI(), req.getRemoteAddr(), ex);

        String msg = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Argument is not valid");

        return ResponseEntity.badRequest().body(new ErrorMessage(msg));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<@NotNull ErrorMessage> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest
            req) {

        log.error("Unhandled exception: method={}, path={}, ip={}",
                req.getMethod(), req.getRequestURI(), req.getRemoteAddr(), ex);

        return ResponseEntity.badRequest().body(new ErrorMessage(ex.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<@NotNull ErrorMessage> handleMissingParameter(MissingServletRequestParameterException ex,  HttpServletRequest req){

        log.error("Unhandled exception: method={}, path={}, ip={}",
                req.getMethod(), req.getRequestURI(), req.getRemoteAddr(), ex);

        return ResponseEntity.badRequest().body(new ErrorMessage("Argument is missing" + ex.getParameterName()));
    }

    @ExceptionHandler(UserNameIsTakenException.class)
    public ResponseEntity<@NotNull ErrorMessage> handleOccupiedUserName(UserNameIsTakenException ex,  HttpServletRequest req) {

        log.error("Unhandled exception: method={}, path={}, ip={}",
                req.getMethod(), req.getRequestURI(), req.getRemoteAddr(), ex);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorMessage(ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<@NotNull ErrorMessage> handleBadCredentials(BadCredentialsException ex,  HttpServletRequest req) {
        log.error("Unhandled exception: method={}, path={}, ip={}",
                req.getMethod(), req.getRequestURI(), req.getRemoteAddr(), ex);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorMessage(ex.getMessage()));
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<@NotNull ErrorMessage> handleMinio(StorageException ex, HttpServletRequest req) {
        log.error("Unhandled exception: method={}, path={}, ip={}",
                req.getMethod(), req.getRequestURI(), req.getRemoteAddr(), ex);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorMessage(ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity <@NotNull ErrorMessage> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        log.error("Unhandled exception: method={}, path={}, ip={}",
                req.getMethod(), req.getRequestURI(), req.getRemoteAddr(), ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorMessage(ex.getMessage()));
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity <@NotNull ErrorMessage> handleResourceAlreadyExists(ResourceAlreadyExistsException ex, HttpServletRequest req) {
        log.error("Unhandled exception: method={}, path={}, ip={}",
                req.getMethod(), req.getRequestURI(), req.getRemoteAddr(), ex);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorMessage(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<@NotNull ErrorMessage> handleIllegalArgument(IllegalArgumentException ex,
                                                                       HttpServletRequest req) {
        log.error("Unhandled exception: method={}, path={}, ip={}",
                req.getMethod(), req.getRequestURI(), req.getRemoteAddr(), ex);

        return ResponseEntity.badRequest().body(new ErrorMessage(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<@NotNull ErrorMessage> handleUnknown(Exception ex,  HttpServletRequest req) {
        log.error("Unhandled exception: method={}, path={}, ip={}",
                req.getMethod(), req.getRequestURI(), req.getRemoteAddr(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorMessage("Unknown error"));
    }
}
