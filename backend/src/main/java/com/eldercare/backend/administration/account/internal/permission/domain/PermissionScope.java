package com.eldercare.backend.administration.account.internal.permission.domain;

/** Phạm vi của một quyền (cột {@code scope} của {@code permission_matrix}; research A-04, A-07). */
public enum PermissionScope {
    FACILITY,
    ASSIGNED,
    RELATIONSHIP,
    /** Chính người cao tuổi của tài khoản (AC-16, FR-068; research A-15). */
    SELF;

    /** Phạm vi này phủ {@code other}: bằng nhau, hoặc toàn viện phủ theo phân công (research A-05). */
    boolean covers(PermissionScope other) {
        return this == other || (this == FACILITY && other == ASSIGNED);
    }
}
