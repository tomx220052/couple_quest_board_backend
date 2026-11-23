package com.hannzer.cqb.common.error;

import org.springframework.http.HttpStatus;

/** 業務錯誤：由 {@link GlobalExceptionHandler} 轉成統一錯誤格式 */
public class AppException extends RuntimeException {

    private final HttpStatus status;
    private final ErrorCode code;

    public AppException(HttpStatus status, ErrorCode code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    public ErrorCode code() {
        return code;
    }

    public static AppException unauthenticated(String message) {
        return new AppException(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHENTICATED, message);
    }
}
