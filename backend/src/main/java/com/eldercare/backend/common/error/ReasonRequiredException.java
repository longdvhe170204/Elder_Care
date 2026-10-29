package com.eldercare.backend.common.error;

import org.springframework.http.HttpStatus;

/** Thiếu lý do hoặc lý do không hợp lệ: 422 (FR-044). */
public class ReasonRequiredException extends BusinessException {

    public ReasonRequiredException() {
        super(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "REASON_REQUIRED",
                "Lý do bắt buộc, không chỉ có khoảng trắng, tối đa 2000 ký tự");
    }
}
