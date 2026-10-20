package com.cambae.backend.m15.account.permission.internal;

/** Phạm vi của một quyền (cột {@code scope} của {@code permission_matrix}; research A-04, A-07). */
public enum PermissionScope {
    FACILITY,
    ASSIGNED,
    RELATIONSHIP;

    /** Phạm vi này phủ {@code other}: bằng nhau, hoặc toàn viện phủ theo phân công (research A-05). */
    boolean covers(PermissionScope other) {
        return this == other || (this == FACILITY && other == ASSIGNED);
    }
}
