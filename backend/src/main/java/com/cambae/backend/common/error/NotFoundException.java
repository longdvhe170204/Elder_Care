package com.cambae.backend.common.error;

import org.springframework.http.HttpStatus;

/** Không tồn tại hoặc ngoài phạm vi người xem; thông báo không lộ là có tồn tại (R-03). */
public class NotFoundException extends BusinessException {

    public NotFoundException() {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", "Không tìm thấy");
    }
}
