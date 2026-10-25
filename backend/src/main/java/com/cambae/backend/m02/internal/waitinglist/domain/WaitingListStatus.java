package com.cambae.backend.m02.internal.waitinglist.domain;

/** Trạng thái hồ sơ chờ (data-model mục 4; bảng trạng thái hồ sơ chờ của spec). */
public enum WaitingListStatus {
    WAITING,
    CONTACTED,
    ADMITTED,
    DECLINED,
    WAITING_CANCELLED;

    /** Chưa ở trạng thái cuối: Đang chờ hoặc Đã liên hệ (FR-007). */
    public boolean isOpen() {
        return this == WAITING || this == CONTACTED;
    }
}
