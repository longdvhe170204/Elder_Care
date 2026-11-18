package com.eldercare.backend.resident.internal.wish.domain;

/** Trạng thái của một phiên bản phiếu nguyện vọng cuối đời (DBR-34). */
public enum WishStatus {
    ACTIVE,
    SUPERSEDED,
    /** Bản Nháp tự khai (Q-412); không có hiệu lực, chỉ ở đường dẫn Nháp. */
    DRAFT,
    DISCARDED;
}
