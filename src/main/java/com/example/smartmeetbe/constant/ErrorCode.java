package com.example.smartmeetbe.constant;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Auth
    EMAIL_ALREADY_EXISTS("Email already exists", HttpStatus.CONFLICT),
    INVALID_CREDENTIALS("Invalid email or password", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("Token is invalid or has expired", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("Token has expired", HttpStatus.UNAUTHORIZED),
    ACCOUNT_NOT_VERIFIED("Account has not been verified. Please check your email.", HttpStatus.FORBIDDEN),
    ACCOUNT_LOCKED("Account is locked. Please contact support.", HttpStatus.FORBIDDEN),
    REFRESH_TOKEN_NOT_FOUND("Refresh token not found", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_EXPIRED("Refresh token has expired", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_REVOKED("Refresh token has been revoked", HttpStatus.UNAUTHORIZED),

    // User
    USER_NOT_FOUND("User not found", HttpStatus.NOT_FOUND),
    WRONG_PASSWORD("Current password is incorrect", HttpStatus.BAD_REQUEST),

    // General
    INVALID_REQUEST("Invalid request", HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String message;
    private final HttpStatus status;

    ErrorCode(String message, HttpStatus status) {
        this.message = message;
        this.status = status;
    }
}