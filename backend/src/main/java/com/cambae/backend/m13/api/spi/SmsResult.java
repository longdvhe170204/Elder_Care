package com.cambae.backend.m13.api.spi;

import java.util.Optional;

/**
 * Kết quả gửi một tin nhắn ({@code KetQua}).
 *
 * @param providerCode mã lỗi của nhà cung cấp, nếu có
 */
public record SmsResult(Outcome outcome, Optional<String> providerCode, Optional<String> detail) {

    public enum Outcome {
        SENT,
        /** Lỗi tạm, gửi lại theo CFG-M13-05 (FR-018). */
        TEMPORARY_FAILURE,
        /** Lỗi vĩnh viễn, ví dụ số không hợp lệ. */
        PERMANENT_FAILURE,
        NOT_CONFIGURED
    }

    public static SmsResult of(Outcome outcome) {
        return new SmsResult(outcome, Optional.empty(), Optional.empty());
    }
}
