package com.example.file_storage.exception;

public class UserNameIsTakenException extends RuntimeException {
    public UserNameIsTakenException(String userName) {
        super("Username "+ userName +" is already taken.");
    }
}
