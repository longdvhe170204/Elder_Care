package com.cambae.backend.common.error;

import org.springframework.http.HttpStatus;

/** Giá trị sai kiểu hoặc ngoài khoảng: 422 (FR-015). */
public class InvalidValueException extends BusinessException {

    public InvalidValueException(String message) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_VALUE", message);
    }
}
