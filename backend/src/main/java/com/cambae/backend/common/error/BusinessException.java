package com.cambae.backend.common.error;

import org.springframework.http.HttpStatus;

/**
 * Lỗi nghiệp vụ trả về client dạng application/problem+json (research R-03). Module dùng các lớp con có sẵn, hoặc
 * lớp này với {@code errorCode} riêng (ví dụ {@code RECORD_VOIDED}); mã lỗi theo glossary mục 5.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public BusinessException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus status() {
        return status;
    }

    public String errorCode() {
        return errorCode;
    }
}
