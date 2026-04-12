package com.example.smartmeetbe.constant;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    USER_NOT_FOUND("User not found", HttpStatus.NOT_FOUND),
    INVALID_REQUEST("Invalid request", HttpStatus.BAD_REQUEST),
    EMAIL_ALREADY_EXISTS("Email already exists", HttpStatus.CONFLICT),
    INTERNAL_ERROR("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String message;
    private final HttpStatus status;

    ErrorCode(String message, HttpStatus status) {
        this.message = message;
        this.status = status;
    }
}