package com.example.smartmeetbe.exception;

import com.example.smartmeetbe.constant.ErrorCode;
import lombok.Getter;

@Getter
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
    public static class AccountLocked extends AppException {
        public AccountLocked() { super(ErrorCode.ACCOUNT_LOCKED); }
    }

    public static class InvalidToken extends AppException {
        public InvalidToken() { super(ErrorCode.INVALID_TOKEN); }
    }

    public static class TokenExpired extends AppException {
        public TokenExpired() { super(ErrorCode.TOKEN_EXPIRED); }
    }
}