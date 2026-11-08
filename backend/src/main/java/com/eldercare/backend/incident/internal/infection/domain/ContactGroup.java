package com.eldercare.backend.incident.internal.infection.domain;

/** Nhóm tra cứu truy vết (FR-060 a → d); {@code OTHER} chỉ dùng khi thêm người, bắt buộc lý do. */
public enum ContactGroup {
    SAME_ROOM,
    ACTIVITY,
    CARE_STAFF,
    FAMILY_VISIT,
    OTHER
}
