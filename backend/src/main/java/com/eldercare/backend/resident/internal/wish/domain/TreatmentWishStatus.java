package com.eldercare.backend.resident.internal.wish.domain;

/** Trạng thái của một phiên bản phiếu nguyện vọng khi điều trị (DBR-39). */
public enum TreatmentWishStatus {
    ACTIVE,
    EXPIRED,
    /** Bản Nháp tự khai (Q-412); không có hiệu lực, chỉ ở đường dẫn Nháp. */
    DRAFT,
    DISCARDED;
}
