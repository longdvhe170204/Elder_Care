package com.cambae.backend.m02.internal.ending.domain;

/**
 * Mã điều kiện kết thúc lưu trú dùng chung với 006, 007, 010, 013, 017 (research T-09, module-interfaces mục 2.1; glossary
 * mục 10.3). (f) hợp đồng được xử lý và (g) giường được giải phóng do hệ thống làm khi lệnh chạy nên chỉ hiển thị "Tự động".
 * {@code END_E_HANDOVER} do 004 cài, không có ngoại lệ. Dòng (h) gộp {@code END_H_SETTLEMENT} (ba ý về tiền, miễn được) và
 * {@code END_H_SETTLEMENT_CONFIRMATION} (không còn giao dịch Chờ duyệt, Kế toán đã xác nhận quyết toán, không miễn được).
 */
public enum EndingConditionCode {
    END_A_COSTS(true),
    END_B_BELONGINGS(true),
    END_C_STORED_MEDICATION(true),
    END_D_ALERTS_INCIDENTS(true),
    END_E_HANDOVER(false),
    END_H_SETTLEMENT(true),
    END_H_SETTLEMENT_CONFIRMATION(false);

    private final boolean waivable;

    EndingConditionCode(boolean waivable) {
        this.waivable = waivable;
    }

    /** Có thể lập ngoại lệ (FR-064). */
    public boolean waivable() {
        return waivable;
    }
}
