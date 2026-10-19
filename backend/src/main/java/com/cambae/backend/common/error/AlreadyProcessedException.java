package com.cambae.backend.common.error;

import org.springframework.http.HttpStatus;

/** Đối tượng đã được người khác xử lý trước: 409 (FR-040). */
public class AlreadyProcessedException extends BusinessException {

    public AlreadyProcessedException(String message) {
        super(HttpStatus.CONFLICT, "ALREADY_PROCESSED", message);
    }
}
