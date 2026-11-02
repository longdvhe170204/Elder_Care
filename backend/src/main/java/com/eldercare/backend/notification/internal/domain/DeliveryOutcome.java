package com.eldercare.backend.notification.internal.domain;

/** Kết quả một lần gửi ({@code ket_qua_lan_gui.ket_qua}; data-model mục 5). */
public enum DeliveryOutcome {
    SENT,
    FAILED,
    CHANNEL_NOT_CONFIGURED,
    NO_ADDRESS,
    SKIPPED_OFF_SHIFT,
    /** Dồn vào tin tổng hợp của khung gộp (FR-016b); tin sẽ đi cùng tin tổng hợp. */
    BATCHED,
    INVALID_CONTENT;

    /** Thông báo đã tới (hoặc sẽ tới trong tin tổng hợp) người nhận qua kênh này. */
    public boolean delivered() {
        return this == SENT || this == BATCHED;
    }
}
