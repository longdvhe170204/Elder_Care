package com.eldercare.backend.common;

/** Vai trò người dùng (mục 2.3, Phụ lục 26.1), kèm mã actor AC-xx. Đặt ở common (không ở m15.account.api) vì khuôn thông báo của common và mọi module dùng chung (000 T034). */
public enum Role {
    NURSING_HOME_MANAGER("AC-01"),
    FLOOR_MANAGER("AC-02"),
    DOCTOR("AC-03"),
    NURSING_STAFF("AC-04"),
    CAREGIVER("AC-05"),
    KITCHEN_STAFF("AC-07"),
    OFFICE_STAFF("AC-09"),
    FAMILY_MEMBER("AC-10"),
    SCHEDULER("AC-11"),
    ACCOUNTANT("AC-14"),
    RESIDENT("AC-16");

    private final String code;

    Role(String code) {
        this.code = code;
    }

    /** Mã actor, ví dụ {@code AC-01}. */
    public String code() {
        return code;
    }
}
