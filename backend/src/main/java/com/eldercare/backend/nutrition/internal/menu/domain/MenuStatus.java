package com.eldercare.backend.nutrition.internal.menu.domain;

/** Trạng thái thực đơn tuần (bảng trạng thái mục D; glossary mục 17.3). */
public enum MenuStatus {
    DRAFT,
    PENDING_PUBLICATION,
    PUBLISHED,
    APPLIED,
    CANCELLED;

    /** Công bố hoặc Đã áp dụng: dùng cho chốt suất, thực đơn chung, lặp món qua ranh giới tuần. */
    public boolean published() {
        return this == PUBLISHED || this == APPLIED;
    }
}
