package com.eldercare.backend.common.error;

import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Lỗi nghiệp vụ trả về client dạng application/problem+json (research R-03). Module dùng các lớp con có sẵn, hoặc
 * lớp này với {@code errorCode} riêng (ví dụ {@code RECORD_VOIDED}); mã lỗi theo glossary mục 5.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final Map<String, Object> properties;

    public BusinessException(HttpStatus status, String errorCode, String message) {
        this(status, errorCode, message, Map.of());
    }

    /** {@code properties}: trường thêm vào thân lỗi (ví dụ {@code failedLayer}, {@code unlocksAt}). */
    public BusinessException(HttpStatus status, String errorCode, String message, Map<String, Object> properties) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.properties = Map.copyOf(properties);
    }

    public HttpStatus status() {
        return status;
    }

    public String errorCode() {
        return errorCode;
    }

    /** Trường thêm vào thân lỗi. */
    public Map<String, Object> properties() {
        return properties;
    }
}
