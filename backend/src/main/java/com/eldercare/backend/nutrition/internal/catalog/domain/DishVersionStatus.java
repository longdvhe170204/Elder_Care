package com.eldercare.backend.nutrition.internal.catalog.domain;

/** Trạng thái phiên bản nội dung món (data-model mục 3, {@code mon_an_phien_ban.trang_thai}). */
public enum DishVersionStatus {
    DRAFT,
    PENDING_CONFIRMATION,
    CONFIRMED,
    RETURNED,
    WITHDRAWN;

    /** Phiên bản đang soạn hoặc chờ: tối đa một mỗi món (khóa duy nhất của {@code simple_record}). */
    public boolean open() {
        return this == DRAFT || this == PENDING_CONFIRMATION;
    }
}
