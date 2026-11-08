package com.eldercare.backend.incident.internal.emergency.domain;

import com.eldercare.backend.common.error.BusinessException;

/**
 * 001 từ chối lệnh Chuyển viện (BR-M05-14): lệnh rollback toàn bộ, sau đó sự cố ghi lại lần thử. Mang lỗi gốc của 001
 * để trả nguyên cho người dùng.
 */
public class TransferRefusedException extends RuntimeException {
    private final BusinessException refusal;

    public TransferRefusedException(BusinessException refusal) {
        super(refusal.getMessage(), refusal);
        this.refusal = refusal;
    }

    public BusinessException refusal() {
        return refusal;
    }
}
