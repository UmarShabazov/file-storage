package com.example.file_storage.exception;

import jakarta.servlet.http.HttpServletRequest;
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

    record ErrorMessage (String message) {};
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorMessage> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req){

        log.error("Unhandled exception: method={}, path={}, ip={}",
                req.getMethod(), req.getRequestURI(), req.getRemoteAddr(), ex);

        return ResponseEntity.badRequest().body(new ErrorMessage("Argument is not valid"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorMessage> handleMissingParameter(MissingServletRequestParameterException ex,  HttpServletRequest req){

        log.error("Unhandled exception: method={}, path={}, ip={}",
                req.getMethod(), req.getRequestURI(), req.getRemoteAddr(), ex);

        return ResponseEntity.badRequest().body(new ErrorMessage("Argument is missing"));
    }

    @ExceptionHandler(UserNameIsTakenException.class)
    public ResponseEntity<ErrorMessage> handleOccupiedUserName(UserNameIsTakenException ex,  HttpServletRequest req) {

        log.error("Unhandled exception: method={}, path={}, ip={}",
                req.getMethod(), req.getRequestURI(), req.getRemoteAddr(), ex);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorMessage(ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorMessage> handleBadCredentials(BadCredentialsException ex,  HttpServletRequest req) {
        log.error("Unhandled exception: method={}, path={}, ip={}",
                req.getMethod(), req.getRequestURI(), req.getRemoteAddr(), ex);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorMessage(ex.getMessage()));
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ErrorMessage> handleMinio(StorageException ex, HttpServletRequest req) {
        log.error("Unhandled exception: method={}, path={}, ip={}",
                req.getMethod(), req.getRequestURI(), req.getRemoteAddr(), ex);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorMessage(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessage> handleUnknown(Exception ex,  HttpServletRequest req) {
        log.error("Unhandled exception: method={}, path={}, ip={}",
                req.getMethod(), req.getRequestURI(), req.getRemoteAddr(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorMessage("Unknown error"));
    }
}
