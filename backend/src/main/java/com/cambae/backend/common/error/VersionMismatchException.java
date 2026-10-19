package com.cambae.backend.common.error;

import org.springframework.http.HttpStatus;

/** If-Match không khớp phiên bản hiện tại: 412 kèm tài nguyên hiện hành (FR-021). */
public class VersionMismatchException extends BusinessException {

    private final Object currentValue;

    public VersionMismatchException(Object currentValue) {
        super(HttpStatus.PRECONDITION_FAILED, "VERSION_MISMATCH", "Dữ liệu đã bị thay đổi, xem giá trị hiện hành");
        this.currentValue = currentValue;
    }

    public Object currentValue() {
        return currentValue;
    }
}
